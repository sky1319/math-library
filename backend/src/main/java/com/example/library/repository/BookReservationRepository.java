package com.example.library.repository;

import com.example.library.entity.BookReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookReservationRepository extends JpaRepository<BookReservation, Long> {
    List<BookReservation> findByUserIdOrderByReservedAtDesc(String userId);
    List<BookReservation> findByIsbnAndStatusInOrderByReservedAtAsc(String isbn, Collection<String> statuses);
    List<BookReservation> findByStatusAndExpiresAtBefore(String status, LocalDateTime time);
    Optional<BookReservation> findFirstByUserIdAndIsbnAndStatusInOrderByReservedAtDesc(
            String userId, String isbn, Collection<String> statuses);
    boolean existsByUserIdAndIsbnAndStatusIn(String userId, String isbn, Collection<String> statuses);
    long countByIsbnAndStatus(String isbn, String status);
    boolean existsByIsbn(String isbn);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM BookReservation r WHERE r.id = :id")
    Optional<BookReservation> findByIdForUpdate(@Param("id") Long id);
}
