package com.example.library.service;

import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookKnowledgeService {

    @Autowired
    private BookRepository bookRepository;

    private volatile List<Book> cachedBooks = List.of();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        cachedBooks = bookRepository.findAll();
    }

    public int getTotalBookCount() {
        refresh();
        return cachedBooks.size();
    }

    /**
     * 基于馆藏数据直接回答（不调用大模型），无法处理时返回 null。
     */
    public String buildDirectAnswer(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        refresh();
        String q = question.trim();

        if (isRecommendQuestion(q)) {
            return buildRecommendAnswer(q);
        }
        if (isCountQuestion(q)) {
            return buildCountAnswer();
        }
        if (isLocationQuestion(q)) {
            return buildLocationAnswer(q);
        }
        Book matched = findBookByTitleInQuestion(q);
        if (matched != null && (q.contains("详情") || q.contains("信息") || q.contains("介绍") || q.contains("是什么"))) {
            return buildBookDetailAnswer(matched);
        }
        return null;
    }

    public void streamDirectAnswer(String question, java.util.function.Consumer<String> onChunk) {
        String answer = buildDirectAnswer(question);
        if (answer == null) {
            return;
        }
        for (int i = 0; i < answer.length(); i++) {
            onChunk.accept(String.valueOf(answer.charAt(i)));
            try {
                Thread.sleep(38);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public boolean hasDirectAnswer(String question) {
        return buildDirectAnswer(question) != null;
    }

    private boolean isCountQuestion(String q) {
        if (q.contains("推荐")) return false;
        return q.contains("多少") || q.contains("总共") || q.contains("一共")
                || q.contains("总数") || q.contains("多少本") || q.contains("馆藏量")
                || q.contains("图书数量") || q.contains("共有几") || q.contains("一共几");
    }

    private boolean isRecommendQuestion(String q) {
        return q.contains("推荐") || q.contains("建议读") || q.contains("有什么书") || q.contains("读什么")
                || q.contains("书单") || q.contains("借什么") || q.contains("看什么书");
    }

    private boolean isLocationQuestion(String q) {
        return q.contains("在哪") || q.contains("哪里") || q.contains("位置") || q.contains("哪儿")
                || q.contains("放在") || q.contains("馆藏");
    }

    private String buildCountAnswer() {
        int total = cachedBooks.size();
        StringBuilder sb = new StringBuilder();
        sb.append("本图书馆共有 ").append(total).append(" 本图书。\n\n");
        sb.append("【分类统计】\n");
        cachedBooks.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCategory() != null ? b.getCategory() : "未分类",
                        Collectors.counting()))
                .forEach((cat, count) -> sb.append("• ").append(cat).append("：").append(count).append(" 本\n"));
        sb.append("\n【馆藏区域】\n");
        cachedBooks.stream()
                .filter(b -> b.getLocation() != null && !b.getLocation().isBlank())
                .collect(Collectors.groupingBy(
                        b -> b.getLocation().split("-")[0],
                        Collectors.counting()))
                .forEach((area, count) -> sb.append("• ").append(area).append("：").append(count).append(" 本\n"));
        sb.append("\n以上数据来自本馆实时馆藏，如需某本书的详细信息，请直接告诉我书名。");
        return sb.toString();
    }

    private String buildRecommendAnswer(String q) {
        List<Book> candidates = searchRelevantBooks(q, 20);
        if (candidates.isEmpty()) {
            String category = detectCategory(q);
            if (category != null) {
                candidates = cachedBooks.stream()
                        .filter(b -> category.equals(b.getCategory()))
                        .limit(5)
                        .collect(Collectors.toList());
            }
        }
        if (candidates.isEmpty()) {
            candidates = cachedBooks.stream()
                    .filter(b -> b.getBorrowable() == null || b.getBorrowable())
                    .limit(5)
                    .collect(Collectors.toList());
        }
        if (candidates.isEmpty()) {
            candidates = cachedBooks.stream().limit(5).collect(Collectors.toList());
        }

        int limit = Math.min(5, candidates.size());
        StringBuilder sb = new StringBuilder();
        sb.append("根据您的需求，为您推荐以下 ").append(limit).append(" 本本馆馆藏图书：\n\n");
        for (int i = 0; i < limit; i++) {
            Book book = candidates.get(i);
            int available = availCount(book);
            sb.append(i + 1).append(". 《").append(book.getTitle()).append("》\n");
            sb.append("   作者：").append(nullSafe(book.getAuthor())).append("\n");
            sb.append("   分类：").append(nullSafe(book.getCategory())).append("\n");
            sb.append("   位置：").append(nullSafe(book.getLocation())).append("\n");
            sb.append("   可借：").append(available).append("/").append(book.getTotalCount() != null ? book.getTotalCount() : 0).append("\n");
            sb.append("   推荐理由：").append(buildRecommendReason(book, q)).append("\n\n");
        }
        sb.append("以上图书均在本馆馆藏中，欢迎前往对应区域借阅。");
        return sb.toString();
    }

    private String buildRecommendReason(Book book, String q) {
        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            return truncate(book.getDescription(), 60);
        }
        return "本馆「" + nullSafe(book.getCategory()) + "」类优质藏书，适合当前阅读需求。";
    }

    private String buildLocationAnswer(String q) {
        Book book = findBookByTitleInQuestion(q);
        if (book != null) {
            return String.format(
                    "《%s》（作者：%s）的馆藏位置为：%s。\n分类：%s\n当前可借：%d/%d",
                    book.getTitle(), nullSafe(book.getAuthor()), nullSafe(book.getLocation()),
                    nullSafe(book.getCategory()), availCount(book),
                    book.getTotalCount() != null ? book.getTotalCount() : 0
            );
        }
        String category = detectCategory(q);
        if (category != null) {
            List<Book> catBooks = cachedBooks.stream()
                    .filter(b -> category.equals(b.getCategory()))
                    .limit(3)
                    .collect(Collectors.toList());
            if (!catBooks.isEmpty()) {
                String area = catBooks.get(0).getLocation() != null ? catBooks.get(0).getLocation().split("-")[0] : "未知";
                StringBuilder sb = new StringBuilder();
                sb.append("「").append(category).append("」类图书主要存放在 ").append(area).append("。\n\n示例：\n");
                for (Book b : catBooks) {
                    sb.append("• 《").append(b.getTitle()).append("》→ ").append(nullSafe(b.getLocation())).append("\n");
                }
                return sb.toString();
            }
        }
        return null;
    }

    private String buildBookDetailAnswer(Book book) {
        return String.format(
                "《%s》详细信息：\n\n作者：%s\n出版社：%s\n分类：%s\nISBN：%s\n馆藏位置：%s\n可借数量：%d/%d\n可借阅：%s\n\n简介：%s",
                book.getTitle(), nullSafe(book.getAuthor()), nullSafe(book.getPublisher()),
                nullSafe(book.getCategory()), nullSafe(book.getIsbn()), nullSafe(book.getLocation()),
                availCount(book), book.getTotalCount() != null ? book.getTotalCount() : 0,
                (book.getBorrowable() == null || book.getBorrowable()) ? "是" : "否",
                nullSafe(book.getDescription())
        );
    }

    private Book findBookByTitleInQuestion(String q) {
        Book best = null;
        int bestLen = 0;
        for (Book book : cachedBooks) {
            if (book.getTitle() != null && q.contains(book.getTitle()) && book.getTitle().length() > bestLen) {
                best = book;
                bestLen = book.getTitle().length();
            }
        }
        if (best != null) return best;
        // 尝试书名号
        int start = q.indexOf('《');
        int end = q.indexOf('》');
        if (start >= 0 && end > start) {
            String title = q.substring(start + 1, end);
            for (Book book : cachedBooks) {
                if (title.equals(book.getTitle())) return book;
            }
            return cachedBooks.stream()
                    .filter(b -> b.getTitle() != null && b.getTitle().contains(title))
                    .findFirst().orElse(null);
        }
        return null;
    }

    private String detectCategory(String q) {
        if (q.contains("科幻")) return "科幻小说";
        if (q.contains("推理") || q.contains("悬疑")) return "推理小说";
        if (q.contains("文学") || q.contains("经典")) return "文学经典";
        if (q.contains("历史")) return "历史";
        if (q.contains("编程") || q.contains("代码") || q.contains("Java") || q.contains("Python")) return "编程";
        return null;
    }

    private int availCount(Book book) {
        return (book.getTotalCount() != null ? book.getTotalCount() : 0)
                - (book.getBorrowedCount() != null ? book.getBorrowedCount() : 0);
    }

    public String buildKnowledgeContext(String question) {
        refresh();
        StringBuilder context = new StringBuilder();
        context.append("=== 图书馆知识库 ===\n");
        context.append("馆藏图书总数：").append(cachedBooks.size()).append(" 本\n\n");

        Map<String, Long> categoryCount = cachedBooks.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCategory() != null ? b.getCategory() : "未分类",
                        Collectors.counting()));
        context.append("【分类统计】\n");
        categoryCount.forEach((cat, count) ->
                context.append("- ").append(cat).append("：").append(count).append(" 本\n"));

        Map<String, Long> locationCount = cachedBooks.stream()
                .filter(b -> b.getLocation() != null && !b.getLocation().isBlank())
                .collect(Collectors.groupingBy(
                        b -> b.getLocation().split("-")[0],
                        Collectors.counting()));
        context.append("\n【馆藏区域分布】\n");
        locationCount.forEach((area, count) ->
                context.append("- ").append(area).append("：").append(count).append(" 本\n"));

        List<Book> relevant = searchRelevantBooks(question, 15);
        context.append("\n【与问题相关的图书信息】\n");
        if (relevant.isEmpty()) {
            context.append("未匹配到具体图书，以下为部分馆藏示例：\n");
            relevant = cachedBooks.stream().limit(10).collect(Collectors.toList());
        }
        for (Book book : relevant) {
            context.append(formatBookEntry(book)).append("\n");
        }

        return context.toString();
    }

    public List<Book> searchRelevantBooks(String question, int limit) {
        if (question == null || question.isBlank()) {
            return cachedBooks.stream().limit(limit).collect(Collectors.toList());
        }

        String q = question.toLowerCase();
        List<String> tokens = tokenize(q);

        return cachedBooks.stream()
                .map(book -> new AbstractMap.SimpleEntry<>(book, scoreBook(book, q, tokens)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int scoreBook(Book book, String question, List<String> tokens) {
        int score = 0;
        String combined = String.join(" ",
                nullSafe(book.getTitle()),
                nullSafe(book.getAuthor()),
                nullSafe(book.getCategory()),
                nullSafe(book.getLocation()),
                nullSafe(book.getKeywords()),
                nullSafe(book.getDescription()),
                nullSafe(book.getIsbn())
        ).toLowerCase();

        if (book.getTitle() != null && question.contains(book.getTitle().toLowerCase())) {
            score += 50;
        }
        if (book.getAuthor() != null && question.contains(book.getAuthor().toLowerCase())) {
            score += 30;
        }

        for (String token : tokens) {
            if (combined.contains(token)) {
                score += 5;
            }
        }

        if (question.contains("多少") || question.contains("总数") || question.contains("几本")) {
            score += 1;
        }
        if (question.contains("位置") || question.contains("哪里") || question.contains("馆藏")) {
            if (book.getLocation() != null) score += 3;
        }
        return score;
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String part : text.split("[\\s，。！？、；：（）\\[\\]{}《》]+")) {
            if (part.length() >= 2) {
                tokens.add(part);
            }
        }
        if (text.contains("科幻")) tokens.add("科幻");
        if (text.contains("推理")) tokens.add("推理");
        if (text.contains("文学")) tokens.add("文学");
        if (text.contains("历史")) tokens.add("历史");
        if (text.contains("编程")) tokens.add("编程");
        return tokens;
    }

    private String formatBookEntry(Book book) {
        int available = (book.getTotalCount() != null ? book.getTotalCount() : 0)
                - (book.getBorrowedCount() != null ? book.getBorrowedCount() : 0);
        boolean borrowable = book.getBorrowable() == null || book.getBorrowable();
        return String.format(
                "• 《%s》| 作者：%s | ISBN：%s | 分类：%s | 出版社：%s | 馆藏位置：%s | 可借：%d/%d | 可借阅：%s | 简介：%s",
                nullSafe(book.getTitle()),
                nullSafe(book.getAuthor()),
                nullSafe(book.getIsbn()),
                nullSafe(book.getCategory()),
                nullSafe(book.getPublisher()),
                nullSafe(book.getLocation()),
                available,
                book.getTotalCount() != null ? book.getTotalCount() : 0,
                borrowable ? "是" : "否（管理员已关闭）",
                truncate(nullSafe(book.getDescription()), 120)
        );
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    private String nullSafe(String value) {
        return value != null ? value : "未知";
    }
}
