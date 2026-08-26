package com.example.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ebook_resources")
public class EbookResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String isbn;

    @Column(name = "rights_status", nullable = false, length = 30)
    private String rightsStatus;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "source_page_pattern", nullable = false, length = 255)
    private String sourcePagePattern;

    @Column(name = "license_name", nullable = false, length = 100)
    private String licenseName;

    @Column(name = "license_url", nullable = false, length = 500)
    private String licenseUrl;

    @Column(nullable = false, length = 100)
    private String jurisdiction;

    @Column(name = "chapter_count", nullable = false)
    private Integer chapterCount;

    @Column(name = "author_death_year", nullable = false)
    private Integer authorDeathYear;

    @Column(name = "first_publication_year", nullable = false)
    private Integer firstPublicationYear;

    @Column(name = "rights_evidence", nullable = false, length = 2000)
    private String rightsEvidence;

    @Column(name = "content_notice", nullable = false, length = 1000)
    private String contentNotice;

    @Column(nullable = false)
    private Boolean published = false;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getRightsStatus() { return rightsStatus; }
    public void setRightsStatus(String rightsStatus) { this.rightsStatus = rightsStatus; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourcePagePattern() { return sourcePagePattern; }
    public void setSourcePagePattern(String sourcePagePattern) { this.sourcePagePattern = sourcePagePattern; }
    public String getLicenseName() { return licenseName; }
    public void setLicenseName(String licenseName) { this.licenseName = licenseName; }
    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    public Integer getChapterCount() { return chapterCount; }
    public void setChapterCount(Integer chapterCount) { this.chapterCount = chapterCount; }
    public Integer getAuthorDeathYear() { return authorDeathYear; }
    public void setAuthorDeathYear(Integer authorDeathYear) { this.authorDeathYear = authorDeathYear; }
    public Integer getFirstPublicationYear() { return firstPublicationYear; }
    public void setFirstPublicationYear(Integer firstPublicationYear) { this.firstPublicationYear = firstPublicationYear; }
    public String getRightsEvidence() { return rightsEvidence; }
    public void setRightsEvidence(String rightsEvidence) { this.rightsEvidence = rightsEvidence; }
    public String getContentNotice() { return contentNotice; }
    public void setContentNotice(String contentNotice) { this.contentNotice = contentNotice; }
    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
