package com.mercury.poker.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * MySQL 连接池（HikariCP）。环境变量：JDBC_URL / JDBC_USER / JDBC_PASSWORD。
 */
public final class JdbcDataSourceFactory {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/poker_aa?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "poker";
    private static final String DEFAULT_PASSWORD = "poker123";

    private static volatile HikariDataSource dataSource;

    private JdbcDataSourceFactory() {
    }

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(envOrDefault("JDBC_URL", DEFAULT_URL));
            config.setUsername(envOrDefault("JDBC_USER", DEFAULT_USER));
            config.setPassword(envOrDefault("JDBC_PASSWORD", DEFAULT_PASSWORD));
            config.setMaximumPoolSize(5);
            config.setPoolName("poker-aa-mysql");
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
