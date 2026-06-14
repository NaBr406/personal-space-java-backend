package cn.nabr.personalspace.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 数据源配置。
 * 对 SQLite 额外开启外键和 WAL，兼顾数据约束与并发读写体验。
 */
@Configuration
public class DataSourceConfig {

    /**
     * 如果是 SQLite，就顺手创建数据库目录并套上 SQLite 的专用配置。
     */
    @Bean
    public DataSource dataSource(DataSourceProperties properties) throws Exception {
        String url = properties.getUrl();
        if (url != null && url.startsWith("jdbc:sqlite:")) {
            String pathText = url.substring("jdbc:sqlite:".length());
            Path dbPath = Path.of(pathText).normalize();
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            SQLiteConfig config = new SQLiteConfig();
            // foreign keys 和 WAL 都是兼容旧库时比较关键的默认项。
            config.enforceForeignKeys(true);
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);

            SQLiteDataSource dataSource = new SQLiteDataSource(config);
            dataSource.setUrl(url);
            return dataSource;
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setUrl(url);
        return dataSource;
    }
}
