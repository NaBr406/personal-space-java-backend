package cn.nabr.personalspace.repository;

import cn.nabr.personalspace.model.NotificationView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<NotificationView> mapper = (rs, rowNum) -> new NotificationView(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("type"),
            rs.getObject("from_user_id") == null ? null : rs.getLong("from_user_id"),
            rs.getObject("post_id") == null ? null : rs.getLong("post_id"),
            rs.getObject("comment_id") == null ? null : rs.getLong("comment_id"),
            rs.getString("content"),
            rs.getInt("is_read"),
            rs.getString("created_at"),
            rs.getString("from_nickname"),
            rs.getString("from_avatar"),
            rs.getString("post_content")
    );

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<NotificationView> findByUserId(long userId) {
        return jdbcTemplate.query("""
                SELECT n.*, u.nickname AS from_nickname, u.avatar AS from_avatar,
                       p.content AS post_content
                FROM notifications n
                LEFT JOIN users u ON n.from_user_id = u.id
                LEFT JOIN posts p ON n.post_id = p.id
                WHERE n.user_id = ?
                ORDER BY n.created_at DESC
                LIMIT 50
                """, mapper, userId);
    }

    public int countUnread(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public void markAllRead(long userId) {
        jdbcTemplate.update("UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0", userId);
    }

    public void markOneRead(long id, long userId) {
        jdbcTemplate.update("UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?", id, userId);
    }
}
