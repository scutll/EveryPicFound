package com.everypicfound.identity.application.port.out;

/**
 * 生成返回给客户端保存的 Refresh Token 原文。
 */
public interface RefreshTokenGenerator {

    String generate();
}
