package cn.nabr.personalspace.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AdminRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    public int countToday(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE datetime(" + columnName + ") >= datetime('now', 'localtime', 'start of day')",
                Integer.class
        );
        return count == null ? 0 : count;
    }

    public List<RecentVisitor> recentVisitors(int limit) {
        return jdbcTemplate.query("""
                SELECT v.id, v.ip, v.visited_at, u.id AS user_id, u.nickname, u.avatar
                FROM visitors v
                LEFT JOIN users u ON v.user_id = u.id
                WHERE u.role IS NULL OR u.role != 'superadmin'
                ORDER BY v.visited_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new RecentVisitor(
                        rs.getLong("id"),
                        rs.getString("ip"),
                        rs.getString("visited_at"),
                        rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar")
                ),
                limit
        );
    }

    public List<AdminPost> topPosts(int limit) {
        return jdbcTemplate.query("""
                SELECT p.id, p.content, p.views, p.created_at, u.nickname AS author_name,
                  (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comment_count,
                  (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id) AS like_count
                FROM posts p
                LEFT JOIN users u ON p.user_id = u.id
                ORDER BY p.views DESC, p.created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new AdminPost(
                        rs.getLong("id"),
                        rs.getString("content"),
                        rs.getInt("views"),
                        rs.getString("created_at"),
                        rs.getString("author_name"),
                        rs.getInt("comment_count"),
                        rs.getInt("like_count")
                ),
                limit
        );
    }

    public List<AdminLog> recentLogs(int limit) {
        return jdbcTemplate.query("""
                SELECT l.*, u.nickname AS operator_name
                FROM operation_logs l
                LEFT JOIN users u ON l.user_id = u.id
                ORDER BY l.created_at DESC, l.id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> mapLog(rs),
                limit
        );
    }

    public List<AdminPostItem> findPosts(String keyword, String author, int page, int limit) {
        QueryParts query = postQuery(keyword, author);
        return jdbcTemplate.query("""
                SELECT p.*, u.nickname AS author_name, u.avatar AS author_avatar,
                  (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id) AS like_count,
                  (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comment_count
                FROM posts p
                LEFT JOIN users u ON p.user_id = u.id
                %s
                ORDER BY p.created_at DESC
                LIMIT ? OFFSET ?
                """.formatted(query.where()),
                (rs, rowNum) -> new AdminPostItem(
                        rs.getLong("id"),
                        rs.getString("content"),
                        rs.getString("image"),
                        rs.getString("thumbnail"),
                        rs.getString("images"),
                        rs.getString("thumbnails"),
                        rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        rs.getInt("views"),
                        rs.getString("created_at"),
                        rs.getString("author_name"),
                        rs.getString("author_avatar"),
                        rs.getInt("like_count"),
                        rs.getInt("comment_count")
                ),
                withPaging(query.params(), limit, offset(page, limit))
        );
    }

    public int countPosts(String keyword, String author) {
        QueryParts query = postQuery(keyword, author);
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM posts p
                LEFT JOIN users u ON p.user_id = u.id
                %s
                """.formatted(query.where()), Integer.class, query.params());
        return total == null ? 0 : total;
    }

    public List<AdminCommentItem> findComments(String keyword, String author, int page, int limit) {
        QueryParts query = commentQuery(keyword, author);
        return jdbcTemplate.query("""
                SELECT c.*, u.nickname, u.avatar, p.content AS post_content
                FROM comments c
                LEFT JOIN users u ON c.user_id = u.id
                LEFT JOIN posts p ON c.post_id = p.id
                %s
                ORDER BY c.created_at DESC
                LIMIT ? OFFSET ?
                """.formatted(query.where()),
                (rs, rowNum) -> new AdminCommentItem(
                        rs.getLong("id"),
                        rs.getLong("post_id"),
                        rs.getLong("user_id"),
                        rs.getString("content"),
                        rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                        rs.getObject("reply_to_user_id") == null ? null : rs.getLong("reply_to_user_id"),
                        rs.getString("created_at"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("post_content")
                ),
                withPaging(query.params(), limit, offset(page, limit))
        );
    }

    public int countComments(String keyword, String author) {
        QueryParts query = commentQuery(keyword, author);
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM comments c
                LEFT JOIN users u ON c.user_id = u.id
                %s
                """.formatted(query.where()), Integer.class, query.params());
        return total == null ? 0 : total;
    }

    public List<AdminArticleItem> findArticles(String keyword, String author, String category, int page, int limit) {
        QueryParts query = articleQuery(keyword, author, category);
        return jdbcTemplate.query("""
                SELECT a.*, u.nickname AS author_name, u.avatar AS author_avatar
                FROM articles a
                LEFT JOIN users u ON a.user_id = u.id
                %s
                ORDER BY a.updated_at DESC, a.created_at DESC
                LIMIT ? OFFSET ?
                """.formatted(query.where()),
                (rs, rowNum) -> new AdminArticleItem(
                        rs.getLong("id"),
                        rs.getString("category"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("summary"),
                        rs.getString("cover_image"),
                        rs.getLong("user_id"),
                        rs.getInt("views"),
                        rs.getString("created_at"),
                        rs.getString("updated_at"),
                        rs.getString("author_name"),
                        rs.getString("author_avatar")
                ),
                withPaging(query.params(), limit, offset(page, limit))
        );
    }

    public int countArticles(String keyword, String author, String category) {
        QueryParts query = articleQuery(keyword, author, category);
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM articles a
                LEFT JOIN users u ON a.user_id = u.id
                %s
                """.formatted(query.where()), Integer.class, query.params());
        return total == null ? 0 : total;
    }

    public List<AdminAnnouncementItem> findAnnouncements(String keyword, String author) {
        QueryParts query = announcementQuery(keyword, author);
        return jdbcTemplate.query("""
                SELECT a.*, u.nickname AS author_name, u.avatar AS author_avatar
                FROM announcements a
                LEFT JOIN users u ON a.user_id = u.id
                %s
                ORDER BY a.pinned DESC, a.created_at DESC
                LIMIT 50
                """.formatted(query.where()),
                (rs, rowNum) -> new AdminAnnouncementItem(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getInt("pinned"),
                        rs.getString("created_at"),
                        rs.getString("author_name"),
                        rs.getString("author_avatar")
                ),
                query.params()
        );
    }

    public List<AdminLog> findLogs(int page, int limit) {
        return jdbcTemplate.query("""
                SELECT l.*, u.nickname AS operator_name
                FROM operation_logs l
                LEFT JOIN users u ON l.user_id = u.id
                ORDER BY l.created_at DESC, l.id DESC
                LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> mapLog(rs),
                limit,
                offset(page, limit)
        );
    }

    public int countLogs() {
        return count("operation_logs");
    }

    public Map<String, String> settings() {
        List<Map.Entry<String, String>> rows = jdbcTemplate.query(
                "SELECT key, value FROM site_settings",
                (rs, rowNum) -> Map.entry(rs.getString("key"), rs.getString("value"))
        );
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>(AdminDefaults.SETTINGS);
        for (Map.Entry<String, String> row : rows) {
            result.put(row.getKey(), row.getValue());
        }
        return result;
    }

    public void upsertSetting(String key, String value) {
        jdbcTemplate.update("""
                INSERT INTO site_settings (key, value, updated_at)
                VALUES (?, ?, datetime('now', 'localtime'))
                ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at
                """, key, value);
    }

    public void insertDefaultSetting(String key, String value) {
        jdbcTemplate.update("""
                INSERT INTO site_settings (key, value, updated_at)
                VALUES (?, ?, datetime('now', 'localtime'))
                ON CONFLICT(key) DO NOTHING
                """, key, value);
    }

    public void logAction(Long userId, String action, String targetType, Object targetId, String detail) {
        jdbcTemplate.update(
                "INSERT INTO operation_logs (user_id, action, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?)",
                userId,
                action,
                targetType,
                targetId == null ? null : String.valueOf(targetId),
                detail
        );
    }

    private AdminLog mapLog(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AdminLog(
                rs.getLong("id"),
                rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                rs.getString("action"),
                rs.getString("target_type"),
                rs.getString("target_id"),
                rs.getString("detail"),
                rs.getString("created_at"),
                rs.getString("operator_name")
        );
    }

    private QueryParts postQuery(String keyword, String author) {
        QueryBuilder builder = new QueryBuilder();
        if (hasText(keyword)) builder.add("COALESCE(p.content, '') LIKE ?", like(keyword));
        if (hasText(author)) builder.add("COALESCE(u.nickname, '') LIKE ?", like(author));
        return builder.build();
    }

    private QueryParts commentQuery(String keyword, String author) {
        QueryBuilder builder = new QueryBuilder();
        if (hasText(keyword)) builder.add("c.content LIKE ?", like(keyword));
        if (hasText(author)) builder.add("COALESCE(u.nickname, '') LIKE ?", like(author));
        return builder.build();
    }

    private QueryParts articleQuery(String keyword, String author, String category) {
        QueryBuilder builder = new QueryBuilder();
        if (hasText(keyword)) builder.add("(a.title LIKE ? OR COALESCE(a.summary, '') LIKE ? OR a.content LIKE ?)", like(keyword), like(keyword), like(keyword));
        if ("blog".equals(category) || "chitchat".equals(category)) builder.add("a.category = ?", category);
        if (hasText(author)) builder.add("COALESCE(u.nickname, '') LIKE ?", like(author));
        return builder.build();
    }

    private QueryParts announcementQuery(String keyword, String author) {
        QueryBuilder builder = new QueryBuilder();
        if (hasText(keyword)) builder.add("(a.title LIKE ? OR a.content LIKE ?)", like(keyword), like(keyword));
        if (hasText(author)) builder.add("COALESCE(u.nickname, '') LIKE ?", like(author));
        return builder.build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String like(String value) {
        return "%" + value.trim() + "%";
    }

    private int offset(int page, int limit) {
        return (page - 1) * limit;
    }

    private Object[] withPaging(Object[] params, int limit, int offset) {
        Object[] all = java.util.Arrays.copyOf(params, params.length + 2);
        all[params.length] = limit;
        all[params.length + 1] = offset;
        return all;
    }

    public static class AdminDefaults {
        public static final Map<String, String> SETTINGS = Map.of(
                "site_name", "我的空间",
                "welcome_title", "我的空间",
                "welcome_subtitle", "记录想法，慢慢生活",
                "allow_register", "1",
                "allow_posting", "1",
                "allow_comments", "1"
        );
    }

    private record QueryParts(String where, Object[] params) {}

    private static class QueryBuilder {
        private final List<String> parts = new java.util.ArrayList<>();
        private final List<Object> params = new java.util.ArrayList<>();

        void add(String wherePart, Object... values) {
            parts.add(wherePart);
            params.addAll(java.util.Arrays.asList(values));
        }

        QueryParts build() {
            String where = parts.isEmpty() ? "" : "WHERE " + String.join(" AND ", parts);
            return new QueryParts(where, params.toArray());
        }
    }

    public record RecentVisitor(long id, String ip, @JsonProperty("visited_at") String visitedAt,
                                @JsonProperty("user_id") Long userId, String nickname, String avatar) {}
    public record AdminPost(long id, String content, int views, @JsonProperty("created_at") String createdAt,
                            @JsonProperty("author_name") String authorName,
                            @JsonProperty("comment_count") int commentCount,
                            @JsonProperty("like_count") int likeCount) {}
    public record AdminPostItem(long id, String content, String image, String thumbnail, String images, String thumbnails,
                                @JsonProperty("user_id") Long userId, int views,
                                @JsonProperty("created_at") String createdAt,
                                @JsonProperty("author_name") String authorName,
                                @JsonProperty("author_avatar") String authorAvatar,
                                @JsonProperty("like_count") int likeCount,
                                @JsonProperty("comment_count") int commentCount) {}
    public record AdminCommentItem(long id, @JsonProperty("post_id") long postId, @JsonProperty("user_id") long userId,
                                   String content, @JsonProperty("parent_id") Long parentId,
                                   @JsonProperty("reply_to_user_id") Long replyToUserId,
                                   @JsonProperty("created_at") String createdAt, String nickname, String avatar,
                                   @JsonProperty("post_content") String postContent) {}
    public record AdminArticleItem(long id, String category, String title, String content, String summary,
                                   @JsonProperty("cover_image") String coverImage,
                                   @JsonProperty("user_id") long userId, int views,
                                   @JsonProperty("created_at") String createdAt,
                                   @JsonProperty("updated_at") String updatedAt,
                                   @JsonProperty("author_name") String authorName,
                                   @JsonProperty("author_avatar") String authorAvatar) {}
    public record AdminAnnouncementItem(long id, @JsonProperty("user_id") long userId, String title, String content,
                                        int pinned, @JsonProperty("created_at") String createdAt,
                                        @JsonProperty("author_name") String authorName,
                                        @JsonProperty("author_avatar") String authorAvatar) {}
    public record AdminLog(long id, @JsonProperty("user_id") Long userId, String action,
                           @JsonProperty("target_type") String targetType,
                           @JsonProperty("target_id") String targetId,
                           String detail, @JsonProperty("created_at") String createdAt,
                           @JsonProperty("operator_name") String operatorName) {}
}
