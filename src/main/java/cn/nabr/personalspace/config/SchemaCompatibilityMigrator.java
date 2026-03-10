package cn.nabr.personalspace.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaCompatibilityMigrator {
    private final JdbcTemplate jdbcTemplate;

    public SchemaCompatibilityMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        ensureColumn("posts", "images", "ALTER TABLE posts ADD COLUMN images TEXT");
        ensureColumn("posts", "thumbnails", "ALTER TABLE posts ADD COLUMN thumbnails TEXT");
        ensureColumn("posts", "user_id", "ALTER TABLE posts ADD COLUMN user_id INTEGER");
        ensureColumn("posts", "thumbnail", "ALTER TABLE posts ADD COLUMN thumbnail TEXT");
        ensureColumn("users", "register_ip", "ALTER TABLE users ADD COLUMN register_ip TEXT");
        ensureColumn("comments", "parent_id", "ALTER TABLE comments ADD COLUMN parent_id INTEGER");
        ensureColumn("comments", "reply_to_user_id", "ALTER TABLE comments ADD COLUMN reply_to_user_id INTEGER");
    }

    void ensureColumn(String tableName, String columnName, String alterSql) {
        if (!tableExists(tableName) || columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute(alterSql);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        return jdbcTemplate.query(
                "PRAGMA table_info(" + tableName + ")",
                (rs, rowNum) -> rs.getString("name")
        ).stream().anyMatch(columnName::equals);
    }
}
