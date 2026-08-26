package com.example.library.repository;

import com.example.library.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {
    Optional<Book> findFirstByTitle(String title);
    List<Book> findByTitleContainingOrAuthorContainingOrKeywordsContaining(String title, String author, String keywords);
    List<Book> findByCategory(String category);
    
    @Query("SELECT b FROM Book b WHERE b.title LIKE %:keyword% OR b.author LIKE %:keyword% OR b.keywords LIKE %:keyword%")
    List<Book> searchBooks(@Param("keyword") String keyword);

    @Query("""
            SELECT b FROM Book b
            WHERE :keyword = ''
               OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(b.keywords, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Book> searchBooksPage(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT DISTINCT b.category FROM Book b")
    List<String> findAllCategories();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Book b
            SET b.borrowedCount = b.borrowedCount + 1
            WHERE b.isbn = :isbn
              AND b.borrowable = true
              AND b.borrowedCount < b.totalCount
            """)
    int borrowOneIfAvailable(@Param("isbn") String isbn);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Book b
            SET b.borrowedCount = b.borrowedCount - 1
            WHERE b.isbn = :isbn
              AND b.borrowedCount > 0
            """)
    int returnOneIfBorrowed(@Param("isbn") String isbn);
}
