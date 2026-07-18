package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.LogoutCurrentSessionCommand;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import com.everypicfound.identity.domain.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutCurrentSessionServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private UserRefreshTokenRepository refreshTokenRepository;

    private LogoutCurrentSessionService service;

    @BeforeEach
    void setUp() {
        service = new LogoutCurrentSessionService(
                userSessionRepository,
                refreshTokenRepository,
                Clock.fixed(
                        Instant.parse("2026-07-18T02:00:00Z"),
                        ZoneOffset.UTC));
    }

    @Test
    void revokesCurrentSessionAndItsActiveRefreshTokens() {
        LogoutCurrentSessionCommand command = new LogoutCurrentSessionCommand(
                42L,
                "session-123");

        assertThatCode(() -> service.logout(command))
                .doesNotThrowAnyException();

        verify(userSessionRepository).revokeActiveSession(
                "session-123",
                42L,
                Instant.parse("2026-07-18T02:00:00Z"));
        verify(refreshTokenRepository).revokeActiveTokensBySessionId(
                "session-123",
                Instant.parse("2026-07-18T02:00:00Z"));
    }
}
