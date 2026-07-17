package com.everypicfound.identity.interfaces.rest.controller;

import com.everypicfound.identity.application.port.in.LoginUserUseCase;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase) {
        this.registerUserUseCase = Objects.requireNonNull(
                registerUserUseCase,
                "registerUserUseCase");
        this.loginUserUseCase = Objects.requireNonNull(
                loginUserUseCase,
                "loginUserUseCase");
        this.refreshTokenUseCase = Objects.requireNonNull(
                refreshTokenUseCase,
                "refreshTokenUseCase");
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
}
