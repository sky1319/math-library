package com.example.library.service;

import com.example.library.dto.response.BorrowRecordResponse;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.entity.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.BorrowRelationRepository;
import com.example.library.repository.UserRepository;
import com.example.library.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowService {
    
    @Autowired
    private BorrowRecordRepository borrowRecordRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BorrowRelationRepository borrowRelationRepository;
    
    @Autowired
    private OperationLogService logService;

    @Autowired(required = false)
    private ReservationService reservationService;
    
    @Value("${app.borrowing.max-books:5}")
    private int maxBooks;
    
    @Value("${app.borrowing.max-days:30}")
    private int maxDays;

    @Value("${app.borrowing.max-renewals:1}")
    private int maxRenewals;
    
    @Transactional
    public void borrowBook(String userId, String isbn) {
        Book book = bookRepository.findById(isbn).orElse(null);
        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        
        if (book == null) {
            throw BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在");
        }
        
        if (user == null) {
            throw BusinessException.notFound("USER_NOT_FOUND", "用户不存在");
        }

        if (book.getBorrowable() != null && !book.getBorrowable()) {
            throw BusinessException.conflict("BOOK_NOT_BORROWABLE", "该图书暂不可借阅，请联系管理员");
        }

        if (reservationService != null) {
            reservationService.assertBorrowAllowed(userId, isbn);
        }

        if (borrowRecordRepository.existsByUserIdAndIsbnAndStatus(userId, isbn, "BORROWED")) {
            throw BusinessException.conflict("BOOK_ALREADY_BORROWED", "你已经借阅了这本图书");
        }
        
        long activeBorrows = borrowRecordRepository.countByUserIdAndStatus(userId, "BORROWED");
        if (activeBorrows >= maxBooks) {
            throw BusinessException.conflict("BORROW_LIMIT_REACHED", "已达到最大借阅数量限制");
        }

        if (bookRepository.borrowOneIfAvailable(isbn) != 1) {
            throw BusinessException.conflict("BOOK_OUT_OF_STOCK", "该图书已全部借出");
        }
        
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setIsbn(isbn);
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(maxDays));
        record.setStatus("BORROWED");
        record.setOverdueWarning(false);
        
        borrowRecordRepository.save(record);

        if (reservationService != null) {
            reservationService.completeAfterBorrow(userId, isbn);
        }
        
        updateBorrowRelation(userId, isbn);
        
        logService.log(userId, user.getRole(), "借阅图书", "ISBN: " + isbn + ", 书名: " + book.getTitle());
        
    }
    
    @Transactional
    public void returnBook(String userId, String isbn) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "用户不存在"));
        BorrowRecord record = borrowRecordRepository.findFirstByUserIdAndIsbnAndStatusOrderByIdDesc(
                userId, isbn, "BORROWED");
        
        if (record == null) {
            throw BusinessException.notFound("ACTIVE_BORROW_NOT_FOUND", "未找到有效借阅记录");
        }
        
        record.setReturnDate(LocalDate.now());
        record.setStatus("RETURNED");
        borrowRecordRepository.save(record);
        
        if (bookRepository.returnOneIfBorrowed(isbn) != 1) {
            throw BusinessException.conflict("BOOK_STOCK_CONFLICT", "馆藏库存状态冲突，请联系管理员");
        }

        logService.log(userId, user.getRole(), "归还图书", "ISBN: " + isbn);

        if (reservationService != null) {
            reservationService.notifyNextForBook(isbn);
        }
    }

    @Transactional
    public BorrowRecordResponse renewBook(String userId, String isbn) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "用户不存在"));
        BorrowRecord record = borrowRecordRepository.findFirstByUserIdAndIsbnAndStatusOrderByIdDesc(
                userId, isbn, "BORROWED");
        if (record == null) {
            throw BusinessException.notFound("ACTIVE_BORROW_NOT_FOUND", "未找到有效借阅记录");
        }
        if (record.getDueDate().isBefore(LocalDate.now())) {
            throw BusinessException.conflict("BORROW_OVERDUE", "逾期图书不能续借");
        }
        int renewCount = record.getRenewCount() == null ? 0 : record.getRenewCount();
        if (renewCount >= maxRenewals) {
            throw BusinessException.conflict("RENEW_LIMIT_REACHED", "已达到最大续借次数");
        }
        if (reservationService != null && reservationService.hasActiveReservationByOtherUser(isbn, userId)) {
            throw BusinessException.conflict("BOOK_HAS_RESERVATION_QUEUE", "已有其他读者排队预约，暂不能续借");
        }
        record.setDueDate(record.getDueDate().plusDays(maxDays));
        record.setRenewCount(renewCount + 1);
        record.setOverdueWarning(false);
        borrowRecordRepository.save(record);
        logService.log(userId, user.getRole(), "续借图书", "ISBN: " + isbn + ", 新到期日: " + record.getDueDate());
        return convertToResponse(record);
    }
    
    private void updateBorrowRelation(String userId, String newIsbn) {
        List<BorrowRecord> userRecords = borrowRecordRepository.findByUserId(userId);
        
        for (BorrowRecord record : userRecords) {
            if (!record.getIsbn().equals(newIsbn) && record.getStatus().equals("RETURNED")) {
                String existingIsbn = record.getIsbn();
                
                com.example.library.entity.BorrowRelation relation = 
                    borrowRelationRepository.findByBookAIsbnAndBookBIsbn(existingIsbn, newIsbn);
                
                if (relation != null) {
                    relation.setRelationCount(relation.getRelationCount() + 1);
                    borrowRelationRepository.save(relation);
                } else {
                    com.example.library.entity.BorrowRelation newRelation = 
                        new com.example.library.entity.BorrowRelation();
                    newRelation.setBookAIsbn(existingIsbn);
                    newRelation.setBookBIsbn(newIsbn);
                    newRelation.setRelationCount(1);
                    borrowRelationRepository.save(newRelation);
                }
            }
        }
    }
    
    public List<BorrowRecordResponse> getUserBorrowRecords(String userId) {
        List<BorrowRecord> records;
        if (userId == null) {
            records = borrowRecordRepository.findAll();
        } else {
            records = borrowRecordRepository.findByUserId(userId);
        }
        return records.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    public List<BorrowRecordResponse> getBookBorrowRecords(String isbn) {
        List<BorrowRecord> records = borrowRecordRepository.findByIsbn(isbn);
        return records.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    public List<BorrowRecordResponse> getAllOverdueRecords() {
        List<BorrowRecord> records = borrowRecordRepository.findByDueDateBeforeAndStatus(LocalDate.now(), "BORROWED");
        return records.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    @Transactional
    public void checkOverdueAndWarn() {
        borrowRecordRepository.markOverdueWarnings(LocalDate.now());
    }
    
    private BorrowRecordResponse convertToResponse(BorrowRecord record) {
        BorrowRecordResponse response = new BorrowRecordResponse();
        response.setId(record.getId());
        response.setUserId(record.getUserId());
        response.setIsbn(record.getIsbn());
        response.setBorrowDate(record.getBorrowDate());
        response.setDueDate(record.getDueDate());
        response.setReturnDate(record.getReturnDate());
        response.setStatus(record.getStatus());
        response.setOverdueWarning(record.getOverdueWarning());
        response.setRenewCount(record.getRenewCount() == null ? 0 : record.getRenewCount());
        response.setRenewable(
                "BORROWED".equals(record.getStatus())
                        && !record.getDueDate().isBefore(LocalDate.now())
                        && (record.getRenewCount() == null ? 0 : record.getRenewCount()) < maxRenewals);
        
        if (record.getReturnDate() == null && record.getDueDate().isBefore(LocalDate.now())) {
            response.setDaysOverdue((int) ChronoUnit.DAYS.between(record.getDueDate(), LocalDate.now()));
        } else {
            response.setDaysOverdue(0);
        }
        
        User user = userRepository.findById(record.getUserId()).orElse(null);
        if (user != null) {
            response.setUserName(user.getName());
        }
        
        Book book = bookRepository.findById(record.getIsbn()).orElse(null);
        if (book != null) {
            response.setBookTitle(book.getTitle());
        }
        
        return response;
    }
}
