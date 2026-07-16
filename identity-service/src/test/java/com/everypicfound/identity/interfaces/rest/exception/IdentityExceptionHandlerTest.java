package com.everypicfound.identity.interfaces.rest.exception;

import com.everypicfound.identity.domain.model.user.InvalidPasswordException;
import com.everypicfound.identity.domain.model.user.InvalidNicknameException;
import com.everypicfound.identity.domain.model.user.InvalidUsernameException;
import com.everypicfound.identity.domain.model.user.NicknameViolation;
import com.everypicfound.identity.domain.model.user.PasswordViolation;
import com.everypicfound.identity.domain.model.user.UsernameViolation;
import com.everypicfound.identity.interfaces.rest.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityExceptionHandlerTest {

    private final IdentityExceptionHandler handler =
            new IdentityExceptionHandler();

    @Test
    void shouldMapUsernameLengthViolationToClientError() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleInvalidUsername(
                        new InvalidUsernameException(
                                UsernameViolation.LENGTH));

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode())
                .isEqualTo("USER_USERNAME_LENGTH_INVALID");
        assertThat(response.getBody().field()).isEqualTo("username");
    }

    @Test
    void shouldMapPasswordUtf8ViolationToClientError() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleInvalidPassword(
                        new InvalidPasswordException(
                                PasswordViolation.UTF8_TOO_LONG));

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode())
                .isEqualTo("USER_PASSWORD_UTF8_TOO_LONG");
        assertThat(response.getBody().field()).isEqualTo("password");
    }

    @Test
    void shouldMapNicknameLengthViolationToClientError() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleInvalidNickname(
                        new InvalidNicknameException(
                                NicknameViolation.LENGTH));

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode())
                .isEqualTo("USER_NICKNAME_LENGTH_INVALID");
        assertThat(response.getBody().field()).isEqualTo("nickname");
    }
}
