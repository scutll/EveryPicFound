package com.everypicfound.identity.application.result;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoginUserResultTest {

    @Test
    void exposesBearerTokenContractWithoutRenderingTokenValue() {
        Instant expiresAt = Instant.parse("2026-07-17T10:30:00Z");
        Instant refreshTokenExpiresAt =
                Instant.parse("2026-07-17T11:00:00Z");
        LoginUserResult result = new LoginUserResult(
                "header.payload.signature",
                expiresAt,
                "refresh-token-raw",
                refreshTokenExpiresAt);

        assertThat(result.accessToken())
                .isEqualTo("header.payload.signature");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
        assertThat(result.refreshToken()).isEqualTo("refresh-token-raw");
        assertThat(result.refreshTokenExpiresAt())
                .isEqualTo(refreshTokenExpiresAt);
        assertThat(result.toString())
                .contains("accessToken=PROTECTED")
                .contains("refreshToken=PROTECTED")
                .contains("tokenType=Bearer")
                .doesNotContain("header.payload.signature")
                .doesNotContain("refresh-token-raw");
    }
}
