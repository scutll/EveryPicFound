package com.everypicfound.identity.application.result;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoginUserResultTest {

    @Test
    void exposesBearerTokenContractWithoutRenderingTokenValue() {
        Instant expiresAt = Instant.parse("2026-07-17T10:30:00Z");
        LoginUserResult result = new LoginUserResult(
                "header.payload.signature",
                expiresAt);

        assertThat(result.accessToken())
                .isEqualTo("header.payload.signature");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
        assertThat(result.toString())
                .contains("accessToken=PROTECTED")
                .contains("tokenType=Bearer")
                .doesNotContain("header.payload.signature");
    }
}
