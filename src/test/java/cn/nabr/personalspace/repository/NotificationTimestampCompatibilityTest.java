package cn.nabr.personalspace.repository;

import cn.nabr.personalspace.config.DataSourceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NotificationTimestampCompatibilityTest {

    private static final String LEGACY_DEFAULT_TIMESTAMP = "2000-01-01 00:00:00";

    @TempDir
    Path tempDir;

    @Test
    void notificationInsertsDoNotRelyOnLegacyTableDefaults() throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplate("notification-compat.db");
        jdbcTemplate.execute("""
                CREATE TABLE notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    from_user_id INTEGER,
                    post_id INTEGER,
                    comment_id INTEGER,
                    content TEXT,
                    is_read INTEGER DEFAULT 0,
                    created_at TEXT DEFAULT '2000-01-01 00:00:00'
                )
                """);

        PostRepository postRepository = new PostRepository(jdbcTemplate);
        postRepository.addLikeNotification(1L, 2L, 3L);
        postRepository.addReplyNotification(1L, 2L, 3L, 4L, "reply");
        postRepository.addCommentNotification(1L, 2L, 3L, 5L, "comment");

        List<String> timestamps = jdbcTemplate.query(
                "SELECT created_at FROM notifications ORDER BY id",
                (rs, rowNum) -> rs.getString("created_at")
        );

        assertEquals(3, timestamps.size());
        for (String timestamp : timestamps) {
            assertNotEquals(LEGACY_DEFAULT_TIMESTAMP, timestamp);
        }
    }

    private JdbcTemplate jdbcTemplate(String dbName) throws Exception {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("org.sqlite.JDBC");
        properties.setUrl("jdbc:sqlite:" + tempDir.resolve(dbName));

        DataSource dataSource = new DataSourceConfig().dataSource(properties);
        return new JdbcTemplate(dataSource);
    }
}
