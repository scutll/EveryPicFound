package com.everypicfound.identity.support.error;

import org.springframework.http.HttpStatus;

/**
 * 用户与认证 HTTP 边界使用的稳定错误码。
 */
public enum UserErrorCode {

    USER_USERNAME_REQUIRED(
            "用户名不能为空", "username", HttpStatus.BAD_REQUEST),
    USER_USERNAME_LENGTH_INVALID(
            "用户名长度必须为 3 到 32 个字符",
            "username",
            HttpStatus.BAD_REQUEST),
    USER_USERNAME_FORMAT_INVALID(
            "用户名只能包含英文字母、数字和符合规则的下划线",
            "username",
            HttpStatus.BAD_REQUEST),
    USER_NICKNAME_LENGTH_INVALID(
            "昵称长度不能超过 32 个字符",
            "nickname",
            HttpStatus.BAD_REQUEST),
    USER_NICKNAME_INVALID_CHARACTER(
            "昵称不能包含控制字符或换行符",
            "nickname",
            HttpStatus.BAD_REQUEST),
    USER_PASSWORD_REQUIRED(
            "密码不能为空", "password", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_LENGTH_INVALID(
            "密码长度必须为 6 到 25 个字符",
            "password",
            HttpStatus.BAD_REQUEST),
    USER_PASSWORD_UTF8_TOO_LONG(
            "密码的 UTF-8 编码不能超过 72 字节",
            "password",
            HttpStatus.BAD_REQUEST),
    USER_PASSWORD_WHITESPACE_NOT_ALLOWED(
            "密码不能包含空白字符",
            "password",
            HttpStatus.BAD_REQUEST),
    USER_PASSWORD_CONTROL_CHARACTER_NOT_ALLOWED(
            "密码不能包含控制字符",
            "password",
            HttpStatus.BAD_REQUEST),
    USER_USERNAME_ALREADY_EXISTS(
            "用户名已被使用",
            "username",
            HttpStatus.CONFLICT),
    AUTH_INVALID_CREDENTIALS(
            "登录凭据无效",
            null,
            HttpStatus.UNAUTHORIZED),
    SYSTEM_INTERNAL_ERROR(
            "服务器内部错误",
            null,
            HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final String field;
    private final HttpStatus httpStatus;

    UserErrorCode(
            String message,
            String field,
            HttpStatus httpStatus) {
        this.message = message;
        this.field = field;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return name();
    }

    public String message() {
        return message;
    }

    public String field() {
        return field;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
