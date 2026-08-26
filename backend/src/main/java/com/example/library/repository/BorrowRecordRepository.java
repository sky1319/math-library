package com.example.library.repository;

import com.example.library.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByUserId(String userId);
    List<BorrowRecord> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    BorrowRecord findFirstByUserIdAndIsbnAndStatusOrderByIdDesc(String userId, String isbn, String status);
    boolean existsByUserIdAndIsbnAndStatus(String userId, String isbn, String status);
    long countByUserIdAndStatus(String userId, String status);
    long countByStatus(String status);
    long countByDueDateBeforeAndStatus(LocalDate date, String status);
    List<BorrowRecord> findByDueDateBeforeAndStatus(LocalDate date, String status);
    List<BorrowRecord> findByDueDateBetweenAndStatus(LocalDate start, LocalDate end, String status);
    
    @Modifying
    @Query("UPDATE BorrowRecord b SET b.overdueWarning = true WHERE b.dueDate < :date AND b.status = 'BORROWED' AND b.overdueWarning = false")
    void markOverdueWarnings(@Param("date") LocalDate date);
}
