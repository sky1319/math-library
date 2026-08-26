package com.example.library.service;

import com.example.library.dto.response.CategoryStatsResponse;
import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {
    
    @Autowired
    private BookRepository bookRepository;
    
    public List<CategoryStatsResponse> getCategoryBorrowStats() {
        List<Book> books = bookRepository.findAll();
        
        Map<String, Long> categoryStats = books.stream()
            .collect(Collectors.groupingBy(Book::getCategory, 
                Collectors.summingLong(Book::getBorrowedCount)));
        
        List<CategoryStatsResponse> stats = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : categoryStats.entrySet()) {
            CategoryStatsResponse stat = new CategoryStatsResponse();
            stat.setCategory(entry.getKey());
            stat.setBorrowCount(entry.getValue());
            stats.add(stat);
        }
        
        return stats;
    }
}
