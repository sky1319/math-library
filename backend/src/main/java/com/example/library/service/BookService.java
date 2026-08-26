package com.example.library.service;

import com.example.library.dto.request.BookRequest;
import com.example.library.dto.response.BookResponse;
import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BookReservationRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.BorrowRelationRepository;
import com.example.library.repository.WishListRepository;
import com.example.library.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private DataCacheService cacheService;
    
    @Autowired
    private OperationLogService logService;

    @Autowired
    private BookKnowledgeService bookKnowledgeService;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private BookReservationRepository reservationRepository;

    @Autowired
    private WishListRepository wishListRepository;

    @Autowired
    private BorrowRelationRepository borrowRelationRepository;
    
    public BookResponse getBookByIsbn(String isbn) {
        Book book = bookRepository.findById(isbn).orElse(null);
        if (book == null) {
            return null;
        }
        return convertToResponse(book);
    }
    
    public List<BookResponse> searchBooks(String keyword) {
        String cacheKey = "search:" + keyword;
        List<Book> cached = cacheService.getCachedResult(cacheKey);
        
        if (cached != null) {
            return cached.stream().map(this::convertToResponse).collect(Collectors.toList());
        }
        
        List<Book> books = bookRepository.searchBooks(keyword);
        cacheService.cacheResult(cacheKey, books);
        
        return books.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<BookResponse> getBooksPage(
            String keyword,
            int page,
            int size,
            String sortBy,
            Sort.Direction direction) {
        return bookRepository.searchBooksPage(
                        keyword == null ? "" : keyword.trim(),
                        PageRequest.of(page, size, Sort.by(direction, sortBy)))
                .map(this::convertToResponse);
    }
    
    public BookResponse addBook(BookRequest request, String userId) {
        if (bookRepository.existsById(request.getIsbn())) {
            throw BusinessException.conflict("BOOK_ISBN_EXISTS", "ISBN 已存在");
        }
        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setCategory(request.getCategory());
        book.setTotalCount(request.getTotalCount());
        book.setBorrowedCount(0);
        book.setLocation(request.getLocation());
        book.setKeywords(request.getKeywords());
        book.setDescription(request.getDescription());
        book.setBorrowable(request.getBorrowable() != null ? request.getBorrowable() : true);
        
        bookRepository.save(book);
        cacheService.clearCache();
        bookKnowledgeService.refresh();
        logService.log(userId, "ADMIN", "新增图书", "ISBN: " + book.getIsbn() + ", 书名: " + book.getTitle());
        
        return convertToResponse(book);
    }
    
    public BookResponse updateBook(String isbn, BookRequest request, String userId) {
        Book book = bookRepository.findById(isbn).orElse(null);
        if (book == null) {
            return null;
        }

        if (request.getTotalCount() != null && request.getTotalCount() < book.getBorrowedCount()) {
            throw BusinessException.conflict(
                    "BOOK_COUNT_BELOW_BORROWED",
                    "馆藏总数不能小于当前已借数量 " + book.getBorrowedCount());
        }
        
        if (request.getTitle() != null) book.setTitle(request.getTitle());
        if (request.getAuthor() != null) book.setAuthor(request.getAuthor());
        if (request.getPublisher() != null) book.setPublisher(request.getPublisher());
        if (request.getCategory() != null) book.setCategory(request.getCategory());
        if (request.getTotalCount() != null) book.setTotalCount(request.getTotalCount());
        if (request.getLocation() != null) book.setLocation(request.getLocation());
        if (request.getKeywords() != null) book.setKeywords(request.getKeywords());
        if (request.getDescription() != null) book.setDescription(request.getDescription());
        if (request.getBorrowable() != null) book.setBorrowable(request.getBorrowable());
        
        bookRepository.save(book);
        cacheService.clearCache();
        bookKnowledgeService.refresh();
        logService.log(userId, "ADMIN", "修改图书", "ISBN: " + isbn);
        
        return convertToResponse(book);
    }
    
    public boolean deleteBook(String isbn, String userId) {
        Book book = bookRepository.findById(isbn).orElse(null);
        if (book == null) {
            return false;
        }

        deleteBookStrict(isbn, userId);
        return true;
    }

    @Transactional
    public BookResponse increaseStock(String isbn, int quantity, String location, String userId) {
        requirePositiveQuantity(quantity);
        Book book = requireBook(isbn);
        book.setTotalCount(Math.addExact(value(book.getTotalCount()), quantity));
        if (location != null && !location.isBlank()) book.setLocation(location.trim());
        bookRepository.save(book);
        refreshBookViews();
        logService.log(userId, "AGENT_STAFF", "增加馆藏",
                "ISBN: " + isbn + ", 数量: +" + quantity + ", 调整后: " + book.getTotalCount());
        return convertToResponse(book);
    }

    @Transactional
    public BookResponse reduceStock(String isbn, int quantity, String userId) {
        requirePositiveQuantity(quantity);
        Book book = requireBook(isbn);
        int current = value(book.getTotalCount());
        int minimum = value(book.getBorrowedCount())
                + (int) reservationRepository.countByIsbnAndStatus(isbn, "NOTIFIED");
        int next = current - quantity;
        if (next < minimum) {
            throw BusinessException.conflict(
                    "BOOK_STOCK_IN_USE",
                    "减少后馆藏不能低于已借副本与到书保留副本之和：" + minimum);
        }
        book.setTotalCount(next);
        bookRepository.save(book);
        refreshBookViews();
        logService.log(userId, "AGENT_STAFF", "减少馆藏",
                "ISBN: " + isbn + ", 数量: -" + quantity + ", 调整后: " + next);
        return convertToResponse(book);
    }

    @Transactional
    public BookResponse setBorrowable(String isbn, boolean borrowable, String userId) {
        Book book = requireBook(isbn);
        book.setBorrowable(borrowable);
        bookRepository.save(book);
        refreshBookViews();
        logService.log(userId, "AGENT_STAFF", borrowable ? "恢复借阅" : "停止借阅", "ISBN: " + isbn);
        return convertToResponse(book);
    }

    @Transactional
    public void deleteBookStrict(String isbn, String userId) {
        Book book = requireBook(isbn);
        if (value(book.getBorrowedCount()) > 0 || borrowRecordRepository.existsByIsbn(isbn)) {
            throw BusinessException.conflict("BOOK_HAS_BORROW_HISTORY", "存在当前或历史借阅记录，不能彻底删除；可选择停止借阅");
        }
        if (reservationRepository.existsByIsbn(isbn)) {
            throw BusinessException.conflict("BOOK_HAS_RESERVATIONS", "存在预约记录，不能彻底删除；可选择停止借阅");
        }
        if (wishListRepository.existsByIsbn(isbn)) {
            throw BusinessException.conflict("BOOK_IN_WISHLIST", "该书仍在读者愿望单中，不能彻底删除；可选择停止借阅");
        }
        if (borrowRelationRepository.existsByBookAIsbnOrBookBIsbn(isbn, isbn)) {
            throw BusinessException.conflict("BOOK_HAS_RELATIONS", "该书存在推荐关联数据，不能彻底删除；可选择停止借阅");
        }
        bookRepository.delete(book);
        refreshBookViews();
        logService.log(userId, "ADMIN", "彻底删除图书", "ISBN: " + isbn + ", 书名: " + book.getTitle());
    }
    
    public List<BookResponse> findSimilarBooks(String isbn) {
        Book book = bookRepository.findById(isbn).orElse(null);
        if (book == null) {
            return new ArrayList<>();
        }
        
        List<Book> similarBooks = bookRepository.findByCategory(book.getCategory()).stream()
                .filter(b -> !b.getIsbn().equals(isbn))
                .collect(Collectors.toList());
        
        return similarBooks.stream()
                .limit(3)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public boolean existsById(String isbn) {
        return bookRepository.existsById(isbn);
    }
    
    public void saveBook(Book book) {
        bookRepository.save(book);
    }

    private Book requireBook(String isbn) {
        return bookRepository.findById(isbn)
                .orElseThrow(() -> BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在"));
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity < 1 || quantity > 1000) {
            throw BusinessException.badRequest("INVALID_STOCK_QUANTITY", "调整数量必须在 1 到 1000 之间");
        }
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private void refreshBookViews() {
        cacheService.clearCache();
        bookKnowledgeService.refresh();
    }
    
    private BookResponse convertToResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setIsbn(book.getIsbn());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setPublisher(book.getPublisher());
        response.setCategory(book.getCategory());
        response.setTotalCount(book.getTotalCount());
        response.setBorrowedCount(book.getBorrowedCount());
        response.setAvailableCount(book.getTotalCount() - book.getBorrowedCount());
        response.setLocation(book.getLocation());
        response.setKeywords(book.getKeywords());
        response.setDescription(book.getDescription());
        response.setBorrowable(book.getBorrowable() != null ? book.getBorrowable() : true);
        return response;
    }
}
