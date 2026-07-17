package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.AccessTokenIssueRequest;
import com.everypicfound.identity.application.command.LoginUserCommand;
import com.everypicfound.identity.application.exception.InvalidCredentialsException;
import com.everypicfound.identity.application.port.out.AccessTokenIssuer;
import com.everypicfound.identity.application.port.out.PasswordHasher;
import com.everypicfound.identity.application.port.out.RefreshTokenGenerator;
import com.everypicfound.identity.application.port.out.RefreshTokenHasher;
import com.everypicfound.identity.application.port.out.SessionIdGenerator;
import com.everypicfound.identity.application.result.IssuedAccessToken;
import com.everypicfound.identity.application.result.LoginUserResult;
import com.everypicfound.identity.domain.enums.AccountStatus;
import com.everypicfound.identity.domain.model.session.UserSession;
import com.everypicfound.identity.domain.model.token.UserRefreshToken;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.PresentedPassword;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import com.everypicfound.identity.domain.repository.UserRepository;
import com.everypicfound.identity.domain.repository.UserSessionRepository;
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
class LoginUserServiceTest {

    private static final Instant AUTHENTICATED_AT =
            Instant.parse("2026-07-17T10:00:00Z");
    private static final Instant EXPIRES_AT =
            Instant.parse("2026-07-17T10:30:00Z");
    private static final Instant REFRESH_TOKEN_EXPIRES_AT =
            Instant.parse("2026-07-17T11:00:00Z");
    private static final Instant SESSION_EXPIRES_AT =
            Instant.parse("2026-07-18T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private UserRefreshTokenRepository userRefreshTokenRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private SessionIdGenerator sessionIdGenerator;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    private LoginUserService service;

    @BeforeEach
    void setUp() {
        service = new LoginUserService(
                userRepository,
                userSessionRepository,
                userRefreshTokenRepository,
                passwordHasher,
                sessionIdGenerator,
                refreshTokenGenerator,
                refreshTokenHasher,
                accessTokenIssuer,
                Clock.fixed(AUTHENTICATED_AT, ZoneOffset.UTC));
    }

    @Test
    void authenticatesAccountCreatesSessionAndIssuesTokens() {
        PasswordHash passwordHash = PasswordHash.of(
                "{bcrypt}encoded-password");
        when(userRepository.findAuthenticationByUsername(
                any(Username.class)))
                .thenReturn(Optional.of(authentication(
                        AccountStatus.NORMAL,
                        passwordHash)));
        when(passwordHasher.matches(
                any(PresentedPassword.class),
                any(PasswordHash.class)))
                .thenReturn(true);
        when(sessionIdGenerator.generate()).thenReturn("session-123");
        when(refreshTokenGenerator.generate())
                .thenReturn("refresh-token-raw");
        when(refreshTokenHasher.hash("refresh-token-raw"))
                .thenReturn("refresh-token-hash");
        when(accessTokenIssuer.issue(any(AccessTokenIssueRequest.class)))
                .thenReturn(new IssuedAccessToken(
                        "header.payload.signature",
                        EXPIRES_AT));

        LoginUserResult result = service.login(new LoginUserCommand(
                "  User01  ",
                "secret123"));

        assertThat(result.accessToken())
                .isEqualTo("header.payload.signature");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(result.refreshToken()).isEqualTo("refresh-token-raw");
        assertThat(result.refreshTokenExpiresAt())
                .isEqualTo(REFRESH_TOKEN_EXPIRES_AT);

        ArgumentCaptor<AccessTokenIssueRequest> requestCaptor =
                ArgumentCaptor.forClass(AccessTokenIssueRequest.class);
        ArgumentCaptor<UserSession> sessionCaptor =
                ArgumentCaptor.forClass(UserSession.class);
        ArgumentCaptor<UserRefreshToken> refreshTokenCaptor =
                ArgumentCaptor.forClass(UserRefreshToken.class);
        InOrder order = inOrder(
                userRepository,
                passwordHasher,
                sessionIdGenerator,
                userSessionRepository,
                refreshTokenGenerator,
                refreshTokenHasher,
                userRefreshTokenRepository,
                accessTokenIssuer);
        order.verify(userRepository)
                .findAuthenticationByUsername(any(Username.class));
        order.verify(passwordHasher)
                .matches(
                        any(PresentedPassword.class),
                        any(PasswordHash.class));
        order.verify(sessionIdGenerator).generate();
        order.verify(userSessionRepository).save(sessionCaptor.capture());
        order.verify(refreshTokenGenerator).generate();
        order.verify(refreshTokenHasher).hash("refresh-token-raw");
        order.verify(userRefreshTokenRepository)
                .save(refreshTokenCaptor.capture());
        order.verify(accessTokenIssuer).issue(requestCaptor.capture());

        UserSession session = sessionCaptor.getValue();
        assertThat(session.sessionId()).isEqualTo("session-123");
        assertThat(session.userId()).isEqualTo(42L);
        assertThat(session.createdAt()).isEqualTo(AUTHENTICATED_AT);
        assertThat(session.expiresAt()).isEqualTo(SESSION_EXPIRES_AT);

        UserRefreshToken refreshToken = refreshTokenCaptor.getValue();
        assertThat(refreshToken.sessionId()).isEqualTo("session-123");
        assertThat(refreshToken.tokenHash()).isEqualTo("refresh-token-hash");
        assertThat(refreshToken.issuedAt()).isEqualTo(AUTHENTICATED_AT);
        assertThat(refreshToken.expiresAt())
                .isEqualTo(REFRESH_TOKEN_EXPIRES_AT);

        AccessTokenIssueRequest request = requestCaptor.getValue();
        assertThat(request.userId()).isEqualTo(42L);
        assertThat(request.sessionId()).isEqualTo("session-123");
        assertThat(request.authTime()).isEqualTo(AUTHENTICATED_AT);
        assertThat(request.scopes()).containsExactly(
                "image:read",
                "image:search",
                "image:upload",
                "user:read",
                "user:write");
    }

    @Test
    void rejectsMissingUsernameWithoutCheckingPasswordOrIssuingToken() {
        when(userRepository.findAuthenticationByUsername(
                any(Username.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginUserCommand(
                "Missing01",
                "secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageNotContaining("Missing01")
                .hasMessageNotContaining("secret123");

        verifyNoInteractions(
                passwordHasher,
                sessionIdGenerator,
                userSessionRepository,
                refreshTokenGenerator,
                refreshTokenHasher,
                userRefreshTokenRepository,
                accessTokenIssuer);
    }

    @Test
    void rejectsWrongPasswordBeforeGeneratingSessionId() {
        PasswordHash passwordHash = PasswordHash.of(
                "{bcrypt}encoded-password");
        when(userRepository.findAuthenticationByUsername(
                any(Username.class)))
                .thenReturn(Optional.of(authentication(
                        AccountStatus.NORMAL,
                        passwordHash)));
        when(passwordHasher.matches(
                any(PresentedPassword.class),
                any(PasswordHash.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginUserCommand(
                "User01",
                "wrong123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(
                sessionIdGenerator,
                userSessionRepository,
                refreshTokenGenerator,
                refreshTokenHasher,
                userRefreshTokenRepository,
                accessTokenIssuer);
    }

    @Test
    void verifiesStoredCredentialWithoutApplyingRegistrationLengthPolicy() {
        PasswordHash passwordHash = PasswordHash.of(
                "{bcrypt}encoded-password");
        when(userRepository.findAuthenticationByUsername(
                any(Username.class)))
                .thenReturn(Optional.of(authentication(
                        AccountStatus.NORMAL,
                        passwordHash)));
        when(passwordHasher.matches(any(), any(PasswordHash.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginUserCommand(
                "User01",
                "short")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(
                sessionIdGenerator,
                userSessionRepository,
                refreshTokenGenerator,
                refreshTokenHasher,
                userRefreshTokenRepository,
                accessTokenIssuer);
    }

    @Test
    void rejectsNonNormalAccountWithSamePublicFailure() {
        PasswordHash passwordHash = PasswordHash.of(
                "{bcrypt}encoded-password");
        when(userRepository.findAuthenticationByUsername(
                any(Username.class)))
                .thenReturn(Optional.of(authentication(
                        AccountStatus.DISABLED,
                        passwordHash)));
        when(passwordHasher.matches(
                any(PresentedPassword.class),
                any(PasswordHash.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginUserCommand(
                "User01",
                "secret123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(
                sessionIdGenerator,
                userSessionRepository,
                refreshTokenGenerator,
                refreshTokenHasher,
                userRefreshTokenRepository,
                accessTokenIssuer);
    }

    private static UserAuthentication authentication(
            AccountStatus status,
            PasswordHash passwordHash) {
        return new UserAuthentication(
                42L,
                passwordHash,
                status,
                Instant.parse("2026-07-16T08:00:00Z"));
    }
}
