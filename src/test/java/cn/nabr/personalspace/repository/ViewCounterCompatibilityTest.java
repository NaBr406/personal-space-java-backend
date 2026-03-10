package cn.nabr.personalspace.repository;

import cn.nabr.personalspace.config.DataSourceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewCounterCompatibilityTest {

    @TempDir
    Path tempDir;

    @Test
    void missingPostViewCounterMatchesFrontendCompatibilityFallback() throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplate("post-views.db");
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, nickname, avatar, role) VALUES (1, 'author', 'hash', 'Author', '/default-avatar.png', 'admin')"
        );
        jdbcTemplate.update(
                "INSERT INTO posts (id, content, user_id, views, created_at) VALUES (1, 'hello', 1, 0, datetime('now', 'localtime'))"
        );

        PostRepository postRepository = new PostRepository(jdbcTemplate);

        assertEquals(0, postRepository.incrementViews(999L));
        assertEquals(1, postRepository.incrementViews(1L));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT views FROM posts WHERE id = 1", Integer.class));
    }

    @Test
    void missingArticleViewCounterMatchesFrontendCompatibilityFallback() throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplate("article-views.db");
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, nickname, avatar, role) VALUES (1, 'author', 'hash', 'Author', '/default-avatar.png', 'superadmin')"
        );
        jdbcTemplate.update(
                "INSERT INTO articles (id, category, title, content, user_id, views, created_at, updated_at) VALUES (1, 'blog', 'Title', 'Body', 1, 0, datetime('now', 'localtime'), datetime('now', 'localtime'))"
        );

        ArticleRepository articleRepository = new ArticleRepository(jdbcTemplate);

        assertEquals(0, articleRepository.incrementViews(999L));
        assertEquals(1, articleRepository.incrementViews(1L));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT views FROM articles WHERE id = 1", Integer.class));
    }

    private JdbcTemplate jdbcTemplate(String dbName) throws Exception {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("org.sqlite.JDBC");
        properties.setUrl("jdbc:sqlite:" + tempDir.resolve(dbName));

        DataSource dataSource = new DataSourceConfig().dataSource(properties);
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        return new JdbcTemplate(dataSource);
    }
}
