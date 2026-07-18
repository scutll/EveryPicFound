package com.everypicfound.identity.application.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogoutCurrentSessionCommandTest {

    @Test
    void rejectsInvalidUserId() {
        assertThatThrownBy(() -> new LogoutCurrentSessionCommand(
                0L,
                "session-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId must be positive");
    }

    @Test
    void rejectsBlankSessionId() {
        assertThatThrownBy(() -> new LogoutCurrentSessionCommand(
                42L,
                " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sessionId must not be blank");
    }
}
