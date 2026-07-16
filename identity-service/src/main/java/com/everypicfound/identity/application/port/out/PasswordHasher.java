package com.everypicfound.identity.application.port.out;

import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.RawPassword;

/**
 * 原始密码哈希与验证端口。
 */
public interface PasswordHasher {

    PasswordHash hash(RawPassword rawPassword);

    boolean matches(RawPassword rawPassword, PasswordHash passwordHash);
}
