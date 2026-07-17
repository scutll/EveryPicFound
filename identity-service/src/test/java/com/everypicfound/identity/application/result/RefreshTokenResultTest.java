package com.everypicfound.identity.application.result;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenResultTest {

    @Test
    void exposesBearerTokenPairWithoutRenderingTokenValues() {
        RefreshTokenResult result = new RefreshTokenResult(
                "new.header.payload.signature",
                Instant.parse("2026-07-17T11:10:00Z"),
                "new-refresh-token",
                Instant.parse("2026-07-17T11:40:00Z"));

        assertThat(result.accessToken())
                .isEqualTo("new.header.payload.signature");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.toString())
                .contains("accessToken=PROTECTED")
                .contains("refreshToken=PROTECTED")
                .doesNotContain("new.header.payload.signature")
                .doesNotContain("new-refresh-token");
    }
}
