package com.tamvagbackend.service;

import com.tamvagbackend.config.WebhookProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class WebhookSecretService {

    private static final String AES = "AES";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final WebhookProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public WebhookSecretService(WebhookProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );

            byte[] ciphertext = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );

            ByteBuffer combined = ByteBuffer.allocate(
                    iv.length + ciphertext.length
            );

            combined.put(iv);
            combined.put(ciphertext);

            return Base64.getEncoder()
                    .encodeToString(combined.array());

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to encrypt webhook signing secret",
                    ex
            );
        }
    }

    public String decrypt(String encrypted) {
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            if (combined.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException(
                        "Invalid encrypted webhook secret"
                );
            }

            ByteBuffer buffer = ByteBuffer.wrap(combined);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(
                    plaintext,
                    StandardCharsets.UTF_8
            );

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to decrypt webhook signing secret",
                    ex
            );
        }
    }

    private SecretKeySpec getKey() {

        String configuredKey = properties.getEncryptionKey();

        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException(
                    "TAMVA webhook encryption key is not configured"
            );
        }

        /*
         * Derive exactly 32 bytes for AES-256.
         *
         * SHA-256 is used only as deterministic key material derivation;
         * the actual encryption uses AES-256-GCM.
         */
        try {
            byte[] key = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            configuredKey.getBytes(StandardCharsets.UTF_8)
                    );

            return new SecretKeySpec(key, AES);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to derive webhook encryption key",
                    ex
            );
        }
    }
}