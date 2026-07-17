package com.everypicfound.identity.interfaces.rest.controller;

import com.everypicfound.identity.application.command.LoginUserCommand;
import com.everypicfound.identity.application.command.RegisterUserCommand;
import com.everypicfound.identity.application.command.RefreshTokenCommand;
import com.everypicfound.identity.application.exception.InvalidCredentialsException;
import com.everypicfound.identity.application.exception.InvalidRefreshTokenException;
import com.everypicfound.identity.application.port.in.LoginUserUseCase;
import com.everypicfound.identity.application.port.in.RegisterUserUseCase;
import com.everypicfound.identity.application.port.in.RefreshTokenUseCase;
import com.everypicfound.identity.application.result.LoginUserResult;
import com.everypicfound.identity.application.result.RefreshTokenResult;
import com.everypicfound.identity.application.result.RegisterUserResult;
import com.everypicfound.identity.domain.model.user.InvalidUsernameException;
import com.everypicfound.identity.domain.model.user.UsernameAlreadyExistsException;
import com.everypicfound.identity.domain.model.user.UsernameViolation;
import com.everypicfound.identity.interfaces.rest.request.LoginUserRequest;
import com.everypicfound.identity.interfaces.rest.request.RegisterUserRequest;
import com.everypicfound.identity.interfaces.rest.response.LoginUserResponse;
import com.everypicfound.identity.support.exception.AccessTokenIssuanceException;
import com.everypicfound.identity.support.exception.PasswordHashingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private LoginUserUseCase loginUserUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @Test
    void registersUserAndReturnsCreatedPublicResponse() throws Exception {
        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenReturn(new RegisterUserResult(
                        42L,
                        "User_01",
                        "探索者",
                        "探索者"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "  User_01  ",
                                  "password": "secret123",
                                  "nickname": "  探索者  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(42L))
                .andExpect(jsonPath("$.username").value("User_01"))
                .andExpect(jsonPath("$.nickname").value("探索者"))
                .andExpect(jsonPath("$.displayName").value("探索者"));

        RegisterUserCommand expectedCommand = new RegisterUserCommand(
                "  User_01  ",
                "secret123",
                "  探索者  ");
        verify(registerUserUseCase).register(
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.username().equals(expectedCommand.username())
                                && command.rawPassword().equals(
                                expectedCommand.rawPassword())
                                && command.nickname().equals(
                                expectedCommand.nickname())));
    }

    @Test
    void protectsPasswordWhenRequestIsRenderedAsText() {
        RegisterUserRequest request = new RegisterUserRequest(
                "User_01",
                "secret123",
                null);

        assertThat(request.toString())
                .contains("password=PROTECTED")
                .doesNotContain("secret123");
    }

    @Test
    void logsInAndReturnsBearerAccessToken() throws Exception {
        when(loginUserUseCase.login(any(LoginUserCommand.class)))
                .thenReturn(new LoginUserResult(
                        "header.payload.signature",
                        Instant.parse("2026-07-17T10:30:00Z"),
                        "refresh-token-raw",
                        Instant.parse("2026-07-17T11:00:00Z")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "  User_01  ",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken")
                        .value("header.payload.signature"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2026-07-17T10:30:00Z"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("refresh-token-raw"))
                .andExpect(jsonPath("$.refreshTokenExpiresAt")
                        .value("2026-07-17T11:00:00Z"));

        verify(loginUserUseCase).login(
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.username().equals("  User_01  ")
                                && command.rawPassword()
                                .equals("secret123")));
    }

    @Test
    void protectsLoginPasswordAndAccessTokenWhenRenderedAsText() {
        LoginUserRequest request = new LoginUserRequest(
                "User_01",
                "secret123");
        LoginUserResponse response = new LoginUserResponse(
                "header.payload.signature",
                "Bearer",
                Instant.parse("2026-07-17T10:30:00Z"),
                "refresh-token-raw",
                Instant.parse("2026-07-17T11:00:00Z"));

        assertThat(request.toString())
                .contains("password=PROTECTED")
                .doesNotContain("secret123");
        assertThat(response.toString())
                .contains("accessToken=PROTECTED")
                .contains("refreshToken=PROTECTED")
                .doesNotContain("header.payload.signature")
                .doesNotContain("refresh-token-raw");
    }

    @Test
    void returnsUnauthorizedForInvalidCredentials() throws Exception {
        when(loginUserUseCase.login(any(LoginUserCommand.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "User_01",
                                  "password": "wrong123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode")
                        .value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message")
                        .value("登录凭据无效"))
                .andExpect(jsonPath("$.field").value((Object) null));
    }

    @Test
    void refreshesTokenPairAndReturnsRotatedRefreshToken() throws Exception {
        when(refreshTokenUseCase.refresh(any(RefreshTokenCommand.class)))
                .thenReturn(new RefreshTokenResult(
                        "new.header.payload.signature",
                        Instant.parse("2026-07-17T11:10:00Z"),
                        "new-refresh-token",
                        Instant.parse("2026-07-17T11:40:00Z")));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "old-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken")
                        .value("new.header.payload.signature"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2026-07-17T11:10:00Z"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("new-refresh-token"))
                .andExpect(jsonPath("$.refreshTokenExpiresAt")
                        .value("2026-07-17T11:40:00Z"));

        verify(refreshTokenUseCase).refresh(
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.refreshToken().equals("old-refresh-token")));
    }

    @Test
    void returnsUnauthorizedForInvalidRefreshToken() throws Exception {
        when(refreshTokenUseCase.refresh(any(RefreshTokenCommand.class)))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "used-or-missing-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode")
                        .value("AUTH_INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message")
                        .value("刷新令牌无效"))
                .andExpect(jsonPath("$.field").value((Object) null));
    }

    @Test
    void hidesAccessTokenIssuanceFailureBehindInternalError()
            throws Exception {
        when(loginUserUseCase.login(any(LoginUserCommand.class)))
                .thenThrow(new AccessTokenIssuanceException(
                        "access token issuance failed",
                        new IllegalStateException("encoder unavailable")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "User_01",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode")
                        .value("SYSTEM_INTERNAL_ERROR"))
                .andExpect(jsonPath("$.field").value((Object) null));
    }

    @Test
    void returnsFieldErrorWhenUsernameViolatesDomainRule()
            throws Exception {
        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new InvalidUsernameException(
                        UsernameViolation.FORMAT));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bad name",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("USER_USERNAME_FORMAT_INVALID"))
                .andExpect(jsonPath("$.field").value("username"));
    }

    @Test
    void returnsConflictWhenUsernameAlreadyExists() throws Exception {
        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new UsernameAlreadyExistsException());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "User_01",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode")
                        .value("USER_USERNAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.field").value("username"));
    }

    @Test
    void hidesPasswordHashingFailureBehindInternalError()
            throws Exception {
        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new PasswordHashingException(
                        "password hashing failed",
                        new IllegalStateException("encoder unavailable")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "User_01",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode")
                        .value("SYSTEM_INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("服务器内部错误"))
                .andExpect(jsonPath("$.field").value((Object) null));
    }

    @Test
    void hidesDatabaseFailureBehindSameInternalError()
            throws Exception {
        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new DataAccessResourceFailureException(
                        "database unavailable"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "User_01",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode")
                        .value("SYSTEM_INTERNAL_ERROR"))
                .andExpect(jsonPath("$.field").value((Object) null));
    }
}
