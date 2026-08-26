package com.example.library.service;

import com.example.library.dto.response.AgentActionDraftResponse;
import com.example.library.dto.request.BookRequest;
import com.example.library.dto.request.CatalogActionPrepareRequest;
import com.example.library.entity.AgentActionDraft;
import com.example.library.entity.Book;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.repository.AgentActionDraftRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
public class AgentActionService {

    private static final List<String> SUPPORTED_ACTIONS = List.of(
            "RESERVE_BOOK", "CANCEL_RESERVATION", "ADD_WISHLIST", "RENEW_BORROW",
            "ADD_BOOK", "INCREASE_STOCK", "REDUCE_STOCK", "DISABLE_BOOK", "ENABLE_BOOK", "DELETE_BOOK");
    private static final List<String> MANAGEMENT_ACTIONS = List.of(
            "ADD_BOOK", "INCREASE_STOCK", "REDUCE_STOCK", "DISABLE_BOOK", "ENABLE_BOOK", "DELETE_BOOK");

    private final AgentActionDraftRepository draftRepository;
    private final BookRepository bookRepository;
    private final ReservationService reservationService;
    private final WishListService wishListService;
    private final BorrowService borrowService;
    private final OperationLogService operationLogService;
    private final BookService bookService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.agent-action.expiration-minutes:5}")
    private int expirationMinutes;

    public AgentActionService(
            AgentActionDraftRepository draftRepository,
            BookRepository bookRepository,
            ReservationService reservationService,
            WishListService wishListService,
            BorrowService borrowService,
            OperationLogService operationLogService) {
        this(draftRepository, bookRepository, reservationService, wishListService, borrowService,
                operationLogService, null, null, new ObjectMapper());
    }

    @Autowired
    public AgentActionService(
            AgentActionDraftRepository draftRepository,
            BookRepository bookRepository,
            ReservationService reservationService,
            WishListService wishListService,
            BorrowService borrowService,
            OperationLogService operationLogService,
            BookService bookService,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.draftRepository = draftRepository;
        this.bookRepository = bookRepository;
        this.reservationService = reservationService;
        this.wishListService = wishListService;
        this.borrowService = borrowService;
        this.operationLogService = operationLogService;
        this.bookService = bookService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentActionDraftResponse prepare(String userId, String actionType, String bookIdentifier) {
        String normalizedAction = normalizeAction(actionType);
        Book book = resolveBook(bookIdentifier);
        validateDraftIntent(userId, normalizedAction, book);

        LocalDateTime now = LocalDateTime.now();
        AgentActionDraft draft = new AgentActionDraft();
        draft.setToken(UUID.randomUUID().toString().replace("-", ""));
        draft.setUserId(userId);
        draft.setActionType(normalizedAction);
        draft.setIsbn(book.getIsbn());
        draft.setStatus("PENDING");
        draft.setSummary(summary(normalizedAction, book.getTitle()));
        draft.setCreatedAt(now);
        draft.setExpiresAt(now.plusMinutes(Math.max(1, expirationMinutes)));
        draftRepository.save(draft);
        operationLogService.log(userId, "AGENT_USER", "Agent生成操作草案", draft.getSummary());
        return toResponse(draft, book.getTitle());
    }

    @Transactional
    public AgentActionDraftResponse prepareManagement(
            String userId, String currentRole, CatalogActionPrepareRequest request) {
        String action = normalizeAction(request.getActionType());
        requireManagementRole(currentRole, action);
        if (!MANAGEMENT_ACTIONS.contains(action)) {
            throw BusinessException.badRequest("UNSUPPORTED_AGENT_ACTION", "该操作不是馆藏管理操作");
        }

        Map<String, Object> payload = validateManagementPayload(action, request);
        String isbn = stringValue(payload.get("isbn"));
        String title = stringValue(payload.get("title"));
        LocalDateTime now = LocalDateTime.now();
        AgentActionDraft draft = new AgentActionDraft();
        draft.setToken(UUID.randomUUID().toString().replace("-", ""));
        draft.setUserId(userId);
        draft.setActionType(action);
        draft.setIsbn(isbn);
        draft.setStatus("PENDING");
        draft.setSummary(managementSummary(action, title, payload));
        draft.setPayloadJson(writePayload(payload));
        draft.setCreatedAt(now);
        draft.setExpiresAt(now.plusMinutes(Math.max(1, expirationMinutes)));
        draftRepository.save(draft);
        operationLogService.log(userId, currentRole, "Agent生成馆藏操作草案", draft.getSummary());
        return toResponse(draft, title);
    }

    public AgentActionDraftResponse findPendingCreatedAfter(String userId, LocalDateTime createdAfter) {
        return draftRepository
                .findFirstByUserIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        userId, "PENDING", createdAfter)
                .filter(draft -> draft.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(draft -> toResponse(draft, titleOf(draft.getIsbn())))
                .orElse(null);
    }

    @Transactional
    public AgentActionDraftResponse confirm(String userId, String token) {
        AgentActionDraft draft = draftRepository.findByTokenAndUserIdForUpdate(token, userId)
                .orElseThrow(() -> BusinessException.notFound("AGENT_ACTION_NOT_FOUND", "操作草案不存在"));
        if ("CONFIRMED".equals(draft.getStatus())) return toResponse(draft, titleOf(draft.getIsbn()));
        if (!"PENDING".equals(draft.getStatus())) {
            throw BusinessException.conflict("AGENT_ACTION_NOT_PENDING", "操作草案已经失效或取消");
        }
        if (!draft.getExpiresAt().isAfter(LocalDateTime.now())) {
            draft.setStatus("EXPIRED");
            draftRepository.save(draft);
            throw BusinessException.conflict("AGENT_ACTION_EXPIRED", "操作确认已过期，请重新发起");
        }

        execute(draft);
        draft.setStatus("CONFIRMED");
        draft.setConfirmedAt(LocalDateTime.now());
        draftRepository.save(draft);
        operationLogService.log(userId, "AGENT_USER", "确认Agent操作", draft.getSummary());
        return toResponse(draft, titleOf(draft.getIsbn()));
    }

    @Transactional
    public void cancel(String userId, String token) {
        AgentActionDraft draft = draftRepository.findByTokenAndUserIdForUpdate(token, userId)
                .orElseThrow(() -> BusinessException.notFound("AGENT_ACTION_NOT_FOUND", "操作草案不存在"));
        if ("PENDING".equals(draft.getStatus())) {
            draft.setStatus("CANCELLED");
            draftRepository.save(draft);
        }
    }

    private void execute(AgentActionDraft draft) {
        if (MANAGEMENT_ACTIONS.contains(draft.getActionType())) {
            executeManagement(draft);
            return;
        }
        switch (draft.getActionType()) {
            case "RESERVE_BOOK" -> reservationService.reserve(draft.getUserId(), draft.getIsbn());
            case "CANCEL_RESERVATION" -> reservationService.cancelByBook(draft.getUserId(), draft.getIsbn());
            case "ADD_WISHLIST" -> wishListService.addToWishList(draft.getUserId(), draft.getIsbn());
            case "RENEW_BORROW" -> borrowService.renewBook(draft.getUserId(), draft.getIsbn());
            default -> throw BusinessException.badRequest("UNSUPPORTED_AGENT_ACTION", "不支持的Agent操作");
        }
    }

    private void executeManagement(AgentActionDraft draft) {
        if (bookService == null || userRepository == null) {
            throw BusinessException.conflict("AGENT_MANAGEMENT_UNAVAILABLE", "馆藏管理服务暂不可用");
        }
        User user = userRepository.findById(draft.getUserId())
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .orElseThrow(() -> BusinessException.unauthorized("ACCOUNT_DISABLED", "账号不存在或已停用"));
        requireManagementRole(user.getRole(), draft.getActionType());
        Map<String, Object> payload = readPayload(draft.getPayloadJson());
        String isbn = stringValue(payload.get("isbn"));
        switch (draft.getActionType()) {
            case "ADD_BOOK" -> bookService.addBook(toBookRequest(payload), draft.getUserId());
            case "INCREASE_STOCK" -> bookService.increaseStock(
                    isbn, intValue(payload.get("quantity")), stringValue(payload.get("location")), draft.getUserId());
            case "REDUCE_STOCK" -> bookService.reduceStock(
                    isbn, intValue(payload.get("quantity")), draft.getUserId());
            case "DISABLE_BOOK" -> bookService.setBorrowable(isbn, false, draft.getUserId());
            case "ENABLE_BOOK" -> bookService.setBorrowable(isbn, true, draft.getUserId());
            case "DELETE_BOOK" -> bookService.deleteBookStrict(isbn, draft.getUserId());
            default -> throw BusinessException.badRequest("UNSUPPORTED_AGENT_ACTION", "不支持的馆藏管理操作");
        }
    }

    private void validateDraftIntent(String userId, String actionType, Book book) {
        switch (actionType) {
            case "CANCEL_RESERVATION" -> reservationService.getUserReservations(userId).stream()
                    .filter(item -> item.isbn().equals(book.getIsbn()))
                    .filter(item -> List.of("WAITING", "NOTIFIED").contains(item.status()))
                    .findFirst()
                    .orElseThrow(() -> BusinessException.notFound("RESERVATION_NOT_FOUND", "没有找到有效预约"));
            case "ADD_WISHLIST" -> {
                if (wishListService.isInWishList(userId, book.getIsbn())) {
                    throw BusinessException.conflict("WISHLIST_DUPLICATE", "该图书已在愿望单中");
                }
            }
            case "RENEW_BORROW" -> borrowService.getUserBorrowRecords(userId).stream()
                    .filter(item -> item.getIsbn().equals(book.getIsbn()) && "BORROWED".equals(item.getStatus()))
                    .findFirst()
                    .orElseThrow(() -> BusinessException.notFound("ACTIVE_BORROW_NOT_FOUND", "未找到有效借阅记录"));
            default -> { }
        }
    }

    private Book resolveBook(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw BusinessException.badRequest("BOOK_IDENTIFIER_REQUIRED", "请提供ISBN或准确书名");
        }
        String value = identifier.trim();
        return bookRepository.findById(value)
                .orElseGet(() -> bookRepository.findAll().stream()
                        .filter(book -> book.getTitle() != null && book.getTitle().equalsIgnoreCase(value))
                        .findFirst()
                        .orElseGet(() -> bookRepository.searchBooks(value).stream().findFirst()
                                .orElseThrow(() -> BusinessException.notFound("BOOK_NOT_FOUND", "馆藏中未找到该图书"))));
    }

    private String normalizeAction(String actionType) {
        String normalized = actionType == null ? "" : actionType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ACTIONS.contains(normalized)) {
            throw BusinessException.badRequest("UNSUPPORTED_AGENT_ACTION", "不支持的Agent操作");
        }
        return normalized;
    }

    private String summary(String actionType, String title) {
        return switch (actionType) {
            case "RESERVE_BOOK" -> "预约《" + title + "》";
            case "CANCEL_RESERVATION" -> "取消《" + title + "》的预约";
            case "ADD_WISHLIST" -> "将《" + title + "》加入愿望单";
            case "RENEW_BORROW" -> "续借《" + title + "》";
            default -> title;
        };
    }

    private String titleOf(String isbn) {
        return bookRepository.findById(isbn).map(Book::getTitle).orElse(isbn);
    }

    private AgentActionDraftResponse toResponse(AgentActionDraft draft, String title) {
        Map<String, Object> details = readPayload(draft.getPayloadJson());
        String displayTitle = stringValue(details.get("title"));
        if (displayTitle == null || displayTitle.isBlank()) displayTitle = title;
        return new AgentActionDraftResponse(
                draft.getToken(), draft.getActionType(), draft.getIsbn(), displayTitle,
                draft.getSummary(), draft.getStatus(), draft.getExpiresAt(), details);
    }

    private Map<String, Object> validateManagementPayload(String action, CatalogActionPrepareRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "query", request.getQuery());
        if ("ADD_BOOK".equals(action)) {
            String isbn = required(request.getIsbn(), "ISBN");
            String title = required(request.getTitle(), "书名");
            String author = required(request.getAuthor(), "作者");
            int quantity = validQuantity(request.getQuantity());
            if (bookRepository.existsById(isbn)) {
                throw BusinessException.conflict("BOOK_ISBN_EXISTS", "该 ISBN 已存在，请选择增加馆藏");
            }
            payload.put("isbn", isbn);
            payload.put("title", title);
            payload.put("author", author);
            putIfPresent(payload, "publisher", request.getPublisher());
            putIfPresent(payload, "category", request.getCategory());
            payload.put("quantity", quantity);
            putIfPresent(payload, "location", request.getLocation());
            putIfPresent(payload, "keywords", request.getKeywords());
            putIfPresent(payload, "description", request.getDescription());
            return payload;
        }

        Book book = bookRepository.findById(required(request.getIsbn(), "ISBN"))
                .orElseThrow(() -> BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在"));
        payload.put("isbn", book.getIsbn());
        payload.put("title", book.getTitle());
        payload.put("currentTotalCount", book.getTotalCount());
        payload.put("borrowedCount", book.getBorrowedCount());
        if (List.of("INCREASE_STOCK", "REDUCE_STOCK").contains(action)) {
            payload.put("quantity", validQuantity(request.getQuantity()));
        }
        if ("INCREASE_STOCK".equals(action)) putIfPresent(payload, "location", request.getLocation());
        return payload;
    }

    private void requireManagementRole(String role, String action) {
        if (!List.of("ADMIN", "LIBRARIAN").contains(role)) {
            throw BusinessException.forbidden("STAFF_ROLE_REQUIRED", "只有管理员或馆员可以管理馆藏");
        }
        if ("DELETE_BOOK".equals(action) && !"ADMIN".equals(role)) {
            throw BusinessException.forbidden("ADMIN_ROLE_REQUIRED", "彻底删除书目只允许管理员执行");
        }
    }

    private String managementSummary(String action, String title, Map<String, Object> payload) {
        return switch (action) {
            case "ADD_BOOK" -> "新增书目《" + title + "》，馆藏 " + payload.get("quantity") + " 本";
            case "INCREASE_STOCK" -> "为《" + title + "》增加 " + payload.get("quantity") + " 本馆藏";
            case "REDUCE_STOCK" -> "为《" + title + "》减少 " + payload.get("quantity") + " 本馆藏";
            case "DISABLE_BOOK" -> "停止《" + title + "》借阅";
            case "ENABLE_BOOK" -> "恢复《" + title + "》借阅";
            case "DELETE_BOOK" -> "彻底删除书目《" + title + "》";
            default -> title;
        };
    }

    private BookRequest toBookRequest(Map<String, Object> payload) {
        BookRequest request = new BookRequest();
        request.setIsbn(stringValue(payload.get("isbn")));
        request.setTitle(stringValue(payload.get("title")));
        request.setAuthor(stringValue(payload.get("author")));
        request.setPublisher(stringValue(payload.get("publisher")));
        request.setCategory(stringValue(payload.get("category")));
        request.setTotalCount(intValue(payload.get("quantity")));
        request.setLocation(stringValue(payload.get("location")));
        request.setKeywords(stringValue(payload.get("keywords")));
        request.setDescription(stringValue(payload.get("description")));
        request.setBorrowable(true);
        return request;
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("INVALID_AGENT_PAYLOAD", "操作参数无法序列化");
        }
    }

    private Map<String, Object> readPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw BusinessException.conflict("INVALID_AGENT_PAYLOAD", "操作草案参数已损坏");
        }
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest("CATALOG_FIELD_REQUIRED", label + "不能为空");
        }
        return value.trim();
    }

    private int validQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 1000) {
            throw BusinessException.badRequest("INVALID_STOCK_QUANTITY", "数量必须在 1 到 1000 之间");
        }
        return quantity;
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) payload.put(key, value.trim());
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }
}
