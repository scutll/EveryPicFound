package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.ChangeMyPasswordCommand;
import com.everypicfound.identity.application.command.DeleteMyAccountCommand;
import com.everypicfound.identity.application.exception.InvalidCredentialsException;
import com.everypicfound.identity.application.exception.UserProfileNotFoundException;
import com.everypicfound.identity.application.port.in.ChangeMyPasswordUseCase;
import com.everypicfound.identity.application.port.in.DeleteMyAccountUseCase;
import com.everypicfound.identity.application.port.out.PasswordHasher;
import com.everypicfound.identity.domain.enums.AccountStatus;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.PresentedPassword;
import com.everypicfound.identity.domain.model.user.RawPassword;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import com.everypicfound.identity.domain.repository.UserRepository;
import com.everypicfound.identity.domain.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 当前用户账户安全操作用例编排。
 */
@Service
public class AccountSecurityService
        implements ChangeMyPasswordUseCase, DeleteMyAccountUseCase {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserRefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public AccountSecurityService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            UserRefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository");
        this.userSessionRepository = Objects.requireNonNull(
                userSessionRepository,
                "userSessionRepository");
        this.refreshTokenRepository = Objects.requireNonNull(
                refreshTokenRepository,
                "refreshTokenRepository");
        this.passwordHasher = Objects.requireNonNull(
                passwordHasher,
                "passwordHasher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public void changeMyPassword(ChangeMyPasswordCommand command) {
        Objects.requireNonNull(command, "command");

        UserAuthentication authentication = activeAuthentication(
                command.userId());
        PresentedPassword currentPassword = PresentedPassword.of(
                command.currentPassword());
        if (!passwordHasher.matches(
                currentPassword,
                authentication.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        PasswordHash newPasswordHash = passwordHasher.hash(
                RawPassword.of(command.newPassword()));
        Instant changedAt = operationTime();
        boolean updated = userRepository.changePassword(
                command.userId(),
                newPasswordHash.value(),
                changedAt);
        if (!updated) {
            throw new UserProfileNotFoundException();
        }
        revokeUserCredentials(command.userId(), changedAt);
    }

    @Override
    @Transactional
    public void deleteMyAccount(DeleteMyAccountCommand command) {
        Objects.requireNonNull(command, "command");

        UserAuthentication authentication = activeAuthentication(
                command.userId());
        PresentedPassword password = PresentedPassword.of(
                command.password());
        if (!passwordHasher.matches(password, authentication.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        Instant deletedAt = operationTime();
        boolean deleted = userRepository.deleteAccount(
                command.userId(),
                deletedAt);
        if (!deleted) {
            throw new UserProfileNotFoundException();
        }
        revokeUserCredentials(command.userId(), deletedAt);
    }

    private UserAuthentication activeAuthentication(long userId) {
        UserAuthentication authentication = userRepository
                .findAuthenticationById(userId)
                .orElseThrow(UserProfileNotFoundException::new);
        if (authentication.status() != AccountStatus.NORMAL) {
            throw new UserProfileNotFoundException();
        }
        return authentication;
    }

    private void revokeUserCredentials(long userId, Instant revokedAt) {
        refreshTokenRepository.revokeActiveTokensByUserId(userId, revokedAt);
        userSessionRepository.revokeActiveSessionsByUserId(userId, revokedAt);
    }

    private Instant operationTime() {
        return Instant.ofEpochSecond(clock.instant().getEpochSecond());
    }

}
