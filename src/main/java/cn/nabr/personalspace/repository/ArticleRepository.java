package cn.nabr.personalspace.repository;

import cn.nabr.personalspace.model.ArticleView;
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
public class ArticleRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ArticleView> mapper = (rs, rowNum) -> new ArticleView(
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
    );

    public ArticleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ArticleView> findPage(String category, int page, int limit) {
        int offset = (page - 1) * limit;
        return jdbcTemplate.query("""
                SELECT a.*, u.nickname AS author_name, u.avatar AS author_avatar
                FROM articles a
                LEFT JOIN users u ON a.user_id = u.id
                WHERE a.category = ?
                ORDER BY a.created_at DESC
                LIMIT ? OFFSET ?
                """, mapper, category, limit, offset);
    }

    public int countByCategory(String category) {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM articles WHERE category = ?", Integer.class, category);
        return total == null ? 0 : total;
    }

    public Optional<ArticleView> findById(long id) {
        List<ArticleView> articles = jdbcTemplate.query("""
                SELECT a.*, u.nickname AS author_name, u.avatar AS author_avatar
                FROM articles a
                LEFT JOIN users u ON a.user_id = u.id
                WHERE a.id = ?
                """, mapper, id);
        return articles.stream().findFirst();
    }

    public long create(String category, String title, String content, String summary, String coverImage, long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO articles (category, title, content, summary, cover_image, user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now', 'localtime'), datetime('now', 'localtime'))",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, category);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setString(4, summary);
            ps.setString(5, coverImage);
            ps.setLong(6, userId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        Long id = jdbcTemplate.queryForObject("SELECT id FROM articles ORDER BY id DESC LIMIT 1", Long.class);
        return id == null ? 0L : id;
    }

    public void update(long id, String title, String content, String summary, String coverImage) {
        jdbcTemplate.update(
                "UPDATE articles SET title = ?, content = ?, summary = ?, cover_image = ?, updated_at = datetime('now', 'localtime') WHERE id = ?",
                title,
                content,
                summary,
                coverImage,
                id
        );
    }

    public void delete(long id) {
        jdbcTemplate.update("DELETE FROM articles WHERE id = ?", id);
    }

    public int incrementViews(long id) {
        jdbcTemplate.update("UPDATE articles SET views = views + 1 WHERE id = ?", id);
        List<Integer> views = jdbcTemplate.query(
                "SELECT views FROM articles WHERE id = ?",
                (rs, rowNum) -> rs.getInt("views"),
                id
        );
        return views.stream().findFirst().orElse(0);
    }
}
