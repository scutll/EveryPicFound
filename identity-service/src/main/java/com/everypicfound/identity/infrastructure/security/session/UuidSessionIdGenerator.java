package com.everypicfound.identity.infrastructure.security.session;

import com.everypicfound.identity.application.port.out.SessionIdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 使用随机 UUID 生成当前 Access Token-only 登录的 sid。
 */
@Component
public final class UuidSessionIdGenerator implements SessionIdGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
