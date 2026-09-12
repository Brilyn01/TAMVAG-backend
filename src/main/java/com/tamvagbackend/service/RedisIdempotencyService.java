package com.tamvagbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    // Local fallback memory store if Redis is unavailable
    private final Map<String, String> localMemoryCache = new ConcurrentHashMap<>();

    public RedisIdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquireIdempotencyLock(String idempotencyKey, long expireSeconds) {
        String key = "idempotency:" + idempotencyKey;
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", Duration.ofSeconds(expireSeconds));
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("Redis unavailable, using local memory lock for key {}: {}", idempotencyKey, e.getMessage());
            if (localMemoryCache.containsKey(key)) {
                return false;
            }
            localMemoryCache.put(key, "LOCKED");
            return true;
        }
    }

    public void cacheValue(String cacheKey, String jsonValue, long ttlSeconds) {
        String key = "cache:" + cacheKey;
        try {
            redisTemplate.opsForValue().set(key, jsonValue, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Redis unavailable, caching in local memory for key {}: {}", cacheKey, e.getMessage());
            localMemoryCache.put(key, jsonValue);
        }
    }

    public Optional<String> getCachedValue(String cacheKey) {
        String key = "cache:" + cacheKey;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return Optional.of(value.toString());
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, checking local memory cache for key {}: {}", cacheKey, e.getMessage());
            return Optional.ofNullable(localMemoryCache.get(key));
        }
        return Optional.empty();
    }

    public void releaseLock(String idempotencyKey) {
        String key = "idempotency:" + idempotencyKey;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            localMemoryCache.remove(key);
        }
    }
}
