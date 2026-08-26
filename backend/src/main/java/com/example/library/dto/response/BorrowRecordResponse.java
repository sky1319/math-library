package com.example.library.dto.response;

import java.time.LocalDate;

public class BorrowRecordResponse {
    
    private Long id;
    private String userId;
    private String userName;
    private String isbn;
    private String bookTitle;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;
    private Boolean overdueWarning;
    private Integer daysOverdue;
    private Integer renewCount;
    private Boolean renewable;

    public BorrowRecordResponse() {}

    public BorrowRecordResponse(Long id, String userId, String userName, String isbn, String bookTitle,
                                LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate, 
                                String status, Boolean overdueWarning, Integer daysOverdue) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.isbn = isbn;
        this.bookTitle = bookTitle;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
        this.overdueWarning = overdueWarning;
        this.daysOverdue = daysOverdue;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    
    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Boolean getOverdueWarning() { return overdueWarning; }
    public void setOverdueWarning(Boolean overdueWarning) { this.overdueWarning = overdueWarning; }
    
    public Integer getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(Integer daysOverdue) { this.daysOverdue = daysOverdue; }

    public Integer getRenewCount() { return renewCount; }
    public void setRenewCount(Integer renewCount) { this.renewCount = renewCount; }

    public Boolean getRenewable() { return renewable; }
    public void setRenewable(Boolean renewable) { this.renewable = renewable; }
}
