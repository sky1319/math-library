package com.example.library.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "borrow_relation")
public class BorrowRelation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "book_a_isbn", nullable = false)
    private String bookAIsbn;
    
    @Column(name = "book_b_isbn", nullable = false)
    private String bookBIsbn;
    
    @Column(name = "relation_count")
    private Integer relationCount;

    public BorrowRelation() {}

    public BorrowRelation(Long id, String bookAIsbn, String bookBIsbn, Integer relationCount) {
        this.id = id;
        this.bookAIsbn = bookAIsbn;
        this.bookBIsbn = bookBIsbn;
        this.relationCount = relationCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getBookAIsbn() { return bookAIsbn; }
    public void setBookAIsbn(String bookAIsbn) { this.bookAIsbn = bookAIsbn; }
    
    public String getBookBIsbn() { return bookBIsbn; }
    public void setBookBIsbn(String bookBIsbn) { this.bookBIsbn = bookBIsbn; }
    
    public Integer getRelationCount() { return relationCount; }
    public void setRelationCount(Integer relationCount) { this.relationCount = relationCount; }
}
