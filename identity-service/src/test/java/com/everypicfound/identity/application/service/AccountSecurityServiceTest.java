package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.ChangeMyPasswordCommand;
import com.everypicfound.identity.application.command.DeleteMyAccountCommand;
import com.everypicfound.identity.application.exception.InvalidCredentialsException;
import com.everypicfound.identity.application.port.out.PasswordHasher;
import com.everypicfound.identity.domain.enums.AccountStatus;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.PresentedPassword;
import com.everypicfound.identity.domain.model.user.RawPassword;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import com.everypicfound.identity.domain.repository.UserRepository;
import com.everypicfound.identity.domain.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private UserRefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private AccountSecurityService service;

    @BeforeEach
    void setUp() {
        service = new AccountSecurityService(
                userRepository,
                userSessionRepository,
                refreshTokenRepository,
                passwordHasher,
                Clock.fixed(
                        Instant.parse("2026-07-18T06:00:00Z"),
                        ZoneOffset.UTC));
    }

    @Test
    void changesPasswordAndRevokesAllUserCredentials() {
        when(userRepository.findAuthenticationById(42L))
                .thenReturn(Optional.of(authentication()));
        when(passwordHasher.matches(
                any(PresentedPassword.class),
                any(PasswordHash.class)))
                .thenReturn(true);
        when(passwordHasher.hash(any(RawPassword.class)))
                .thenReturn(PasswordHash.of("{bcrypt}new-hash"));
        when(userRepository.changePassword(
                42L,
                "{bcrypt}new-hash",
                Instant.parse("2026-07-18T06:00:00Z")))
                .thenReturn(true);

        service.changeMyPassword(new ChangeMyPasswordCommand(
                42L,
                "old123",
                "new123"));

        verify(passwordHasher).matches(
                argThat(password -> password.value().equals("old123")),
                argThat(hash -> hash.value().equals("{bcrypt}old-hash")));
        verify(passwordHasher).hash(
                argThat(password -> password.value().equals("new123")));
        verify(refreshTokenRepository).revokeActiveTokensByUserId(
                42L,
                Instant.parse("2026-07-18T06:00:00Z"));
        verify(userSessionRepository).revokeActiveSessionsByUserId(
                42L,
                Instant.parse("2026-07-18T06:00:00Z"));
    }

    @Test
    void rejectsPasswordChangeWhenCurrentPasswordDoesNotMatch() {
        when(userRepository.findAuthenticationById(42L))
                .thenReturn(Optional.of(authentication()));
        when(passwordHasher.matches(
                any(PresentedPassword.class),
                any(PasswordHash.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.changeMyPassword(
                new ChangeMyPasswordCommand(42L, "wrong123", "new123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).changePassword(
                any(Long.class),
                any(String.class),
                any(Instant.class));
        verify(userSessionRepository, never()).revokeActiveSessionsByUserId(
                any(Long.class),
                any(Instant.class));
    }

    @Test
    void deletesAccountAndRevokesAllUserCredentials() {
        when(userRepository.findAuthenticationById(42L))
                .thenReturn(Optional.of(authentication()));
        when(passwordHasher.matches(
                any(PresentedPassword.class),
                any(PasswordHash.class)))
                .thenReturn(true);
        when(userRepository.deleteAccount(
                42L,
                Instant.parse("2026-07-18T06:00:00Z")))
                .thenReturn(true);

        service.deleteMyAccount(new DeleteMyAccountCommand(
                42L,
                "secret123"));

        verify(userRepository).deleteAccount(
                42L,
                Instant.parse("2026-07-18T06:00:00Z"));
        verify(refreshTokenRepository).revokeActiveTokensByUserId(
                42L,
                Instant.parse("2026-07-18T06:00:00Z"));
        verify(userSessionRepository).revokeActiveSessionsByUserId(
                42L,
                Instant.parse("2026-07-18T06:00:00Z"));
    }

    private static UserAuthentication authentication() {
        return new UserAuthentication(
                42L,
                PasswordHash.of("{bcrypt}old-hash"),
                AccountStatus.NORMAL,
                Instant.parse("2026-07-18T01:00:00Z"));
    }
}
