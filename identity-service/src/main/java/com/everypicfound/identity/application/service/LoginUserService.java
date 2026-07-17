package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.AccessTokenIssueRequest;
import com.everypicfound.identity.application.command.LoginUserCommand;
import com.everypicfound.identity.application.exception.InvalidCredentialsException;
import com.everypicfound.identity.application.port.in.LoginUserUseCase;
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
import com.everypicfound.identity.domain.model.user.PresentedPassword;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import com.everypicfound.identity.domain.repository.UserRepository;
import com.everypicfound.identity.domain.repository.UserSessionRepository;
import com.everypicfound.security.contract.SecurityScopes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 用户名密码登录用例。
 */
@Service
public final class LoginUserService implements LoginUserUseCase {

    private static final Duration SESSION_TTL = Duration.ofDays(1);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofHours(1);

    private static final List<String> DEFAULT_USER_SCOPES = List.of(
            SecurityScopes.IMAGE_READ,
            SecurityScopes.IMAGE_SEARCH,
            SecurityScopes.IMAGE_UPLOAD,
            SecurityScopes.USER_READ,
            SecurityScopes.USER_WRITE);

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final SessionIdGenerator sessionIdGenerator;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenHasher refreshTokenHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    public LoginUserService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            UserRefreshTokenRepository userRefreshTokenRepository,
            PasswordHasher passwordHasher,
            SessionIdGenerator sessionIdGenerator,
            RefreshTokenGenerator refreshTokenGenerator,
            RefreshTokenHasher refreshTokenHasher,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository");
        this.userSessionRepository = Objects.requireNonNull(
                userSessionRepository,
                "userSessionRepository");
        this.userRefreshTokenRepository = Objects.requireNonNull(
                userRefreshTokenRepository,
                "userRefreshTokenRepository");
        this.passwordHasher = Objects.requireNonNull(
                passwordHasher,
                "passwordHasher");
        this.sessionIdGenerator = Objects.requireNonNull(
                sessionIdGenerator,
                "sessionIdGenerator");
        this.refreshTokenGenerator = Objects.requireNonNull(
                refreshTokenGenerator,
                "refreshTokenGenerator");
        this.refreshTokenHasher = Objects.requireNonNull(
                refreshTokenHasher,
                "refreshTokenHasher");
        this.accessTokenIssuer = Objects.requireNonNull(
                accessTokenIssuer,
                "accessTokenIssuer");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
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

        UserSession session = UserSession.create(
                sessionId,
                authentication.userId(),
                authenticatedAt,
                authenticatedAt.plus(SESSION_TTL));
        userSessionRepository.save(session);

        String refreshToken = refreshTokenGenerator.generate();
        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);
        Instant refreshTokenExpiresAt = authenticatedAt.plus(
                REFRESH_TOKEN_TTL);
        userRefreshTokenRepository.save(UserRefreshToken.issue(
                sessionId,
                refreshTokenHash,
                authenticatedAt,
                refreshTokenExpiresAt));

        IssuedAccessToken issuedToken = accessTokenIssuer.issue(
                new AccessTokenIssueRequest(
                        authentication.userId(),
                        sessionId,
                        DEFAULT_USER_SCOPES,
                        authenticatedAt));

        return new LoginUserResult(
                issuedToken.tokenValue(),
                issuedToken.expiresAt(),
                refreshToken,
                refreshTokenExpiresAt);
    }
}
