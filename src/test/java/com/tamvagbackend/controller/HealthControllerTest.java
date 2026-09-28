package com.tamvagbackend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class HealthControllerTest {

    @Test
    void checkHealthReturnsUpWhenAllDependenciesAreHealthy() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        HealthController healthController = new HealthController(dataSource, redisTemplate);
        ResponseEntity<Map<String, Object>> response = healthController.checkHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void checkHealthReturnsServiceUnavailableWhenDatabaseFails() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        HealthController healthController = new HealthController(dataSource, redisTemplate);
        ResponseEntity<Map<String, Object>> response = healthController.checkHealth();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("DOWN", response.getBody().get("status"));
    }
}