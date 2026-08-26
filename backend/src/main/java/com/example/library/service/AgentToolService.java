package com.example.library.service;

import com.example.library.dto.response.BookResponse;
import com.example.library.dto.response.BorrowRecordResponse;
import com.example.library.dto.response.NotificationResponse;
import com.example.library.dto.response.ReservationResponse;
import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgentToolService {

    private static final int MAX_SEARCH_LIMIT = 10;

    private final BookRepository bookRepository;
    private final BookKnowledgeService bookKnowledgeService;
    private final BorrowService borrowService;
    private final WishListService wishListService;
    private final ObjectMapper objectMapper;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final AgentActionService agentActionService;
    private final CatalogProposalService catalogProposalService;
    private final EbookService ebookService;

    public AgentToolService(
            BookRepository bookRepository,
            BookKnowledgeService bookKnowledgeService,
            BorrowService borrowService,
            WishListService wishListService,
            ObjectMapper objectMapper) {
        this(bookRepository, bookKnowledgeService, borrowService, wishListService, objectMapper,
                null, null, null, null, null);
    }

    @Autowired
    public AgentToolService(
            BookRepository bookRepository,
            BookKnowledgeService bookKnowledgeService,
            BorrowService borrowService,
            WishListService wishListService,
            ObjectMapper objectMapper,
            ReservationService reservationService,
            NotificationService notificationService,
            AgentActionService agentActionService,
            CatalogProposalService catalogProposalService,
            EbookService ebookService) {
        this.bookRepository = bookRepository;
        this.bookKnowledgeService = bookKnowledgeService;
        this.borrowService = borrowService;
        this.wishListService = wishListService;
        this.objectMapper = objectMapper;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
        this.agentActionService = agentActionService;
        this.catalogProposalService = catalogProposalService;
        this.ebookService = ebookService;
    }

    public List<Map<String, Object>> definitions() {
        return List.of(
                tool("search_catalog", "按书名、作者、分类、关键词或自然语言需求检索馆藏。需要推荐或查找图书时使用。",
                        Map.of(
                                "query", stringProperty("检索词或阅读需求，1到100个字符"),
                                "limit", integerProperty("返回数量，1到10，默认5", 1, MAX_SEARCH_LIMIT)),
                        List.of("query")),
                tool("get_book_details", "根据 ISBN 或准确书名查询馆藏位置、可借数量和图书详情。",
                        Map.of("isbn_or_title", stringProperty("ISBN 或书名")),
                        List.of("isbn_or_title")),
                tool("get_library_statistics", "查询实时馆藏总量、可借数量、分类和区域统计。",
                        Map.of(), List.of()),
                tool("get_my_borrowing", "查询当前登录用户的借阅记录、到期日和逾期情况。只能查询当前用户。",
                        Map.of("status", enumProperty("记录范围，默认 active", List.of("active", "overdue", "all"))),
                        List.of()),
                tool("get_my_wishlist", "查询当前登录用户的愿望单。只能查询当前用户。",
                        Map.of(), List.of()),
                tool("get_my_reservations", "查询当前登录用户的预约队列、到书状态和保留期限。",
                        Map.of(), List.of()),
                tool("get_my_notifications", "查询当前登录用户最近的站内通知。",
                        Map.of(), List.of()),
                tool("open_verified_ebook", "当用户明确要求打开、阅读或继续阅读某本书时调用。只能打开已经通过权属核验的古典原文；未核验图书会被服务端拒绝。未指定章节时恢复当前用户上次进度。",
                        Map.of(
                                "isbn_or_title", stringProperty("准确书名或 ISBN"),
                                "chapter", integerProperty("可选章节编号；仅在用户明确指定时填写", 1, 120)),
                        List.of("isbn_or_title")),
                tool("prepare_action", "为预约、取消预约、加入愿望单或续借创建待用户确认的操作草案。此工具不会直接执行操作。",
                        Map.of(
                                "action", enumProperty("操作类型", List.of(
                                        "RESERVE_BOOK", "CANCEL_RESERVATION", "ADD_WISHLIST", "RENEW_BORROW")),
                                "isbn_or_title", stringProperty("ISBN或准确书名")),
                        List.of("action", "isbn_or_title")),
                tool("propose_catalog_management", "仅供管理员或馆员使用。根据一个或多个书名查询真实候选版本，并生成新增、增减馆藏、停借或删除的可选方案卡。不得直接修改馆藏。",
                        Map.of("titles", arrayProperty("需要管理的书名列表，1到5项", stringProperty("准确或近似书名"), 1, 5)),
                        List.of("titles"))
        );
    }

    public String execute(String toolName, String rawArguments, String userId) {
        try {
            JsonNode arguments = parseArguments(rawArguments);
            return switch (toolName) {
                case "search_catalog" -> searchCatalog(arguments);
                case "get_book_details" -> getBookDetails(arguments);
                case "get_library_statistics" -> getLibraryStatistics(arguments);
                case "get_my_borrowing" -> getMyBorrowing(arguments, userId);
                case "get_my_wishlist" -> getMyWishlist(arguments, userId);
                case "get_my_reservations" -> getMyReservations(arguments, userId);
                case "get_my_notifications" -> getMyNotifications(arguments, userId);
                case "open_verified_ebook" -> openVerifiedEbook(arguments, userId);
                case "prepare_action" -> prepareAction(arguments, userId);
                case "propose_catalog_management" -> proposeCatalogManagement(arguments, userId);
                default -> error("未知工具：" + toolName);
            };
        } catch (IllegalArgumentException | com.example.library.exception.BusinessException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("工具执行失败，请调整参数后重试");
        }
    }

    public String displayName(String toolName) {
        return switch (toolName) {
            case "search_catalog" -> "馆藏检索";
            case "get_book_details" -> "图书详情查询";
            case "get_library_statistics" -> "馆藏统计";
            case "get_my_borrowing" -> "个人借阅查询";
            case "get_my_wishlist" -> "个人愿望单查询";
            case "get_my_reservations" -> "个人预约查询";
            case "get_my_notifications" -> "站内通知查询";
            case "open_verified_ebook" -> "合规电子书阅读";
            case "prepare_action" -> "操作确认草案";
            case "propose_catalog_management" -> "馆藏管理方案";
            default -> "数据查询";
        };
    }

    private String searchCatalog(JsonNode arguments) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of("query", "limit"));
        String query = requiredText(arguments, "query", 100);
        int limit = optionalInt(arguments, "limit", 5, 1, MAX_SEARCH_LIMIT);
        List<Map<String, Object>> books = bookKnowledgeService.searchRelevantBooks(query, limit).stream()
                .map(this::bookSummary)
                .toList();
        return success(Map.of("query", query, "count", books.size(), "books", books));
    }

    private String getBookDetails(JsonNode arguments) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of("isbn_or_title"));
        String identifier = requiredText(arguments, "isbn_or_title", 100);
        Book book = bookRepository.findById(identifier).orElse(null);
        if (book == null) {
            book = bookRepository.findAll().stream()
                    .filter(item -> item.getTitle() != null && item.getTitle().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseGet(() -> bookRepository.searchBooks(identifier).stream().findFirst().orElse(null));
        }
        if (book == null) {
            return success(Map.of("found", false, "message", "馆藏中未找到该图书"));
        }
        Map<String, Object> details = new LinkedHashMap<>(bookSummary(book));
        details.put("publisher", safe(book.getPublisher()));
        details.put("keywords", safe(book.getKeywords()));
        details.put("description", truncate(safe(book.getDescription()), 400));
        return success(Map.of("found", true, "book", details));
    }

    private String getLibraryStatistics(JsonNode arguments) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of());
        List<Book> books = bookRepository.findAll();
        int totalCopies = books.stream().mapToInt(book -> value(book.getTotalCount())).sum();
        int borrowedCopies = books.stream().mapToInt(book -> value(book.getBorrowedCount())).sum();
        Map<String, Long> categories = books.stream().collect(Collectors.groupingBy(
                book -> safeCategory(book.getCategory()), LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> areas = books.stream()
                .filter(book -> book.getLocation() != null && !book.getLocation().isBlank())
                .collect(Collectors.groupingBy(
                        book -> book.getLocation().split("-")[0], LinkedHashMap::new, Collectors.counting()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("titleCount", books.size());
        result.put("totalCopies", totalCopies);
        result.put("availableCopies", Math.max(0, totalCopies - borrowedCopies));
        result.put("categories", categories);
        result.put("areas", areas);
        return success(result);
    }

    private String getMyBorrowing(JsonNode arguments, String userId) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of("status"));
        String status = optionalText(arguments, "status", "active", 20);
        if (!Set.of("active", "overdue", "all").contains(status)) {
            throw new IllegalArgumentException("status 只能是 active、overdue 或 all");
        }
        List<BorrowRecordResponse> records = borrowService.getUserBorrowRecords(userId).stream()
                .filter(record -> switch (status) {
                    case "active" -> "BORROWED".equals(record.getStatus());
                    case "overdue" -> "BORROWED".equals(record.getStatus())
                            && record.getDaysOverdue() != null && record.getDaysOverdue() > 0;
                    default -> true;
                })
                .sorted(Comparator.comparing(BorrowRecordResponse::getBorrowDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .toList();
        List<Map<String, Object>> safeRecords = records.stream().map(record -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("isbn", record.getIsbn());
            item.put("bookTitle", record.getBookTitle());
            item.put("borrowDate", record.getBorrowDate());
            item.put("dueDate", record.getDueDate());
            item.put("returnDate", record.getReturnDate());
            item.put("status", record.getStatus());
            item.put("daysOverdue", record.getDaysOverdue());
            return item;
        }).toList();
        return success(Map.of("scope", status, "count", safeRecords.size(), "records", safeRecords));
    }

    private String getMyWishlist(JsonNode arguments, String userId) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of());
        List<Map<String, Object>> books = wishListService.getUserWishList(userId).stream()
                .limit(20)
                .map(this::bookSummary)
                .toList();
        return success(Map.of("count", books.size(), "books", books));
    }

    private String getMyReservations(JsonNode arguments, String userId) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of());
        if (reservationService == null) return error("预约服务暂不可用");
        List<ReservationResponse> reservations = reservationService.getUserReservations(userId).stream()
                .limit(20)
                .toList();
        return success(Map.of("count", reservations.size(), "reservations", reservations));
    }

    private String getMyNotifications(JsonNode arguments, String userId) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of());
        if (notificationService == null) return error("通知服务暂不可用");
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId).stream()
                .limit(10)
                .toList();
        return success(Map.of("count", notifications.size(), "notifications", notifications));
    }

    private String openVerifiedEbook(JsonNode arguments, String userId) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of("isbn_or_title", "chapter"));
        if (ebookService == null) return error("电子阅读服务暂不可用");
        String identifier = requiredText(arguments, "isbn_or_title", 100);
        Integer chapter = arguments.hasNonNull("chapter")
                ? optionalInt(arguments, "chapter", 1, 1, 120)
                : null;
        return success(ebookService.prepareReaderNavigation(identifier, chapter, userId));
    }

    private String prepareAction(JsonNode arguments, String userId) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of("action", "isbn_or_title"));
        if (agentActionService == null) return error("操作确认服务暂不可用");
        String action = requiredText(arguments, "action", 40);
        String identifier = requiredText(arguments, "isbn_or_title", 100);
        return success(agentActionService.prepare(userId, action, identifier));
    }

    private String proposeCatalogManagement(JsonNode arguments, String userId) throws JsonProcessingException {
        rejectUnknown(arguments, Set.of("titles"));
        if (catalogProposalService == null) return error("馆藏方案服务暂不可用");
        JsonNode titlesNode = arguments.get("titles");
        if (titlesNode == null || !titlesNode.isArray() || titlesNode.isEmpty() || titlesNode.size() > 5) {
            throw new IllegalArgumentException("titles 必须是包含 1 到 5 个书名的数组");
        }
        List<String> titles = new java.util.ArrayList<>();
        for (JsonNode title : titlesNode) {
            if (!title.isTextual() || title.asText().isBlank() || title.asText().length() > 100) {
                throw new IllegalArgumentException("每个书名必须是 1 到 100 个字符的字符串");
            }
            titles.add(title.asText().trim());
        }
        return success(catalogProposalService.propose(userId, titles));
    }

    private Map<String, Object> bookSummary(Book book) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("isbn", safe(book.getIsbn()));
        item.put("title", safe(book.getTitle()));
        item.put("author", safe(book.getAuthor()));
        item.put("category", safeCategory(book.getCategory()));
        item.put("location", safe(book.getLocation()));
        item.put("availableCount", Math.max(0, value(book.getTotalCount()) - value(book.getBorrowedCount())));
        item.put("totalCount", value(book.getTotalCount()));
        item.put("borrowable", book.getBorrowable() == null || book.getBorrowable());
        return item;
    }

    private Map<String, Object> bookSummary(BookResponse book) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("isbn", safe(book.getIsbn()));
        item.put("title", safe(book.getTitle()));
        item.put("author", safe(book.getAuthor()));
        item.put("category", safeCategory(book.getCategory()));
        item.put("location", safe(book.getLocation()));
        item.put("availableCount", value(book.getAvailableCount()));
        item.put("totalCount", value(book.getTotalCount()));
        item.put("borrowable", book.getBorrowable() == null || book.getBorrowable());
        return item;
    }

    private JsonNode parseArguments(String rawArguments) throws JsonProcessingException {
        if (rawArguments == null || rawArguments.isBlank()) {
            return objectMapper.createObjectNode();
        }
        JsonNode node = objectMapper.readTree(rawArguments);
        if (!node.isObject()) {
            throw new IllegalArgumentException("工具参数必须是 JSON 对象");
        }
        return node;
    }

    private void rejectUnknown(JsonNode arguments, Set<String> allowed) {
        Set<String> unknown = new LinkedHashSet<>();
        arguments.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name)) unknown.add(name);
        });
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("存在未定义参数：" + String.join(", ", unknown));
        }
    }

    private String requiredText(JsonNode arguments, String field, int maxLength) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " 是必填字符串");
        }
        return bounded(value.asText().trim(), field, maxLength);
    }

    private String optionalText(JsonNode arguments, String field, String defaultValue, int maxLength) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull()) return defaultValue;
        if (!value.isTextual()) throw new IllegalArgumentException(field + " 必须是字符串");
        return bounded(value.asText().trim(), field, maxLength);
    }

    private int optionalInt(JsonNode arguments, String field, int defaultValue, int min, int max) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull()) return defaultValue;
        if (!value.canConvertToInt()) throw new IllegalArgumentException(field + " 必须是整数");
        int parsed = value.asInt();
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(field + " 必须在 " + min + " 到 " + max + " 之间");
        }
        return parsed;
    }

    private String bounded(String value, String field, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " 长度不能超过 " + maxLength);
        }
        return value;
    }

    private String success(Object data) throws JsonProcessingException {
        return objectMapper.writeValueAsString(Map.of("ok", true, "data", data));
    }

    private String error(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("ok", false, "error", message));
        } catch (JsonProcessingException ignored) {
            return "{\"ok\":false,\"error\":\"tool_error\"}";
        }
    }

    private Map<String, Object> tool(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> required) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);
        parameters.put("additionalProperties", false);
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters));
    }

    private Map<String, Object> stringProperty(String description) {
        return Map.of("type", "string", "description", description);
    }

    private Map<String, Object> integerProperty(String description, int minimum, int maximum) {
        return Map.of("type", "integer", "description", description, "minimum", minimum, "maximum", maximum);
    }

    private Map<String, Object> enumProperty(String description, List<String> values) {
        return Map.of("type", "string", "description", description, "enum", values);
    }

    private Map<String, Object> arrayProperty(
            String description, Map<String, Object> items, int minimum, int maximum) {
        return Map.of(
                "type", "array",
                "description", description,
                "items", items,
                "minItems", minimum,
                "maxItems", maximum);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未知" : value;
    }

    private String safeCategory(String value) {
        return value == null || value.isBlank() ? "未分类" : value;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}
