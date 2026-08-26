package com.example.library.service;

import com.example.library.dto.request.CatalogActionPrepareRequest;
import com.example.library.dto.response.AgentActionDraftResponse;
import com.example.library.dto.response.CatalogCandidateResponse;
import com.example.library.dto.response.CatalogManagementProposalResponse;
import com.example.library.dto.response.CatalogProposalGroupResponse;
import com.example.library.entity.AgentCatalogProposal;
import com.example.library.entity.Book;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.repository.AgentCatalogProposalRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CatalogProposalService {

    private static final int MAX_QUERIES = 5;
    private static final int MAX_CANDIDATES = 5;

    private final AgentCatalogProposalRepository proposalRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final AgentActionService agentActionService;
    private final ObjectMapper objectMapper;

    @Value("${app.agent-action.expiration-minutes:5}")
    private int expirationMinutes;

    public CatalogProposalService(
            AgentCatalogProposalRepository proposalRepository,
            BookRepository bookRepository,
            UserRepository userRepository,
            AgentActionService agentActionService,
            ObjectMapper objectMapper) {
        this.proposalRepository = proposalRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.agentActionService = agentActionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CatalogManagementProposalResponse propose(String userId, List<String> requestedTitles) {
        User user = requireStaff(userId);
        List<String> queries = normalizeQueries(requestedTitles);
        LocalDateTime now = LocalDateTime.now();
        AgentCatalogProposal proposal = new AgentCatalogProposal();
        proposal.setToken(UUID.randomUUID().toString().replace("-", ""));
        proposal.setUserId(userId);
        proposal.setQueriesJson(writeQueries(queries));
        proposal.setStatus("PENDING");
        proposal.setCreatedAt(now);
        proposal.setExpiresAt(now.plusMinutes(Math.max(1, expirationMinutes)));
        proposalRepository.save(proposal);
        return toResponse(proposal, user.getRole());
    }

    public CatalogManagementProposalResponse findPendingCreatedAfter(String userId, LocalDateTime createdAfter) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !isStaff(user.getRole())) return null;
        return proposalRepository
                .findFirstByUserIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        userId, "PENDING", createdAfter)
                .filter(item -> item.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(item -> toResponse(item, user.getRole()))
                .orElse(null);
    }

    @Transactional
    public AgentActionDraftResponse prepareAction(
            String userId, String proposalToken, CatalogActionPrepareRequest request) {
        User user = requireStaff(userId);
        AgentCatalogProposal proposal = proposalRepository.findByTokenAndUserIdForUpdate(proposalToken, userId)
                .orElseThrow(() -> BusinessException.notFound("CATALOG_PROPOSAL_NOT_FOUND", "馆藏方案不存在"));
        if (!"PENDING".equals(proposal.getStatus()) || !proposal.getExpiresAt().isAfter(LocalDateTime.now())) {
            proposal.setStatus("EXPIRED");
            proposalRepository.save(proposal);
            throw BusinessException.conflict("CATALOG_PROPOSAL_EXPIRED", "馆藏方案已过期，请重新让 Agent 生成");
        }
        String query = request.getQuery() == null ? "" : request.getQuery().trim();
        List<String> queries = readQueries(proposal.getQueriesJson());
        if (queries.stream().noneMatch(item -> item.equalsIgnoreCase(query))) {
            throw BusinessException.badRequest("INVALID_CATALOG_QUERY", "所选书名不属于当前方案");
        }

        String action = request.getActionType().trim().toUpperCase(Locale.ROOT);
        if (!"ADD_BOOK".equals(action)) {
            String isbn = request.getIsbn() == null ? "" : request.getIsbn().trim();
            boolean offered = findCandidates(query).stream().anyMatch(book -> book.getIsbn().equals(isbn));
            if (!offered) {
                throw BusinessException.badRequest("INVALID_CATALOG_CANDIDATE", "所选书目不属于当前候选方案");
            }
        }
        return agentActionService.prepareManagement(userId, user.getRole(), request);
    }

    private CatalogManagementProposalResponse toResponse(AgentCatalogProposal proposal, String role) {
        List<CatalogProposalGroupResponse> groups = readQueries(proposal.getQueriesJson()).stream()
                .map(query -> new CatalogProposalGroupResponse(
                        query,
                        findCandidates(query).stream().map(book -> toCandidate(book, role)).toList(),
                        true))
                .toList();
        return new CatalogManagementProposalResponse(
                proposal.getToken(), proposal.getStatus(), role, groups, proposal.getExpiresAt());
    }

    private CatalogCandidateResponse toCandidate(Book book, String role) {
        List<String> operations = new ArrayList<>();
        operations.add("INCREASE_STOCK");
        operations.add("REDUCE_STOCK");
        operations.add(Boolean.FALSE.equals(book.getBorrowable()) ? "ENABLE_BOOK" : "DISABLE_BOOK");
        if ("ADMIN".equals(role)) operations.add("DELETE_BOOK");
        return new CatalogCandidateResponse(
                book.getIsbn(), book.getTitle(), book.getAuthor(), book.getPublisher(), book.getCategory(),
                value(book.getTotalCount()), value(book.getBorrowedCount()), book.getLocation(),
                !Boolean.FALSE.equals(book.getBorrowable()), List.copyOf(operations));
    }

    private List<Book> findCandidates(String query) {
        LinkedHashMap<String, Book> results = new LinkedHashMap<>();
        bookRepository.findById(query).ifPresent(book -> results.put(book.getIsbn(), book));
        bookRepository.findAll().stream()
                .filter(book -> book.getTitle() != null && book.getTitle().equalsIgnoreCase(query))
                .forEach(book -> results.putIfAbsent(book.getIsbn(), book));
        bookRepository.searchBooks(query).forEach(book -> results.putIfAbsent(book.getIsbn(), book));
        return results.values().stream().limit(MAX_CANDIDATES).toList();
    }

    private User requireStaff(String userId) {
        User user = userRepository.findById(userId)
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .orElseThrow(() -> BusinessException.unauthorized("ACCOUNT_DISABLED", "账号不存在或已停用"));
        if (!isStaff(user.getRole())) {
            throw BusinessException.forbidden("STAFF_ROLE_REQUIRED", "只有管理员或馆员可以生成馆藏管理方案");
        }
        return user;
    }

    private boolean isStaff(String role) {
        return List.of("ADMIN", "LIBRARIAN").contains(role);
    }

    private List<String> normalizeQueries(List<String> requestedTitles) {
        if (requestedTitles == null) return List.of();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String title : requestedTitles) {
            if (title != null && !title.isBlank()) values.add(title.trim());
            if (values.size() == MAX_QUERIES) break;
        }
        if (values.isEmpty()) {
            throw BusinessException.badRequest("BOOK_TITLES_REQUIRED", "请至少提供一个书名");
        }
        if (values.stream().anyMatch(value -> value.length() > 100)) {
            throw BusinessException.badRequest("BOOK_TITLE_TOO_LONG", "书名不能超过 100 个字符");
        }
        return List.copyOf(values);
    }

    private String writeQueries(List<String> queries) {
        try {
            return objectMapper.writeValueAsString(queries);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("INVALID_CATALOG_PROPOSAL", "书名列表无法序列化");
        }
    }

    private List<String> readQueries(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw BusinessException.conflict("INVALID_CATALOG_PROPOSAL", "馆藏方案数据已损坏");
        }
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }
}
