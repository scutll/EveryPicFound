package com.everypicfound.identity.interfaces.rest.exception;

import com.everypicfound.identity.application.exception.InvalidCredentialsException;
import com.everypicfound.identity.domain.model.user.InvalidPasswordException;
import com.everypicfound.identity.domain.model.user.InvalidNicknameException;
import com.everypicfound.identity.domain.model.user.InvalidUsernameException;
import com.everypicfound.identity.domain.model.user.NicknameViolation;
import com.everypicfound.identity.domain.model.user.PasswordViolation;
import com.everypicfound.identity.domain.model.user.UsernameViolation;
import com.everypicfound.identity.domain.model.user.UsernameAlreadyExistsException;
import com.everypicfound.identity.interfaces.rest.response.ApiErrorResponse;
import com.everypicfound.identity.support.error.UserErrorCode;
import com.everypicfound.identity.support.exception.AccessTokenIssuanceException;
import com.everypicfound.identity.support.exception.PasswordHashingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将 Identity 领域异常转换为稳定的 HTTP 错误契约。
 */
@RestControllerAdvice
public class IdentityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            IdentityExceptionHandler.class);

    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidUsername(
            InvalidUsernameException exception) {
        return responseFor(map(exception.violation()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPassword(
            InvalidPasswordException exception) {
        return responseFor(map(exception.violation()));
    }

    @ExceptionHandler(InvalidNicknameException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidNickname(
            InvalidNicknameException exception) {
        return responseFor(map(exception.violation()));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameAlreadyExists() {
        return responseFor(UserErrorCode.USER_USERNAME_ALREADY_EXISTS);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials() {
        return responseFor(UserErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @ExceptionHandler({
            PasswordHashingException.class,
            AccessTokenIssuanceException.class,
            DataAccessException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInternalFailure(
            RuntimeException exception) {
        LOGGER.error(
                "Identity request failed due to internal error type={}",
                exception.getClass().getSimpleName(),
                exception);
        return responseFor(UserErrorCode.SYSTEM_INTERNAL_ERROR);
    }

    private ResponseEntity<ApiErrorResponse> responseFor(
            UserErrorCode errorCode) {
        ApiErrorResponse body = new ApiErrorResponse(
                errorCode.code(),
                errorCode.message(),
                errorCode.field());
        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(body);
    }

    private UserErrorCode map(UsernameViolation violation) {
        return switch (violation) {
            case REQUIRED -> UserErrorCode.USER_USERNAME_REQUIRED;
            case LENGTH -> UserErrorCode.USER_USERNAME_LENGTH_INVALID;
            case FORMAT -> UserErrorCode.USER_USERNAME_FORMAT_INVALID;
        };
    }

    private UserErrorCode map(PasswordViolation violation) {
        return switch (violation) {
            case REQUIRED -> UserErrorCode.USER_PASSWORD_REQUIRED;
            case LENGTH -> UserErrorCode.USER_PASSWORD_LENGTH_INVALID;
            case UTF8_TOO_LONG ->
                    UserErrorCode.USER_PASSWORD_UTF8_TOO_LONG;
            case WHITESPACE ->
                    UserErrorCode.USER_PASSWORD_WHITESPACE_NOT_ALLOWED;
            case CONTROL_CHARACTER ->
                    UserErrorCode
                            .USER_PASSWORD_CONTROL_CHARACTER_NOT_ALLOWED;
        };
    }

    private UserErrorCode map(NicknameViolation violation) {
        return switch (violation) {
            case LENGTH -> UserErrorCode.USER_NICKNAME_LENGTH_INVALID;
            case INVALID_CHARACTER ->
                    UserErrorCode.USER_NICKNAME_INVALID_CHARACTER;
        };
    }
}
