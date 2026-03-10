package cn.nabr.personalspace.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSourceConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void sqliteConnectionsEnableForeignKeys() throws Exception {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("org.sqlite.JDBC");
        properties.setUrl("jdbc:sqlite:" + tempDir.resolve("compat-test.db"));

        DataSource dataSource = new DataSourceConfig().dataSource(properties);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }
}
