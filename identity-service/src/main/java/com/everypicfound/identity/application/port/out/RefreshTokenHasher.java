package com.everypicfound.identity.application.port.out;

/**
 * 将 Refresh Token 原文转换成数据库中保存的不可逆摘要。
 */
public interface RefreshTokenHasher {

    String hash(String tokenValue);
}
