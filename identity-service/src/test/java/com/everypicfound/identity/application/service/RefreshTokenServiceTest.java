package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.AccessTokenIssueRequest;
import com.everypicfound.identity.application.command.RefreshTokenCommand;
import com.everypicfound.identity.application.exception.InvalidRefreshTokenException;
import com.everypicfound.identity.application.port.out.AccessTokenIssuer;
import com.everypicfound.identity.application.port.out.RefreshTokenGenerator;
import com.everypicfound.identity.application.port.out.RefreshTokenHasher;
import com.everypicfound.identity.application.result.IssuedAccessToken;
import com.everypicfound.identity.application.result.RefreshTokenResult;
import com.everypicfound.identity.domain.model.session.UserSessionStatus;
import com.everypicfound.identity.domain.model.token.RefreshTokenSession;
import com.everypicfound.identity.domain.model.token.UserRefreshToken;
import com.everypicfound.identity.domain.model.token.UserRefreshTokenStatus;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant REFRESHED_AT =
            Instant.parse("2026-07-17T10:40:00Z");
    private static final Instant SESSION_CREATED_AT =
            Instant.parse("2026-07-17T10:00:00Z");
    private static final Instant SESSION_EXPIRES_AT =
            Instant.parse("2026-07-18T10:00:00Z");
    private static final Instant OLD_REFRESH_EXPIRES_AT =
            Instant.parse("2026-07-17T11:00:00Z");
    private static final Instant NEW_REFRESH_EXPIRES_AT =
            Instant.parse("2026-07-17T11:40:00Z");
    private static final Instant ACCESS_EXPIRES_AT =
            Instant.parse("2026-07-17T11:10:00Z");

    @Mock
    private UserRefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(
                refreshTokenRepository,
                refreshTokenHasher,
                refreshTokenGenerator,
                accessTokenIssuer,
                Clock.fixed(REFRESHED_AT, ZoneOffset.UTC));
    }

    @Test
    void rotatesActiveRefreshTokenAndIssuesNewTokenPair() {
        when(refreshTokenHasher.hash("old-refresh-token"))
                .thenReturn("old-refresh-hash");
        when(refreshTokenRepository.findSessionByTokenHash(
                "old-refresh-hash"))
                .thenReturn(Optional.of(activeTokenSession()));
        when(refreshTokenRepository.markActiveTokenAsUsed(
                "old-refresh-hash",
                REFRESHED_AT))
                .thenReturn(true);
        when(refreshTokenGenerator.generate())
                .thenReturn("new-refresh-token");
        when(refreshTokenHasher.hash("new-refresh-token"))
                .thenReturn("new-refresh-hash");
        when(accessTokenIssuer.issue(any(AccessTokenIssueRequest.class)))
                .thenReturn(new IssuedAccessToken(
                        "new.header.payload.signature",
                        ACCESS_EXPIRES_AT));

        RefreshTokenResult result = service.refresh(
                new RefreshTokenCommand("old-refresh-token"));

        assertThat(result.accessToken())
                .isEqualTo("new.header.payload.signature");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresAt()).isEqualTo(ACCESS_EXPIRES_AT);
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.refreshTokenExpiresAt())
                .isEqualTo(NEW_REFRESH_EXPIRES_AT);

        ArgumentCaptor<UserRefreshToken> newRefreshTokenCaptor =
                ArgumentCaptor.forClass(UserRefreshToken.class);
        ArgumentCaptor<AccessTokenIssueRequest> accessRequestCaptor =
                ArgumentCaptor.forClass(AccessTokenIssueRequest.class);
        InOrder order = inOrder(
                refreshTokenHasher,
                refreshTokenRepository,
                refreshTokenGenerator,
                accessTokenIssuer);
        order.verify(refreshTokenHasher).hash("old-refresh-token");
        order.verify(refreshTokenRepository)
                .findSessionByTokenHash("old-refresh-hash");
        order.verify(refreshTokenRepository)
                .markActiveTokenAsUsed("old-refresh-hash", REFRESHED_AT);
        order.verify(refreshTokenGenerator).generate();
        order.verify(refreshTokenHasher).hash("new-refresh-token");
        order.verify(refreshTokenRepository)
                .save(newRefreshTokenCaptor.capture());
        order.verify(accessTokenIssuer).issue(accessRequestCaptor.capture());

        UserRefreshToken newRefreshToken = newRefreshTokenCaptor.getValue();
        assertThat(newRefreshToken.sessionId()).isEqualTo("session-123");
        assertThat(newRefreshToken.tokenHash()).isEqualTo("new-refresh-hash");
        assertThat(newRefreshToken.issuedAt()).isEqualTo(REFRESHED_AT);
        assertThat(newRefreshToken.expiresAt())
                .isEqualTo(NEW_REFRESH_EXPIRES_AT);

        AccessTokenIssueRequest accessRequest = accessRequestCaptor.getValue();
        assertThat(accessRequest.userId()).isEqualTo(42L);
        assertThat(accessRequest.sessionId()).isEqualTo("session-123");
        assertThat(accessRequest.authTime()).isEqualTo(SESSION_CREATED_AT);
        assertThat(accessRequest.scopes()).containsExactly(
                "image:read",
                "image:search",
                "image:upload",
                "user:read",
                "user:write");
    }

    @Test
    void rejectsMissingRefreshTokenRecord() {
        when(refreshTokenHasher.hash("missing-refresh-token"))
                .thenReturn("missing-refresh-hash");
        when(refreshTokenRepository.findSessionByTokenHash(
                "missing-refresh-hash"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(
                new RefreshTokenCommand("missing-refresh-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenGenerator, accessTokenIssuer);
    }

    @Test
    void rejectsExpiredRefreshTokenBeforeRotation() {
        when(refreshTokenHasher.hash("old-refresh-token"))
                .thenReturn("old-refresh-hash");
        when(refreshTokenRepository.findSessionByTokenHash(
                "old-refresh-hash"))
                .thenReturn(Optional.of(new RefreshTokenSession(
                        "old-refresh-hash",
                        UserRefreshTokenStatus.ACTIVE,
                        Instant.parse("2026-07-17T10:39:59Z"),
                        "session-123",
                        42L,
                        UserSessionStatus.ACTIVE,
                        SESSION_CREATED_AT,
                        SESSION_EXPIRES_AT)));

        assertThatThrownBy(() -> service.refresh(
                new RefreshTokenCommand("old-refresh-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenGenerator, accessTokenIssuer);
    }

    @Test
    void rejectsConcurrentReuseWhenActiveUpdateAffectsNoRows() {
        when(refreshTokenHasher.hash("old-refresh-token"))
                .thenReturn("old-refresh-hash");
        when(refreshTokenRepository.findSessionByTokenHash(
                "old-refresh-hash"))
                .thenReturn(Optional.of(activeTokenSession()));
        when(refreshTokenRepository.markActiveTokenAsUsed(
                "old-refresh-hash",
                REFRESHED_AT))
                .thenReturn(false);

        assertThatThrownBy(() -> service.refresh(
                new RefreshTokenCommand("old-refresh-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenGenerator, accessTokenIssuer);
    }

    private static RefreshTokenSession activeTokenSession() {
        return new RefreshTokenSession(
                "old-refresh-hash",
                UserRefreshTokenStatus.ACTIVE,
                OLD_REFRESH_EXPIRES_AT,
                "session-123",
                42L,
                UserSessionStatus.ACTIVE,
                SESSION_CREATED_AT,
                SESSION_EXPIRES_AT);
    }
}
