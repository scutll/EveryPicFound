package com.everypicfound.identity.infrastructure.security.token;

import com.everypicfound.identity.application.port.out.RefreshTokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 将 Refresh Token 原文转换成 SHA-256 摘要后保存。
 */
@Component
public final class Sha256RefreshTokenHasher implements RefreshTokenHasher {

    @Override
    public String hash(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("tokenValue must not be blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    tokenValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception);
        }
    }
}
