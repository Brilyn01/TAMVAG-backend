package com.tamvagbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (redisUrl != null && !redisUrl.isBlank()) {
            try {
                log.info("Configuring Redis Connection Factory from REDIS_URL...");
                URI uri = new URI(redisUrl);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 6379;
                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
                if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
                    config.setPassword(uri.getUserInfo().split(":")[1]);
                }
                return new LettuceConnectionFactory(config);
            } catch (Exception e) {
                log.warn("Failed to parse REDIS_URL, falling back to host/port config: {}", e.getMessage());
            }
        }

        log.info("Configuring Redis Connection Factory with Host: {}, Port: {}", redisHost, redisPort);
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
