package com.everypicfound.identity.interfaces.rest.controller;

import com.everypicfound.identity.application.port.in.RegisterUserUseCase;
import com.everypicfound.identity.application.result.RegisterUserResult;
import com.everypicfound.identity.interfaces.rest.request.RegisterUserRequest;
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

    public AuthController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = Objects.requireNonNull(
                registerUserUseCase,
                "registerUserUseCase");
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
}
