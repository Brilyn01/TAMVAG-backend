package com.tamvagbackend.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class WebhookSignatureService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public String sign(String payload, String secret) {

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            SecretKeySpec key = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );

            mac.init(key);

            byte[] signature = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder()
                    .encodeToString(signature);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to sign webhook payload",
                    ex
            );
        }
    }

    public boolean verify(
            String payload,
            String secret,
            String expectedSignature
    ) {

        if (expectedSignature == null || expectedSignature.isBlank()) {
            return false;
        }

        String actualSignature = sign(payload, secret);

        return MessageDigest.isEqual(
                actualSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}