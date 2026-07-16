package com.everypicfound.identity.application.result;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuedAccessTokenTest {

    private static final Instant EXPIRES_AT =
            Instant.parse("2026-07-16T12:30:00Z");

    @Test
    void exposesTokenToCallerButProtectsDiagnosticText() {
        IssuedAccessToken token = new IssuedAccessToken(
                "header.payload.signature",
                EXPIRES_AT);

        assertThat(token.tokenValue())
                .isEqualTo("header.payload.signature");
        assertThat(token.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(token.toString())
                .contains("tokenValue=PROTECTED")
                .contains(EXPIRES_AT.toString())
                .doesNotContain("header.payload.signature");
    }

    @Test
    void rejectsBlankTokenValue() {
        assertThatThrownBy(() -> new IssuedAccessToken(" ", EXPIRES_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenValue");
    }

    @Test
    void rejectsMissingExpiration() {
        assertThatThrownBy(() -> new IssuedAccessToken(
                "header.payload.signature",
                null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("expiresAt");
    }
}
