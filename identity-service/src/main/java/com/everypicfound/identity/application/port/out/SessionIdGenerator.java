package com.everypicfound.identity.application.port.out;

/**
 * 为一次成功登录生成不可预测的 Session 标识。
 */
public interface SessionIdGenerator {

    String generate();
}
