package cn.nabr.personalspace.repository;

import cn.nabr.personalspace.model.CommentView;
import cn.nabr.personalspace.model.PostView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PostRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PostView> postMapper = (rs, rowNum) -> new PostView(
            rs.getLong("id"),
            rs.getString("content"),
            rs.getString("image"),
            rs.getString("thumbnail"),
            rs.getString("images"),
            rs.getString("thumbnails"),
            rs.getLong("user_id"),
            rs.getInt("views"),
            rs.getString("created_at"),
            rs.getString("author_name"),
            rs.getString("author_avatar"),
            rs.getInt("like_count"),
            rs.getInt("comment_count"),
            rs.getObject("liked") == null ? null : rs.getInt("liked") == 1
    );

    private final RowMapper<CommentView> commentMapper = (rs, rowNum) -> new CommentView(
            rs.getLong("id"),
            rs.getLong("post_id"),
            rs.getLong("user_id"),
            rs.getString("content"),
            rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
            rs.getObject("reply_to_user_id") == null ? null : rs.getLong("reply_to_user_id"),
            rs.getString("created_at"),
            rs.getString("nickname"),
            rs.getString("avatar"),
            rs.getString("reply_to_nickname")
    );

    public PostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PostView> findPosts(Integer currentUserId, int page, int limit, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.*, u.nickname AS author_name, u.avatar AS author_avatar,
                       (SELECT COUNT(*) FROM likes WHERE post_id = p.id) AS like_count,
                       (SELECT COUNT(*) FROM comments WHERE post_id = p.id) AS comment_count
                """);
        List<Object> params = new ArrayList<>();
        if (currentUserId != null) {
            sql.append(", (SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM likes WHERE post_id = p.id AND user_id = ?) AS liked ");
            params.add(currentUserId);
        } else {
            sql.append(", NULL AS liked ");
        }
        sql.append("FROM posts p LEFT JOIN users u ON p.user_id = u.id ");
        appendDateWhere(sql, params, startDate, endDate);
        sql.append(" ORDER BY p.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add((page - 1) * limit);
        return jdbcTemplate.query(sql.toString(), postMapper, params.toArray());
    }

    public int countPosts(String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM posts p ");
        List<Object> params = new ArrayList<>();
        appendDateWhere(sql, params, startDate, endDate);
        Integer total = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return total == null ? 0 : total;
    }

    public Optional<PostView> findPostById(long id, Integer currentUserId) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.*, u.nickname AS author_name, u.avatar AS author_avatar,
                       (SELECT COUNT(*) FROM likes WHERE post_id = p.id) AS like_count,
                       (SELECT COUNT(*) FROM comments WHERE post_id = p.id) AS comment_count
                """);
        List<Object> params = new ArrayList<>();
        if (currentUserId != null) {
            sql.append(", (SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM likes WHERE post_id = p.id AND user_id = ?) AS liked ");
            params.add(currentUserId);
        } else {
            sql.append(", NULL AS liked ");
        }
        sql.append("FROM posts p LEFT JOIN users u ON p.user_id = u.id WHERE p.id = ?");
        params.add(id);
        List<PostView> posts = jdbcTemplate.query(sql.toString(), postMapper, params.toArray());
        return posts.stream().findFirst();
    }

    public int incrementViews(long id) {
        jdbcTemplate.update("UPDATE posts SET views = views + 1 WHERE id = ?", id);
        Integer views = jdbcTemplate.queryForObject("SELECT views FROM posts WHERE id = ?", Integer.class, id);
        return views == null ? 0 : views;
    }

    public long createPost(String content, String image, String thumbnail, String imagesJson, String thumbnailsJson, long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO posts (content, image, thumbnail, images, thumbnails, user_id, created_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now', 'localtime'))",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, content);
            ps.setString(2, image);
            ps.setString(3, thumbnail);
            ps.setString(4, imagesJson);
            ps.setString(5, thumbnailsJson);
            ps.setLong(6, userId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        Long id = jdbcTemplate.queryForObject("SELECT id FROM posts ORDER BY id DESC LIMIT 1", Long.class);
        return id == null ? 0L : id;
    }

    public Optional<PostOwner> findPostOwner(long postId) {
        List<PostOwner> posts = jdbcTemplate.query(
                "SELECT id, user_id FROM posts WHERE id = ?",
                (rs, rowNum) -> new PostOwner(rs.getLong("id"), rs.getLong("user_id")),
                postId
        );
        return posts.stream().findFirst();
    }

    public void deletePostAndRelations(long postId) {
        jdbcTemplate.update("DELETE FROM notifications WHERE post_id = ?", postId);
        jdbcTemplate.update("DELETE FROM likes WHERE post_id = ?", postId);
        jdbcTemplate.update("DELETE FROM comments WHERE post_id = ?", postId);
        jdbcTemplate.update("DELETE FROM posts WHERE id = ?", postId);
    }

    public boolean hasLike(long postId, long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE post_id = ? AND user_id = ?",
                Integer.class,
                postId,
                userId
        );
        return count != null && count > 0;
    }

    public void insertLike(long postId, long userId) {
        jdbcTemplate.update("INSERT OR IGNORE INTO likes (post_id, user_id) VALUES (?, ?)", postId, userId);
    }

    public void deleteLike(long postId, long userId) {
        jdbcTemplate.update("DELETE FROM likes WHERE post_id = ? AND user_id = ?", postId, userId);
    }

    public int countLikes(long postId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM likes WHERE post_id = ?", Integer.class, postId);
        return count == null ? 0 : count;
    }

    public void addLikeNotification(long userId, long fromUserId, long postId) {
        jdbcTemplate.update(
                "INSERT INTO notifications (user_id, type, from_user_id, post_id) VALUES (?, 'like', ?, ?)",
                userId,
                fromUserId,
                postId
        );
    }

    public List<CommentView> findComments(long postId) {
        return jdbcTemplate.query("""
                SELECT c.*, u.nickname, u.avatar,
                       ru.nickname AS reply_to_nickname
                FROM comments c
                LEFT JOIN users u ON c.user_id = u.id
                LEFT JOIN users ru ON c.reply_to_user_id = ru.id
                WHERE c.post_id = ?
                ORDER BY c.created_at ASC
                """, commentMapper, postId);
    }

    public Optional<CommentRecord> findCommentRecord(long commentId) {
        List<CommentRecord> comments = jdbcTemplate.query(
                "SELECT id, post_id, user_id, parent_id, reply_to_user_id FROM comments WHERE id = ?",
                (rs, rowNum) -> new CommentRecord(
                        rs.getLong("id"),
                        rs.getLong("post_id"),
                        rs.getLong("user_id"),
                        rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                        rs.getObject("reply_to_user_id") == null ? null : rs.getLong("reply_to_user_id")
                ),
                commentId
        );
        return comments.stream().findFirst();
    }

    public Optional<CommentRecord> findCommentRecordInPost(long commentId, long postId) {
        List<CommentRecord> comments = jdbcTemplate.query(
                "SELECT id, post_id, user_id, parent_id, reply_to_user_id FROM comments WHERE id = ? AND post_id = ?",
                (rs, rowNum) -> new CommentRecord(
                        rs.getLong("id"),
                        rs.getLong("post_id"),
                        rs.getLong("user_id"),
                        rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                        rs.getObject("reply_to_user_id") == null ? null : rs.getLong("reply_to_user_id")
                ),
                commentId,
                postId
        );
        return comments.stream().findFirst();
    }

    public long createComment(long postId, long userId, String content, Long parentId, Long replyToUserId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_user_id, created_at) VALUES (?, ?, ?, ?, ?, datetime('now', 'localtime'))",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            ps.setString(3, content);
            if (parentId == null) {
                ps.setObject(4, null);
            } else {
                ps.setLong(4, parentId);
            }
            if (replyToUserId == null) {
                ps.setObject(5, null);
            } else {
                ps.setLong(5, replyToUserId);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        Long id = jdbcTemplate.queryForObject("SELECT id FROM comments ORDER BY id DESC LIMIT 1", Long.class);
        return id == null ? 0L : id;
    }

    public Optional<CommentView> findCommentViewById(long commentId) {
        List<CommentView> comments = jdbcTemplate.query("""
                SELECT c.*, u.nickname, u.avatar,
                       ru.nickname AS reply_to_nickname
                FROM comments c
                LEFT JOIN users u ON c.user_id = u.id
                LEFT JOIN users ru ON c.reply_to_user_id = ru.id
                WHERE c.id = ?
                """, commentMapper, commentId);
        return comments.stream().findFirst();
    }

    public void addReplyNotification(long userId, long fromUserId, long postId, long commentId, String content) {
        jdbcTemplate.update(
                "INSERT INTO notifications (user_id, type, from_user_id, post_id, comment_id, content) VALUES (?, 'reply', ?, ?, ?, ?)",
                userId,
                fromUserId,
                postId,
                commentId,
                content
        );
    }

    public void addCommentNotification(long userId, long fromUserId, long postId, long commentId, String content) {
        jdbcTemplate.update(
                "INSERT INTO notifications (user_id, type, from_user_id, post_id, comment_id, content) VALUES (?, 'comment', ?, ?, ?, ?)",
                userId,
                fromUserId,
                postId,
                commentId,
                content
        );
    }

    public List<Long> findChildCommentIds(long parentId) {
        return jdbcTemplate.query(
                "SELECT id FROM comments WHERE parent_id = ?",
                (rs, rowNum) -> rs.getLong("id"),
                parentId
        );
    }

    public void deleteNotificationsByCommentId(long commentId) {
        jdbcTemplate.update("DELETE FROM notifications WHERE comment_id = ?", commentId);
    }

    public void deleteCommentsByParentId(long parentId) {
        jdbcTemplate.update("DELETE FROM comments WHERE parent_id = ?", parentId);
    }

    public void deleteCommentById(long commentId) {
        jdbcTemplate.update("DELETE FROM comments WHERE id = ?", commentId);
    }

    private void appendDateWhere(StringBuilder sql, List<Object> params, String startDate, String endDate) {
        if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()) {
            sql.append("WHERE p.created_at >= ? AND p.created_at < datetime(?, '+1 day') ");
            params.add(startDate);
            params.add(endDate);
        } else if (startDate != null && !startDate.isBlank()) {
            sql.append("WHERE p.created_at >= ? ");
            params.add(startDate);
        } else if (endDate != null && !endDate.isBlank()) {
            sql.append("WHERE p.created_at < datetime(?, '+1 day') ");
            params.add(endDate);
        }
    }

    public record PostOwner(long id, long userId) {}

    public record CommentRecord(long id, long postId, long userId, Long parentId, Long replyToUserId) {}
}
