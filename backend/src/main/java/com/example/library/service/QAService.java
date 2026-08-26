package com.example.library.service;

import com.example.library.dto.response.BookResponse;
import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QAService {
    
    @Autowired
    private BookRepository bookRepository;
    
    private Map<String, List<String>> keywordMap = new HashMap<>();
    
    public QAService() {
        keywordMap.put("推理小说", List.of("推理", "侦探", "悬疑", "破案", "犯罪"));
        keywordMap.put("科幻小说", List.of("科幻", "太空", "未来", "三体", "机器人"));
        keywordMap.put("文学经典", List.of("经典", "文学", "名著", "诺贝尔", "茅盾"));
        keywordMap.put("历史", List.of("历史", "古代", "王朝", "战争", "传记"));
        keywordMap.put("编程", List.of("编程", "代码", "Java", "Python", "算法"));
    }
    
    public List<BookResponse> searchByQuestion(String question) {
        String normalizedQuestion = question.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (normalizedQuestion.contains(keyword.toLowerCase())) {
                    return searchBooksByKeyword(keyword);
                }
            }
        }
        
        return searchBooksByKeyword(question);
    }
    
    private List<BookResponse> searchBooksByKeyword(String keyword) {
        List<Book> books = bookRepository.searchBooks(keyword);
        return books.stream().map(this::convertToResponse).collect(Collectors.toList());
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
        return response;
    }
}
