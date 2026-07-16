package com.everypicfound.identity.domain.model.user;

/**
 * 用户名创建失败的领域原因。
 */
public enum UsernameViolation {
    REQUIRED,
    LENGTH,
    FORMAT
}
