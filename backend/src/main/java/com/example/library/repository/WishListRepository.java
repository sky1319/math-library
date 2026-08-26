package com.example.library.repository;

import com.example.library.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishListRepository extends JpaRepository<WishList, Long> {
    List<WishList> findByUserId(String userId);
    WishList findByUserIdAndIsbn(String userId, String isbn);
    void deleteByUserIdAndIsbn(String userId, String isbn);
    boolean existsByUserIdAndIsbn(String userId, String isbn);
    boolean existsByIsbn(String isbn);
}
