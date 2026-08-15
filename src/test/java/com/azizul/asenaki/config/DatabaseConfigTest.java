package com.azizul.asenaki.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

class DatabaseConfigTest {

    @Test
    void convertsNeonUrlToPostgreSqlJdbcSettings() {
        String neonUrl = "postgresql://student:secret@db.example.com/classroom?channel_binding=require&sslmode=require";

        HikariDataSource dataSource = (HikariDataSource) new DatabaseConfig().dataSource(neonUrl);

        assertThat(dataSource.getJdbcUrl()).isEqualTo(
                "jdbc:postgresql://db.example.com/classroom?channelBinding=require&sslmode=require");
        assertThat(dataSource.getUsername()).isEqualTo("student");
        assertThat(dataSource.getPassword()).isEqualTo("secret");
        dataSource.close();
    }
}
