package com.everypicfound.identity.domain.model.user;

/**
 * 注册原始密码校验失败的领域原因。
 */
public enum PasswordViolation {
    REQUIRED,
    LENGTH,
    UTF8_TOO_LONG,
    WHITESPACE,
    CONTROL_CHARACTER
}
