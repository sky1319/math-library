package com.example.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

public class BookRequest {
    
    @NotBlank(groups = Create.class, message = "ISBN不能为空")
    @Size(max = 32, message = "ISBN不能超过32个字符")
    private String isbn;

    @NotBlank(groups = Create.class, message = "书名不能为空")
    @Size(max = 255, message = "书名不能超过255个字符")
    private String title;

    @NotBlank(groups = Create.class, message = "作者不能为空")
    @Size(max = 255, message = "作者不能超过255个字符")
    private String author;

    @Size(max = 255, message = "出版社不能超过255个字符")
    private String publisher;

    @Size(max = 100, message = "分类不能超过100个字符")
    private String category;

    @NotNull(groups = Create.class, message = "馆藏数量不能为空")
    @PositiveOrZero(groups = {Create.class, Default.class}, message = "馆藏数量不能为负数")
    private Integer totalCount;

    @Size(max = 100, message = "馆藏位置不能超过100个字符")
    private String location;

    @Size(max = 500, message = "关键词不能超过500个字符")
    private String keywords;

    @Size(max = 2000, message = "简介不能超过2000个字符")
    private String description;
    private Boolean borrowable;

    public interface Create { }

    public BookRequest() {}

    public BookRequest(String isbn, String title, String author, String publisher, String category,
                       Integer totalCount, String location, String keywords, String description) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.category = category;
        this.totalCount = totalCount;
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
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getBorrowable() { return borrowable; }
    public void setBorrowable(Boolean borrowable) { this.borrowable = borrowable; }
}
