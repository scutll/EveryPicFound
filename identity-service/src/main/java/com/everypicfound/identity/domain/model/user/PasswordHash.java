package com.everypicfound.identity.domain.model.user;

import java.util.Objects;

/**
 * 已由密码编码器生成、可持久化的密码摘要。
 */
public final class PasswordHash {

    private final String value;

    private PasswordHash(String value) {
        this.value = value;
    }

    public static PasswordHash of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "password hash must not be blank");
        }
        return new PasswordHash(value);
    }

    /**
     * 返回供持久化或密码验证适配器使用的摘要值。
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PasswordHash passwordHash)) {
            return false;
        }
        return value.equals(passwordHash.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "PasswordHash[PROTECTED]";
    }
}
