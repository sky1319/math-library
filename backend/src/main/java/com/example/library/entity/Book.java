package com.example.library.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "books")
public class Book {
    
    @Id
    private String isbn;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String author;
    
    private String publisher;
    
    @Column(length = 100)
    private String category;
    
    @Column(name = "total_count")
    private Integer totalCount;
    
    @Column(name = "borrowed_count")
    private Integer borrowedCount;
    
    @Column(length = 100)
    private String location;
    
    @Column(length = 500)
    private String keywords;
    
    @Column(length = 2000)
    private String description;

    @Column(name = "borrowable")
    private Boolean borrowable = true;

    public Book() {}

    public Book(String isbn, String title, String author, String publisher, String category, 
                Integer totalCount, Integer borrowedCount, String location, String keywords, String description) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.category = category;
        this.totalCount = totalCount;
        this.borrowedCount = borrowedCount;
        this.location = location;
        this.keywords = keywords;
        this.description = description;
    }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    
    public Integer getBorrowedCount() { return borrowedCount; }
    public void setBorrowedCount(Integer borrowedCount) { this.borrowedCount = borrowedCount; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getBorrowable() { return borrowable; }
    public void setBorrowable(Boolean borrowable) { this.borrowable = borrowable; }
}
