package cn.nabr.personalspace.repository;

import cn.nabr.personalspace.model.AnnouncementView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class AnnouncementRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AnnouncementView> mapper = (rs, rowNum) -> new AnnouncementView(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getInt("pinned"),
            rs.getString("created_at"),
            rs.getString("author_name"),
            rs.getString("author_avatar")
    );

    public AnnouncementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AnnouncementView> findPage(int page, int limit) {
        int offset = (page - 1) * limit;
        return jdbcTemplate.query("""
                SELECT a.*, u.nickname AS author_name, u.avatar AS author_avatar
                FROM announcements a
                LEFT JOIN users u ON a.user_id = u.id
                ORDER BY a.pinned DESC, a.created_at DESC
                LIMIT ? OFFSET ?
                """, mapper, limit, offset);
    }

    public int countAll() {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM announcements", Integer.class);
        return total == null ? 0 : total;
    }

    public Optional<AnnouncementView> findById(long id) {
        List<AnnouncementView> list = jdbcTemplate.query("""
                SELECT a.*, u.nickname AS author_name, u.avatar AS author_avatar
                FROM announcements a
                LEFT JOIN users u ON a.user_id = u.id
                WHERE a.id = ?
                """, mapper, id);
        return list.stream().findFirst();
    }

    public long create(long userId, String title, String content, boolean pinned) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO announcements (user_id, title, content, pinned, created_at) VALUES (?, ?, ?, ?, datetime('now', 'localtime'))",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setInt(4, pinned ? 1 : 0);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        Long id = jdbcTemplate.queryForObject("SELECT id FROM announcements ORDER BY id DESC LIMIT 1", Long.class);
        return id == null ? 0L : id;
    }

    public void delete(long id) {
        jdbcTemplate.update("DELETE FROM announcements WHERE id = ?", id);
    }

    public void updatePinned(long id, boolean pinned) {
        jdbcTemplate.update("UPDATE announcements SET pinned = ? WHERE id = ?", pinned ? 1 : 0, id);
    }
}
