package com.everypicfound.identity.interfaces.rest.controller;

import com.everypicfound.identity.application.command.LogoutCurrentSessionCommand;
import com.everypicfound.identity.application.exception.InvalidAccessTokenException;
import com.everypicfound.identity.application.port.in.LoginUserUseCase;
import com.everypicfound.identity.application.port.in.LogoutCurrentSessionUseCase;
import com.everypicfound.identity.application.port.in.RegisterUserUseCase;
import com.everypicfound.identity.application.port.in.RefreshTokenUseCase;
import com.everypicfound.identity.application.result.LoginUserResult;
import com.everypicfound.identity.application.result.RefreshTokenResult;
import com.everypicfound.identity.application.result.RegisterUserResult;
import com.everypicfound.identity.interfaces.rest.request.LoginUserRequest;
import com.everypicfound.identity.interfaces.rest.request.RegisterUserRequest;
import com.everypicfound.identity.interfaces.rest.request.RefreshTokenRequest;
import com.everypicfound.identity.interfaces.rest.response.LoginUserResponse;
import com.everypicfound.identity.interfaces.rest.response.RegisterUserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 注册与认证相关 HTTP 接口。
 */
@RestController
@RequestMapping("/api/auth")
public final class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutCurrentSessionUseCase logoutCurrentSessionUseCase;
    private final JwtDecoder jwtDecoder;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutCurrentSessionUseCase logoutCurrentSessionUseCase,
            JwtDecoder jwtDecoder) {
        this.registerUserUseCase = Objects.requireNonNull(
                registerUserUseCase,
                "registerUserUseCase");
        this.loginUserUseCase = Objects.requireNonNull(
                loginUserUseCase,
                "loginUserUseCase");
        this.refreshTokenUseCase = Objects.requireNonNull(
                refreshTokenUseCase,
                "refreshTokenUseCase");
        this.logoutCurrentSessionUseCase = Objects.requireNonNull(
                logoutCurrentSessionUseCase,
                "logoutCurrentSessionUseCase");
        this.jwtDecoder = Objects.requireNonNull(jwtDecoder, "jwtDecoder");
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(
            @RequestBody RegisterUserRequest request) {
        RegisterUserResult result = registerUserUseCase.register(
                request.toCommand());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RegisterUserResponse.from(result));
    }

    @PostMapping("/login")
    public LoginUserResponse login(@RequestBody LoginUserRequest request) {
        LoginUserResult result = loginUserUseCase.login(
                request.toCommand());
        return LoginUserResponse.from(result);
    }

    @PostMapping("/refresh")
    public LoginUserResponse refresh(@RequestBody RefreshTokenRequest request) {
        RefreshTokenResult result = refreshTokenUseCase.refresh(
                request.toCommand());
        return LoginUserResponse.from(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(name = "Authorization", required = false)
            String authorizationHeader) {
        Jwt jwt = decodeBearerToken(authorizationHeader);
        logoutCurrentSessionUseCase.logout(new LogoutCurrentSessionCommand(
                parseUserId(jwt.getSubject()),
                requiredClaim(jwt, "sid")));
        return ResponseEntity.noContent().build();
    }

    private Jwt decodeBearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidAccessTokenException();
        }
        String tokenValue = authorizationHeader.substring("Bearer ".length());
        if (tokenValue.isBlank()) {
            throw new InvalidAccessTokenException();
        }
        try {
            return jwtDecoder.decode(tokenValue);
        } catch (JwtException exception) {
            throw new InvalidAccessTokenException();
        }
    }

    private static long parseUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new InvalidAccessTokenException();
        }
        try {
            long userId = Long.parseLong(subject);
            if (userId <= 0) {
                throw new InvalidAccessTokenException();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new InvalidAccessTokenException();
        }
    }

    private static String requiredClaim(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null || value.isBlank()) {
            throw new InvalidAccessTokenException();
        }
        return value;
    }
}
