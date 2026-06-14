package cn.nabr.personalspace.service;

import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.AdminRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private static final List<String> SETTING_KEYS = List.of(
            "site_name",
            "welcome_title",
            "welcome_subtitle",
            "allow_register",
            "allow_posting",
            "allow_comments"
    );

    private final AdminRepository adminRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public Map<String, Object> summary(UserSummary user) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", adminRepository.count("users"));
        stats.put("totalPosts", adminRepository.count("posts"));
        stats.put("totalComments", adminRepository.count("comments"));
        stats.put("totalArticles", adminRepository.count("articles"));
        stats.put("totalAnnouncements", adminRepository.count("announcements"));
        stats.put("newUsersToday", adminRepository.countToday("users", "created_at"));
        stats.put("newPostsToday", adminRepository.countToday("posts", "created_at"));
        stats.put("newCommentsToday", adminRepository.countToday("comments", "created_at"));
        stats.put("visitorCountToday", adminRepository.countToday("visitors", "visited_at"));

        Map<String, String> settings = settings();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stats", stats);
        body.put("settings", settings);
        body.put("featureFlags", Map.of(
                "allowRegister", boolSetting(settings, "allow_register", true),
                "allowPosting", boolSetting(settings, "allow_posting", true),
                "allowComments", boolSetting(settings, "allow_comments", true)
        ));
        body.put("recentVisitors", adminRepository.recentVisitors(8));
        body.put("topPosts", adminRepository.topPosts(6));
        body.put("recentLogs", parseLogs(adminRepository.recentLogs(10)));
        body.put("permissions", Map.of(
                "canManageUsers", "superadmin".equals(user.role()),
                "canManageSettings", "superadmin".equals(user.role())
        ));
        return body;
    }

    public Map<String, Object> posts(String keyword, String author, int page, int limit) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit, 50, 10);
        int total = adminRepository.countPosts(keyword, author);
        return Map.of(
                "posts", adminRepository.findPosts(keyword, author, safePage, safeLimit),
                "pagination", pagination(safePage, safeLimit, total)
        );
    }

    public Map<String, Object> comments(String keyword, String author, int page, int limit) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit, 50, 10);
        int total = adminRepository.countComments(keyword, author);
        return Map.of(
                "comments", adminRepository.findComments(keyword, author, safePage, safeLimit),
                "pagination", pagination(safePage, safeLimit, total)
        );
    }

    public Map<String, Object> articles(String keyword, String author, String category, int page, int limit) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit, 50, 10);
        int total = adminRepository.countArticles(keyword, author, category);
        return Map.of(
                "articles", adminRepository.findArticles(keyword, author, category, safePage, safeLimit),
                "pagination", pagination(safePage, safeLimit, total)
        );
    }

    public Map<String, Object> announcements(String keyword, String author) {
        return Map.of("announcements", adminRepository.findAnnouncements(keyword, author));
    }

    public Map<String, Object> logs(int page, int limit) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit, 100, 20);
        int total = adminRepository.countLogs();
        return Map.of(
                "logs", parseLogs(adminRepository.findLogs(safePage, safeLimit)),
                "pagination", pagination(safePage, safeLimit, total)
        );
    }

    public Map<String, String> settings() {
        return adminRepository.settings();
    }

    public boolean isSettingEnabled(String key, boolean fallback) {
        return boolSetting(settings(), key, fallback);
    }

    @Transactional
    public Map<String, String> updateSettings(UserSummary user, Map<String, Object> incoming) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (String key : SETTING_KEYS) {
            if (!incoming.containsKey(key)) continue;
            Object value = incoming.get(key);
            if (key.startsWith("allow_")) {
                normalized.put(key, truthy(value) ? "1" : "0");
            } else {
                normalized.put(key, value == null ? "" : String.valueOf(value));
            }
        }
        normalized.forEach(adminRepository::upsertSetting);
        adminRepository.logAction(user.id(), "settings.update", "site_settings", "global", toJson(normalized));
        return settings();
    }

    public void ensureDefaultSettings() {
        AdminRepository.AdminDefaults.SETTINGS.forEach(adminRepository::insertDefaultSetting);
    }

    public void logAction(UserSummary user, String action, String targetType, Object targetId, Object detail) {
        adminRepository.logAction(user == null ? null : user.id(), action, targetType, targetId, toJson(detail));
    }

    private List<Map<String, Object>> parseLogs(List<AdminRepository.AdminLog> logs) {
        return logs.stream().map(log -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.id());
            item.put("user_id", log.userId());
            item.put("action", log.action());
            item.put("target_type", log.targetType());
            item.put("target_id", log.targetId());
            item.put("detail", parseDetail(log.detail()));
            item.put("created_at", log.createdAt());
            item.put("operator_name", log.operatorName());
            return item;
        }).toList();
    }

    private Object parseDetail(String detail) {
        if (detail == null || detail.isBlank()) return null;
        try {
            return objectMapper.readValue(detail, Object.class);
        } catch (JsonProcessingException ignored) {
            return detail;
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        if (value instanceof String stringValue) return stringValue;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private boolean boolSetting(Map<String, String> settings, String key, boolean fallback) {
        String value = settings.get(key);
        if (value == null) return fallback;
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private boolean truthy(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue;
        if (value == null) return false;
        String text = String.valueOf(value);
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text);
    }

    private int safePage(int page) {
        return Math.max(1, page);
    }

    private int safeLimit(int limit, int max, int fallback) {
        return Math.min(max, Math.max(1, limit <= 0 ? fallback : limit));
    }

    private Map<String, Object> pagination(int page, int limit, int total) {
        return Map.of(
                "page", page,
                "limit", limit,
                "total", total,
                "pages", (int) Math.ceil(total / (double) limit)
        );
    }
}
