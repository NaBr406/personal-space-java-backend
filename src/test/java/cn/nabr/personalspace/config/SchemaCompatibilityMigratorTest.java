package cn.nabr.personalspace.config;

import cn.nabr.personalspace.repository.AuthRepository;
import cn.nabr.personalspace.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaCompatibilityMigratorTest {

    @TempDir
    Path tempDir;

    @Test
    void migrateAddsLegacyCommentReplyColumnsForAppJsFlows() throws Exception {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("org.sqlite.JDBC");
        properties.setUrl("jdbc:sqlite:" + tempDir.resolve("legacy-comments.db"));

        DataSource dataSource = new DataSourceConfig().dataSource(properties);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    nickname TEXT NOT NULL,
                    avatar TEXT,
                    role TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE posts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    content TEXT,
                    image TEXT,
                    thumbnail TEXT,
                    images TEXT,
                    thumbnails TEXT,
                    user_id INTEGER,
                    views INTEGER DEFAULT 0,
                    created_at TEXT DEFAULT (datetime('now', 'localtime'))
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE comments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    post_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    created_at TEXT DEFAULT (datetime('now', 'localtime'))
                )
                """);

        jdbcTemplate.update(
                "INSERT INTO users (id, username, nickname, avatar, role) VALUES (1, 'tester', 'Tester', '/default-avatar.png', 'guest')"
        );
        jdbcTemplate.update(
                "INSERT INTO posts (id, content, user_id, created_at) VALUES (1, 'hello', 1, datetime('now', 'localtime'))"
        );
        jdbcTemplate.update(
                "INSERT INTO comments (id, post_id, user_id, content, created_at) VALUES (1, 1, 1, 'legacy comment', datetime('now', 'localtime'))"
        );

        SchemaCompatibilityMigrator migrator = new SchemaCompatibilityMigrator(jdbcTemplate);
        migrator.migrate();

        List<String> columns = jdbcTemplate.query(
                "PRAGMA table_info(comments)",
                (rs, rowNum) -> rs.getString("name")
        );
        assertTrue(columns.contains("parent_id"));
        assertTrue(columns.contains("reply_to_user_id"));

        PostRepository postRepository = new PostRepository(jdbcTemplate);
        assertDoesNotThrow(() -> postRepository.findComments(1));
        assertEquals(1, postRepository.findComments(1).size());
        assertEquals("legacy comment", postRepository.findComments(1).get(0).content());
    }

    @Test
    void migrateAddsLegacyPostAndRegisterColumnsForAppJsFlows() throws Exception {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("org.sqlite.JDBC");
        properties.setUrl("jdbc:sqlite:" + tempDir.resolve("legacy-app.db"));

        DataSource dataSource = new DataSourceConfig().dataSource(properties);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    password_hash TEXT NOT NULL,
                    nickname TEXT NOT NULL,
                    avatar TEXT,
                    role TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE posts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    content TEXT,
                    image TEXT,
                    views INTEGER DEFAULT 0,
                    created_at TEXT DEFAULT (datetime('now', 'localtime'))
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE likes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    post_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE comments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    post_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    created_at TEXT DEFAULT (datetime('now', 'localtime'))
                )
                """);

        jdbcTemplate.update("""
                INSERT INTO users (id, username, password_hash, nickname, avatar, role)
                VALUES (1, 'legacy-user', 'hash', 'Legacy User', '/default-avatar.png', 'guest')
                """);
        jdbcTemplate.update("""
                INSERT INTO posts (id, content, image, views, created_at)
                VALUES (1, 'legacy post', null, 0, datetime('now', 'localtime'))
                """);

        SchemaCompatibilityMigrator migrator = new SchemaCompatibilityMigrator(jdbcTemplate);
        migrator.migrate();

        assertTrue(tableColumns(jdbcTemplate, "users").contains("register_ip"));
        assertTrue(tableColumns(jdbcTemplate, "posts").contains("thumbnail"));
        assertTrue(tableColumns(jdbcTemplate, "posts").contains("images"));
        assertTrue(tableColumns(jdbcTemplate, "posts").contains("thumbnails"));
        assertTrue(tableColumns(jdbcTemplate, "posts").contains("user_id"));

        PostRepository postRepository = new PostRepository(jdbcTemplate);
        AuthRepository authRepository = new AuthRepository(jdbcTemplate);

        assertDoesNotThrow(() -> postRepository.findPosts(null, 1, 10, null, null));
        assertEquals(1, postRepository.findPosts(null, 1, 10, null, null).size());
        assertDoesNotThrow(() -> postRepository.createPost("new post", null, null, null, null, 1L));
        assertDoesNotThrow(() -> authRepository.registerIpExists("127.0.0.1"));
        assertDoesNotThrow(() -> authRepository.createUser(
                "new-user",
                "hash",
                "New User",
                "/default-avatar.png",
                "127.0.0.1"
        ));
    }

    private List<String> tableColumns(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.query(
                "PRAGMA table_info(" + tableName + ")",
                (rs, rowNum) -> rs.getString("name")
        );
    }
}
