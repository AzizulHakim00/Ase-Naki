package com.azizul.asenaki.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(@Value("${DATABASE_URL}") String databaseUrl) {
        URI uri = URI.create(databaseUrl.replaceFirst("^jdbc:", ""));
        String[] credentials = uri.getRawUserInfo().split(":", 2);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(toJdbcUrl(uri));
        dataSource.setUsername(decode(credentials[0]));
        dataSource.setPassword(decode(credentials[1]));
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    static String toJdbcUrl(URI uri) {
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        String query = uri.getRawQuery();
        if (query != null) {
            query = "?" + query.replace("channel_binding=", "channelBinding=");
        } else {
            query = "";
        }
        return "jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath() + query;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
