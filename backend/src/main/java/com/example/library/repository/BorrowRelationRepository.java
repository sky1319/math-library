package com.example.library.repository;

import com.example.library.entity.BorrowRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRelationRepository extends JpaRepository<BorrowRelation, Long> {
    BorrowRelation findByBookAIsbnAndBookBIsbn(String bookAIsbn, String bookBIsbn);
    List<BorrowRelation> findByBookAIsbnOrderByRelationCountDesc(String bookAIsbn);
    boolean existsByBookAIsbnOrBookBIsbn(String bookAIsbn, String bookBIsbn);
    
    @Modifying
    @Query("UPDATE BorrowRelation b SET b.relationCount = b.relationCount + 1 WHERE b.bookAIsbn = :bookAIsbn AND b.bookBIsbn = :bookBIsbn")
    void incrementRelationCount(@Param("bookAIsbn") String bookAIsbn, @Param("bookBIsbn") String bookBIsbn);
}
