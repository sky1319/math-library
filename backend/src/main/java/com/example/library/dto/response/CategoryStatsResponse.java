package com.example.library.dto.response;

public class CategoryStatsResponse {
    
    private String category;
    private Long borrowCount;

    public CategoryStatsResponse() {}

    public CategoryStatsResponse(String category, Long borrowCount) {
        this.category = category;
        this.borrowCount = borrowCount;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Long getBorrowCount() { return borrowCount; }
    public void setBorrowCount(Long borrowCount) { this.borrowCount = borrowCount; }
}
