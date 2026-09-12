package com.tamvagbackend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            try {
                log.info("Configuring PostgreSQL datasource from Render DATABASE_URL...");
                // Handle postgres:// or postgresql://
                String cleanedUrl = databaseUrl;
                if (cleanedUrl.startsWith("postgres://")) {
                    cleanedUrl = "postgresql://" + cleanedUrl.substring("postgres://".length());
                }

                URI dbUri = new URI(cleanedUrl);
                String userInfo = dbUri.getUserInfo();
                String username = "";
                String password = "";
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                }

                int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
                String dbPath = dbUri.getPath();
                if (dbPath != null && dbPath.startsWith("/")) {
                    dbPath = dbPath.substring(1);
                }

                String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", dbUri.getHost(), port, dbPath);

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("org.postgresql.Driver");
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setIdleTimeout(30000);
                config.setConnectionTimeout(20000);

                return new HikariDataSource(config);
            } catch (Exception e) {
                log.error("Failed to parse DATABASE_URL from environment, falling back to default properties: {}", e.getMessage());
            }
        }

        log.info("Using standard DataSource configuration with URL: {}", properties.getUrl());
        return properties.initializeDataSourceBuilder().build();
    }
}
