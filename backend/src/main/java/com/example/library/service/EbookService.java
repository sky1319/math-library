package com.example.library.service;

import com.example.library.dto.request.ReadingProgressRequest;
import com.example.library.dto.response.EbookChapterResponse;
import com.example.library.dto.response.EbookResourceResponse;
import com.example.library.dto.response.ReadingProgressResponse;
import com.example.library.dto.response.AgentNavigationResponse;
import com.example.library.entity.Book;
import com.example.library.entity.EbookResource;
import com.example.library.entity.ReadingProgress;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.EbookResourceRepository;
import com.example.library.repository.ReadingProgressRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EbookService {

    private static final String RIGHTS_STATUS = "PUBLIC_DOMAIN_VERIFIED";
    private static final String API_HOST = "zh.wikisource.org";
    private static final Set<String> ALLOWED_PATTERNS = Set.of(
            "紅樓夢/第%03d回",
            "三國演義/第%03d回",
            "水滸傳_(120回本)/第%03d回",
            "西遊記/第%03d回");
    private static final int MAX_CACHE_ENTRIES = 100;
    private static final int MAX_CONTENT_LENGTH = 1_000_000;

    private final EbookResourceRepository resourceRepository;
    private final ReadingProgressRepository progressRepository;
    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final Map<String, EbookChapterResponse> chapterCache = new ConcurrentHashMap<>();

    public EbookService(
            EbookResourceRepository resourceRepository,
            ReadingProgressRepository progressRepository,
            BookRepository bookRepository,
            ObjectMapper objectMapper,
            @Qualifier("wikisourceHttpClient") OkHttpClient httpClient) {
        this.resourceRepository = resourceRepository;
        this.progressRepository = progressRepository;
        this.bookRepository = bookRepository;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Transactional(readOnly = true)
    public EbookResourceResponse getResource(String isbn, String userId) {
        EbookResource resource = requirePublishable(isbn);
        Book book = bookRepository.findById(isbn)
                .orElseThrow(() -> BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在"));
        ReadingProgressResponse progress = progressRepository.findByUserIdAndResourceId(userId, resource.getId())
                .map(this::toProgressResponse)
                .orElse(new ReadingProgressResponse(1, 0, null));
        return new EbookResourceResponse(
                isbn,
                book.getTitle(),
                book.getAuthor(),
                resource.getRightsStatus(),
                resource.getSourceName(),
                resource.getSourceUrl(),
                resource.getLicenseName(),
                resource.getLicenseUrl(),
                resource.getJurisdiction(),
                resource.getChapterCount(),
                resource.getAuthorDeathYear(),
                resource.getFirstPublicationYear(),
                resource.getRightsEvidence(),
                resource.getContentNotice(),
                resource.getVerifiedAt(),
                progress);
    }

    @Transactional(readOnly = true)
    public EbookChapterResponse getChapter(String isbn, int chapterNumber) {
        EbookResource resource = requirePublishable(isbn);
        validateChapter(resource, chapterNumber);
        String cacheKey = isbn + ":" + chapterNumber;
        EbookChapterResponse cached = chapterCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        EbookChapterResponse chapter = fetchChapter(resource, chapterNumber);
        if (chapterCache.size() >= MAX_CACHE_ENTRIES) {
            chapterCache.clear();
        }
        chapterCache.put(cacheKey, chapter);
        return chapter;
    }

    @Transactional
    public ReadingProgressResponse saveProgress(String isbn, String userId, ReadingProgressRequest request) {
        EbookResource resource = requirePublishable(isbn);
        validateChapter(resource, request.chapterNumber());
        ReadingProgress progress = progressRepository.findByUserIdAndResourceId(userId, resource.getId())
                .orElseGet(ReadingProgress::new);
        progress.setUserId(userId);
        progress.setResourceId(resource.getId());
        progress.setChapterNumber(request.chapterNumber());
        progress.setScrollPercent(request.scrollPercent());
        progress.setUpdatedAt(LocalDateTime.now());
        return toProgressResponse(progressRepository.save(progress));
    }

    @Transactional(readOnly = true)
    public AgentNavigationResponse prepareReaderNavigation(
            String isbnOrTitle, Integer requestedChapter, String userId) {
        String identifier = isbnOrTitle == null ? "" : isbnOrTitle.trim();
        if (identifier.isEmpty()) {
            throw BusinessException.badRequest("BOOK_IDENTIFIER_REQUIRED", "请提供要阅读的准确书名或 ISBN");
        }
        Book book = bookRepository.findById(identifier)
                .or(() -> bookRepository.findFirstByTitle(identifier))
                .orElseThrow(() -> BusinessException.notFound("BOOK_NOT_FOUND", "馆藏中未找到该图书"));
        EbookResource resource = requirePublishable(book.getIsbn());
        int chapterNumber;
        if (requestedChapter != null) {
            validateChapter(resource, requestedChapter);
            chapterNumber = requestedChapter;
        } else {
            chapterNumber = progressRepository.findByUserIdAndResourceId(userId, resource.getId())
                    .map(ReadingProgress::getChapterNumber)
                    .filter(value -> value >= 1 && value <= resource.getChapterCount())
                    .orElse(1);
        }
        return new AgentNavigationResponse(
                "OPEN_EBOOK",
                book.getIsbn(),
                book.getTitle(),
                chapterNumber,
                resource.getChapterCount(),
                "已核验该书的公版原文与数字文本许可，可以安全进入阅读器");
    }

    @Transactional
    public void unpublish(String isbn) {
        EbookResource resource = resourceRepository.findByIsbn(isbn)
                .orElseThrow(() -> BusinessException.notFound(
                        "EBOOK_NOT_FOUND", "电子资源不存在"));
        resource.setPublished(false);
        resourceRepository.save(resource);
        chapterCache.keySet().removeIf(key -> key.startsWith(isbn + ":"));
    }

    private EbookResource requirePublishable(String isbn) {
        EbookResource resource = resourceRepository.findByIsbnAndPublishedTrue(isbn)
                .orElseThrow(() -> BusinessException.notFound(
                        "EBOOK_NOT_AVAILABLE", "该图书没有通过权属核验的可阅读正文"));
        boolean complete = RIGHTS_STATUS.equals(resource.getRightsStatus())
                && ALLOWED_PATTERNS.contains(resource.getSourcePagePattern())
                && resource.getSourceUrl() != null
                && resource.getSourceUrl().startsWith("https://zh.wikisource.org/wiki/")
                && "CC BY-SA 4.0".equals(resource.getLicenseName())
                && "https://creativecommons.org/licenses/by-sa/4.0/deed.zh-hans".equals(resource.getLicenseUrl())
                && resource.getRightsEvidence() != null && !resource.getRightsEvidence().isBlank()
                && resource.getContentNotice() != null && !resource.getContentNotice().isBlank()
                && resource.getVerifiedAt() != null
                && resource.getAuthorDeathYear() <= LocalDateTime.now().getYear() - 100
                && resource.getFirstPublicationYear() <= 1930;
        if (!complete) {
            throw BusinessException.forbidden(
                    "EBOOK_RIGHTS_NOT_VERIFIED", "电子资源权属材料不完整，已阻止发布");
        }
        return resource;
    }

    private void validateChapter(EbookResource resource, int chapterNumber) {
        if (chapterNumber < 1 || chapterNumber > resource.getChapterCount()) {
            throw BusinessException.badRequest("INVALID_CHAPTER", "章节编号超出范围");
        }
    }

    private EbookChapterResponse fetchChapter(EbookResource resource, int chapterNumber) {
        String pageName = String.format(resource.getSourcePagePattern(), chapterNumber);
        HttpUrl apiUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(API_HOST)
                .addPathSegment("w")
                .addPathSegment("api.php")
                .addQueryParameter("action", "parse")
                .addQueryParameter("page", pageName)
                .addQueryParameter("prop", "text|displaytitle")
                .addQueryParameter("format", "json")
                .addQueryParameter("formatversion", "2")
                .build();
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "LivingLibrary/1.0 (educational public-domain reader)")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw BusinessException.serviceUnavailable(
                        "EBOOK_SOURCE_UNAVAILABLE", "正版文本来源暂时不可用，请稍后重试");
            }
            JsonNode root = objectMapper.readTree(response.body().string());
            JsonNode parse = root.path("parse");
            String rawHtml = parse.path("text").asText("");
            if (rawHtml.isBlank()) {
                throw BusinessException.notFound("EBOOK_CHAPTER_NOT_FOUND", "来源站点未找到该章节");
            }
            String cleanHtml = sanitize(rawHtml);
            String displayTitle = Jsoup.parse(parse.path("displaytitle").asText("第 " + chapterNumber + " 回")).text();
            String sourcePageUrl = "https://zh.wikisource.org/wiki/"
                    + URLEncoder.encode(pageName, StandardCharsets.UTF_8)
                    .replace("+", "%20").replace("%2F", "/");
            return new EbookChapterResponse(
                    chapterNumber,
                    displayTitle,
                    cleanHtml,
                    sourcePageUrl,
                    resource.getLicenseName(),
                    resource.getLicenseUrl(),
                    "作品原作者见馆藏信息；数字文本由中文维基文库参与者整理",
                    "仅进行安全清洗和网页排版，未改写原文内容。外部来源若修订，显示内容可能随之更新。");
        } catch (IOException error) {
            throw BusinessException.serviceUnavailable(
                    "EBOOK_SOURCE_UNAVAILABLE", "正版文本来源暂时不可用，请稍后重试");
        }
    }

    private String sanitize(String rawHtml) {
        Document parsed = Jsoup.parseBodyFragment(rawHtml, "https://zh.wikisource.org/");
        parsed.select("script,style,iframe,object,embed,img,svg,canvas,form,input,button,"+
                ".mw-editsection,.ws-noexport,.noprint,.navbox,.licenseContainer,.headerContainer,"+
                ".sistersitebox,.plainSister,.catlinks,.printfooter").remove();
        parsed.select("a[href]").forEach(link -> link.attr("href", link.absUrl("href")));

        Safelist safelist = Safelist.relaxed()
                .removeTags("img")
                .addTags("ruby", "rt", "rp")
                .addAttributes("a", "href", "title")
                .addProtocols("a", "href", "https");
        Document cleaned = new Cleaner(safelist).clean(parsed);
        Element body = cleaned.body();
        String html = body.html();
        if (html.length() > MAX_CONTENT_LENGTH) {
            throw BusinessException.serviceUnavailable("EBOOK_CONTENT_REJECTED", "来源正文超出安全限制");
        }
        return html;
    }

    private ReadingProgressResponse toProgressResponse(ReadingProgress progress) {
        return new ReadingProgressResponse(
                progress.getChapterNumber(), progress.getScrollPercent(), progress.getUpdatedAt());
    }
}
