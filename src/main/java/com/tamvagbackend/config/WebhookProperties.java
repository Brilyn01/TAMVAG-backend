package com.tamvagbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tamva.webhook")
public class WebhookProperties {

    private String encryptionKey;

    private int maxAttempts = 8;

    private long initialRetrySeconds = 30;

    private long maxRetrySeconds = 3600;

    private long staleDeliverySeconds = 120;

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getInitialRetrySeconds() {
        return initialRetrySeconds;
    }

    public void setInitialRetrySeconds(long initialRetrySeconds) {
        this.initialRetrySeconds = initialRetrySeconds;
    }

    public long getMaxRetrySeconds() {
        return maxRetrySeconds;
    }

    public void setMaxRetrySeconds(long maxRetrySeconds) {
        this.maxRetrySeconds = maxRetrySeconds;
    }

    public long getStaleDeliverySeconds() {
        return staleDeliverySeconds;
    }

    public void setStaleDeliverySeconds(long staleDeliverySeconds) {
        this.staleDeliverySeconds = staleDeliverySeconds;
    }
}