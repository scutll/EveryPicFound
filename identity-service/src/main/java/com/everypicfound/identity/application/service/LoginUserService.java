package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.AccessTokenIssueRequest;
import com.everypicfound.identity.application.command.LoginUserCommand;
import com.everypicfound.identity.application.exception.InvalidCredentialsException;
import com.everypicfound.identity.application.port.in.LoginUserUseCase;
import com.everypicfound.identity.application.port.out.AccessTokenIssuer;
import com.everypicfound.identity.application.port.out.PasswordHasher;
import com.everypicfound.identity.application.port.out.SessionIdGenerator;
import com.everypicfound.identity.application.result.IssuedAccessToken;
import com.everypicfound.identity.application.result.LoginUserResult;
import com.everypicfound.identity.domain.enums.AccountStatus;
import com.everypicfound.identity.domain.model.user.PresentedPassword;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.repository.UserRepository;
import com.everypicfound.security.contract.SecurityScopes;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Access Token-only 用户名密码登录用例。
 */
@Service
public final class LoginUserService implements LoginUserUseCase {

    private static final List<String> DEFAULT_USER_SCOPES = List.of(
            SecurityScopes.IMAGE_READ,
            SecurityScopes.IMAGE_SEARCH,
            SecurityScopes.IMAGE_UPLOAD,
            SecurityScopes.USER_READ,
            SecurityScopes.USER_WRITE);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SessionIdGenerator sessionIdGenerator;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    public LoginUserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            SessionIdGenerator sessionIdGenerator,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository");
        this.passwordHasher = Objects.requireNonNull(
                passwordHasher,
                "passwordHasher");
        this.sessionIdGenerator = Objects.requireNonNull(
                sessionIdGenerator,
                "sessionIdGenerator");
        this.accessTokenIssuer = Objects.requireNonNull(
                accessTokenIssuer,
                "accessTokenIssuer");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public LoginUserResult login(LoginUserCommand command) {
        Objects.requireNonNull(command, "command");

        Username username = Username.of(command.username());
        PresentedPassword presentedPassword = PresentedPassword.of(
                command.rawPassword());
        UserAuthentication authentication = userRepository
                .findAuthenticationByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordHasher.matches(
                presentedPassword,
                authentication.passwordHash());
        if (!passwordMatches
                || authentication.status() != AccountStatus.NORMAL) {
            throw new InvalidCredentialsException();
        }

        Instant authenticatedAt = Instant.ofEpochSecond(
                clock.instant().getEpochSecond());
        String sessionId = sessionIdGenerator.generate();
        IssuedAccessToken issuedToken = accessTokenIssuer.issue(
                new AccessTokenIssueRequest(
                        authentication.userId(),
                        sessionId,
                        DEFAULT_USER_SCOPES,
                        authenticatedAt));

        return new LoginUserResult(
                issuedToken.tokenValue(),
                issuedToken.expiresAt());
    }
}
