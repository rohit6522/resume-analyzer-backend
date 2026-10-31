package com.resumeanalyzer.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${spring.datasource.url:#{null}}")
    private String springDatasourceUrl;

    @Value("${DB_USERNAME:#{null}}")
    private String dbUsername;

    @Value("${spring.datasource.username:#{null}}")
    private String springDatasourceUsername;

    @Value("${DB_PASSWORD:#{null}}")
    private String dbPassword;

    @Value("${spring.datasource.password:#{null}}")
    private String springDatasourcePassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        String rawUrl = StringUtils.hasText(databaseUrl) ? databaseUrl.trim() :
                (StringUtils.hasText(springDatasourceUrl) ? springDatasourceUrl.trim() : null);

        String username = StringUtils.hasText(dbUsername) ? dbUsername.trim() :
                (StringUtils.hasText(springDatasourceUsername) ? springDatasourceUsername.trim() : null);

        String password = StringUtils.hasText(dbPassword) ? dbPassword.trim() :
                (StringUtils.hasText(springDatasourcePassword) ? springDatasourcePassword.trim() : null);

        if (StringUtils.hasText(rawUrl)) {
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                try {
                    String uriString = rawUrl.startsWith("postgresql://")
                            ? "postgres://" + rawUrl.substring("postgresql://".length())
                            : rawUrl;

                    URI uri = new URI(uriString);
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/")) {
                        path = path.substring(1);
                    }

                    String userInfo = uri.getUserInfo();
                    if (StringUtils.hasText(userInfo)) {
                        String[] parts = userInfo.split(":", 2);
                        if (parts.length > 0 && !StringUtils.hasText(username)) {
                            username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                        }
                        if (parts.length > 1 && !StringUtils.hasText(password)) {
                            password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                        }
                    }

                    StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                            .append(host)
                            .append(":")
                            .append(port)
                            .append("/")
                            .append(path);

                    String query = uri.getQuery();
                    if (StringUtils.hasText(query)) {
                        jdbcUrl.append("?").append(query);
                    } else if (host != null && host.contains("render.com")) {
                        jdbcUrl.append("?sslmode=require");
                    }

                    String finalJdbcUrl = jdbcUrl.toString();
                    log.info("Constructed PostgreSQL JDBC URL for host: {}:{}", host, port);
                    config.setJdbcUrl(finalJdbcUrl);
                } catch (Exception e) {
                    log.warn("Failed to parse URI from raw URL, applying regex fallback: {}", e.getMessage());
                    String formatted = rawUrl.replaceFirst("^(postgres|postgresql)://", "jdbc:postgresql://");
                    config.setJdbcUrl(formatted);
                }
            } else if (!rawUrl.startsWith("jdbc:")) {
                config.setJdbcUrl("jdbc:postgresql://" + rawUrl);
            } else {
                config.setJdbcUrl(rawUrl);
            }
        } else {
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/resume_db");
        }

        if (StringUtils.hasText(username)) {
            config.setUsername(username);
        }
        if (StringUtils.hasText(password)) {
            config.setPassword(password);
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}

