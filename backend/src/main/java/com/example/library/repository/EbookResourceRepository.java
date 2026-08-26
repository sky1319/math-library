package com.example.library.repository;

import com.example.library.entity.EbookResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EbookResourceRepository extends JpaRepository<EbookResource, Long> {
    Optional<EbookResource> findByIsbnAndPublishedTrue(String isbn);
    Optional<EbookResource> findByIsbn(String isbn);
}
