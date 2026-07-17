package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.AccessTokenIssueRequest;
import com.everypicfound.identity.application.command.LoginUserCommand;
import com.everypicfound.identity.application.exception.InvalidCredentialsException;
import com.everypicfound.identity.application.port.out.AccessTokenIssuer;
import com.everypicfound.identity.application.port.out.PasswordHasher;
import com.everypicfound.identity.application.port.out.SessionIdGenerator;
import com.everypicfound.identity.application.result.IssuedAccessToken;
import com.everypicfound.identity.application.result.LoginUserResult;
import com.everypicfound.identity.domain.enums.AccountStatus;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.PresentedPassword;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.repository.UserRepository;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private SessionIdGenerator sessionIdGenerator;

    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    private LoginUserService service;

    @BeforeEach
    void setUp() {
        service = new LoginUserService(
                userRepository,
                passwordHasher,
                sessionIdGenerator,
                accessTokenIssuer,
                Clock.fixed(AUTHENTICATED_AT, ZoneOffset.UTC));
    }

    @Test
    void authenticatesAccountAndIssuesBearerAccessToken() {
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

        ArgumentCaptor<AccessTokenIssueRequest> requestCaptor =
                ArgumentCaptor.forClass(AccessTokenIssueRequest.class);
        InOrder order = inOrder(
                userRepository,
                passwordHasher,
                sessionIdGenerator,
                accessTokenIssuer);
        order.verify(userRepository)
                .findAuthenticationByUsername(any(Username.class));
        order.verify(passwordHasher)
                .matches(
                        any(PresentedPassword.class),
                        any(PasswordHash.class));
        order.verify(sessionIdGenerator).generate();
        order.verify(accessTokenIssuer).issue(requestCaptor.capture());

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

        verifyNoInteractions(sessionIdGenerator, accessTokenIssuer);
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

        verifyNoInteractions(sessionIdGenerator, accessTokenIssuer);
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

        verifyNoInteractions(sessionIdGenerator, accessTokenIssuer);
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
