package com.example.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CatalogActionPrepareRequest {

    @NotBlank
    @Size(max = 100)
    private String query;

    @NotBlank
    @Size(max = 40)
    private String actionType;

    @Size(max = 32)
    private String isbn;

    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String author;

    @Size(max = 255)
    private String publisher;

    @Size(max = 100)
    private String category;

    private Integer quantity;

    @Size(max = 100)
    private String location;

    @Size(max = 500)
    private String keywords;

    @Size(max = 2000)
    private String description;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
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
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
