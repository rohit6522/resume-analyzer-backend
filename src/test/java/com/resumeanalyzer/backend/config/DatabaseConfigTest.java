package com.resumeanalyzer.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void testStandardPostgresUrlConversion() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "databaseUrl", "postgres://testuser:secretpass@dpg-c12345678-a.oregon-postgres.render.com:5432/resume_db");

        DataSource ds = config.dataSource();
        assertTrue(ds instanceof HikariDataSource);
        HikariDataSource hds = (HikariDataSource) ds;

        assertEquals("jdbc:postgresql://dpg-c12345678-a.oregon-postgres.render.com:5432/resume_db?sslmode=require", hds.getJdbcUrl());
        assertEquals("testuser", hds.getUsername());
        assertEquals("secretpass", hds.getPassword());
        hds.close();
    }

    @Test
    void testPostgresqlSchemeWithoutPort() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "databaseUrl", "postgresql://admin:mypass@localhost/custom_db");

        DataSource ds = config.dataSource();
        HikariDataSource hds = (HikariDataSource) ds;

        assertEquals("jdbc:postgresql://localhost:5432/custom_db", hds.getJdbcUrl());
        assertEquals("admin", hds.getUsername());
        assertEquals("mypass", hds.getPassword());
        hds.close();
    }

    @Test
    void testStandardJdbcUrlPassedDirectly() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "springDatasourceUrl", "jdbc:postgresql://localhost:5432/local_db");
        ReflectionTestUtils.setField(config, "springDatasourceUsername", "localuser");
        ReflectionTestUtils.setField(config, "springDatasourcePassword", "localpass");

        DataSource ds = config.dataSource();
        HikariDataSource hds = (HikariDataSource) ds;

        assertEquals("jdbc:postgresql://localhost:5432/local_db", hds.getJdbcUrl());
        assertEquals("localuser", hds.getUsername());
        assertEquals("localpass", hds.getPassword());
        hds.close();
    }
}

