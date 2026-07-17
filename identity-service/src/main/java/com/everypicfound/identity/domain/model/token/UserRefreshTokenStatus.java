package com.everypicfound.identity.domain.model.token;

/**
 * Refresh Token 状态。
 */
public enum UserRefreshTokenStatus {
    ACTIVE,
    USED,
    REVOKED,
    EXPIRED
}
