package com.example.library.service;

import com.example.library.dto.response.BookResponse;
import com.example.library.entity.Book;
import com.example.library.entity.WishList;
import com.example.library.repository.BookRepository;
import com.example.library.repository.WishListRepository;
import com.example.library.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class WishListService {
    
    @Autowired
    private WishListRepository wishListRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @org.springframework.transaction.annotation.Transactional
    public void addToWishList(String userId, String isbn) {
        if (!bookRepository.existsById(isbn)) {
            throw BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在");
        }
        if (wishListRepository.existsByUserIdAndIsbn(userId, isbn)) {
            throw BusinessException.conflict("WISHLIST_DUPLICATE", "该图书已在愿望单中");
        }
        
        WishList wishList = new WishList();
        wishList.setUserId(userId);
        wishList.setIsbn(isbn);
        wishList.setAddedDate(LocalDate.now());
        
        wishListRepository.save(wishList);
    }
    
    @org.springframework.transaction.annotation.Transactional
    public void removeFromWishList(String userId, String isbn) {
        WishList wishList = wishListRepository.findByUserIdAndIsbn(userId, isbn);
        if (wishList == null) {
            throw BusinessException.notFound("WISHLIST_ITEM_NOT_FOUND", "愿望单中不存在该图书");
        }
        
        wishListRepository.delete(wishList);
    }
    
    public List<Book> getWishListBooks(String userId) {
        List<WishList> wishLists = wishListRepository.findByUserId(userId);
        List<Book> books = new ArrayList<>();
        
        for (WishList wishList : wishLists) {
            Book book = bookRepository.findById(wishList.getIsbn()).orElse(null);
            if (book != null) {
                books.add(book);
            }
        }
        
        return books;
    }
    
    public boolean isInWishList(String userId, String isbn) {
        return wishListRepository.existsByUserIdAndIsbn(userId, isbn);
    }
    
    public List<BookResponse> getUserWishList(String userId) {
        List<WishList> wishLists = wishListRepository.findByUserId(userId);
        List<BookResponse> books = new ArrayList<>();
        
        for (WishList wishList : wishLists) {
            Book book = bookRepository.findById(wishList.getIsbn()).orElse(null);
            if (book != null) {
                books.add(convertToResponse(book));
            }
        }
        
        return books;
    }
    
    private BookResponse convertToResponse(Book book) {
        return new BookResponse(
            book.getIsbn(),
            book.getTitle(),
            book.getAuthor(),
            book.getPublisher(),
            book.getCategory(),
            book.getTotalCount(),
            book.getBorrowedCount(),
            book.getTotalCount() - book.getBorrowedCount(),
            book.getLocation(),
            book.getKeywords(),
            book.getDescription()
        );
    }
}
