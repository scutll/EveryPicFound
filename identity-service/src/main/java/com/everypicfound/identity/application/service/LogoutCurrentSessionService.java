package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.LogoutCurrentSessionCommand;
import com.everypicfound.identity.application.port.in.LogoutCurrentSessionUseCase;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import com.everypicfound.identity.domain.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * MySQL 权威状态下的当前会话退出用例。
 */
@Service
public class LogoutCurrentSessionService implements LogoutCurrentSessionUseCase {

    private final UserSessionRepository userSessionRepository;
    private final UserRefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public LogoutCurrentSessionService(
            UserSessionRepository userSessionRepository,
            UserRefreshTokenRepository refreshTokenRepository,
            Clock clock) {
        this.userSessionRepository = Objects.requireNonNull(
                userSessionRepository,
                "userSessionRepository");
        this.refreshTokenRepository = Objects.requireNonNull(
                refreshTokenRepository,
                "refreshTokenRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public void logout(LogoutCurrentSessionCommand command) {
        Objects.requireNonNull(command, "command");
        Instant revokedAt = Instant.ofEpochSecond(
                clock.instant().getEpochSecond());
        userSessionRepository.revokeActiveSession(
                command.sessionId(),
                command.userId(),
                revokedAt);
        refreshTokenRepository.revokeActiveTokensBySessionId(
                command.sessionId(),
                revokedAt);
    }
}
