package com.everypicfound.identity.interfaces.rest.controller;

import com.everypicfound.identity.application.command.RegisterUserCommand;
import com.everypicfound.identity.application.port.in.RegisterUserUseCase;
import com.everypicfound.identity.application.result.RegisterUserResult;
import com.everypicfound.identity.domain.model.user.InvalidUsernameException;
import com.everypicfound.identity.domain.model.user.UsernameAlreadyExistsException;
import com.everypicfound.identity.domain.model.user.UsernameViolation;
import com.everypicfound.identity.interfaces.rest.request.RegisterUserRequest;
import com.everypicfound.identity.support.exception.PasswordHashingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
