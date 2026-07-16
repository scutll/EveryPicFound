package com.everypicfound.identity.domain.model.user;

import java.nio.charset.StandardCharsets;

/**
 * 尚未进行密码哈希的注册密码。
 *
 * <p>该类型只负责校验原始输入，不执行 trim、大小写转换或
 * Unicode 规范化。调用方应在完成密码哈希后尽快丢弃它。</p>
 */
public final class RawPassword {

    private static final int MIN_CODE_POINTS = 6;
    private static final int MAX_CODE_POINTS = 25;
    private static final int MAX_UTF8_BYTES = 72;

    private final String value;

    private RawPassword(String value) {
        validate(value);
        this.value = value;
    }

    public static RawPassword of(String value) {
        return new RawPassword(value);
    }

    /**
     * 返回供密码编码器使用的原始值，不得写入日志或响应。
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "RawPassword[PROTECTED]";
    }

    private static void validate(String value) {
        if (value == null || value.isEmpty()) {
            throw new InvalidPasswordException(
                    PasswordViolation.REQUIRED);
        }

        if (value.codePoints().anyMatch(Character::isISOControl)) {
            throw new InvalidPasswordException(
                    PasswordViolation.CONTROL_CHARACTER);
        }

        if (value.codePoints().anyMatch(Character::isWhitespace)) {
            throw new InvalidPasswordException(
                    PasswordViolation.WHITESPACE);
        }

        int codePointLength =
                value.codePointCount(0, value.length());
        if (codePointLength < MIN_CODE_POINTS
                || codePointLength > MAX_CODE_POINTS) {
            throw new InvalidPasswordException(
                    PasswordViolation.LENGTH);
        }

        int utf8Length =
                value.getBytes(StandardCharsets.UTF_8).length;
        if (utf8Length > MAX_UTF8_BYTES) {
            throw new InvalidPasswordException(
                    PasswordViolation.UTF8_TOO_LONG);
        }
    }
}
