package com.everypicfound.identity.infrastructure.security.token;

import com.everypicfound.identity.application.port.out.RefreshTokenGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 使用安全随机数生成 URL-safe Refresh Token。
 */
@Component
public final class SecureRandomRefreshTokenGenerator
        implements RefreshTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;

    public SecureRandomRefreshTokenGenerator() {
        this(new SecureRandom());
    }

    SecureRandomRefreshTokenGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
