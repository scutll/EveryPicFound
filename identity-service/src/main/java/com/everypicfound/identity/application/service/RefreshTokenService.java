package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.AccessTokenIssueRequest;
import com.everypicfound.identity.application.command.RefreshTokenCommand;
import com.everypicfound.identity.application.exception.InvalidRefreshTokenException;
import com.everypicfound.identity.application.port.in.RefreshTokenUseCase;
import com.everypicfound.identity.application.port.out.AccessTokenIssuer;
import com.everypicfound.identity.application.port.out.RefreshTokenGenerator;
import com.everypicfound.identity.application.port.out.RefreshTokenHasher;
import com.everypicfound.identity.application.result.IssuedAccessToken;
import com.everypicfound.identity.application.result.RefreshTokenResult;
import com.everypicfound.identity.domain.model.token.RefreshTokenSession;
import com.everypicfound.identity.domain.model.token.UserRefreshToken;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import com.everypicfound.security.contract.SecurityScopes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Refresh Token 换发与轮换用例。
 */
@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofHours(1);

    private static final List<String> DEFAULT_USER_SCOPES = List.of(
            SecurityScopes.IMAGE_READ,
            SecurityScopes.IMAGE_SEARCH,
            SecurityScopes.IMAGE_UPLOAD,
            SecurityScopes.USER_READ,
            SecurityScopes.USER_WRITE);

    private final UserRefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    public RefreshTokenService(
            UserRefreshTokenRepository refreshTokenRepository,
            RefreshTokenHasher refreshTokenHasher,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock) {
        this.refreshTokenRepository = Objects.requireNonNull(
                refreshTokenRepository,
                "refreshTokenRepository");
        this.refreshTokenHasher = Objects.requireNonNull(
                refreshTokenHasher,
                "refreshTokenHasher");
        this.refreshTokenGenerator = Objects.requireNonNull(
                refreshTokenGenerator,
                "refreshTokenGenerator");
        this.accessTokenIssuer = Objects.requireNonNull(
                accessTokenIssuer,
                "accessTokenIssuer");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public RefreshTokenResult refresh(RefreshTokenCommand command) {
        Objects.requireNonNull(command, "command");

        Instant refreshedAt = Instant.ofEpochSecond(
                clock.instant().getEpochSecond());
        String oldTokenHash = refreshTokenHasher.hash(
                command.refreshToken());
        RefreshTokenSession tokenSession = refreshTokenRepository
                .findSessionByTokenHash(oldTokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!tokenSession.canRefreshAt(refreshedAt)) {
            throw new InvalidRefreshTokenException();
        }
        boolean oldTokenMarkedUsed = refreshTokenRepository
                .markActiveTokenAsUsed(oldTokenHash, refreshedAt);
        if (!oldTokenMarkedUsed) {
            throw new InvalidRefreshTokenException();
        }

        String newRefreshToken = refreshTokenGenerator.generate();
        String newRefreshTokenHash = refreshTokenHasher.hash(
                newRefreshToken);
        Instant newRefreshTokenExpiresAt = refreshedAt.plus(
                REFRESH_TOKEN_TTL);
        refreshTokenRepository.save(UserRefreshToken.issue(
                tokenSession.sessionId(),
                newRefreshTokenHash,
                refreshedAt,
                newRefreshTokenExpiresAt));

        IssuedAccessToken issuedAccessToken = accessTokenIssuer.issue(
                new AccessTokenIssueRequest(
                        tokenSession.userId(),
                        tokenSession.sessionId(),
                        DEFAULT_USER_SCOPES,
                        tokenSession.sessionCreatedAt()));

        return new RefreshTokenResult(
                issuedAccessToken.tokenValue(),
                issuedAccessToken.expiresAt(),
                newRefreshToken,
                newRefreshTokenExpiresAt);
    }
}
