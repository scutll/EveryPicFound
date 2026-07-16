package com.everypicfound.identity.domain.model.user;

import java.util.Objects;
import java.util.Optional;

/**
 * 用户主动设置的可打印 Unicode 昵称。
 */
public final class Nickname {

    private static final int MAX_CODE_POINTS = 32;

    private final String value;

    private Nickname(String value) {
        this.value = value;
    }

    /**
     * 将可空输入转换为可选昵称。
     *
     * <p>null 或 strip 后为空表示清除昵称，其余输入在
     * strip 后按领域规则校验。</p>
     */
    public static Optional<Nickname> optionalOf(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }

        String normalized = rawValue.strip();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        validate(normalized);
        return Optional.of(new Nickname(normalized));
    }

    public String value() {
        return value;
    }

    private static void validate(String value) {
        int length = value.codePointCount(0, value.length());
        if (length > MAX_CODE_POINTS) {
            throw new InvalidNicknameException(
                    NicknameViolation.LENGTH);
        }

        boolean hasInvalidCharacter = value.codePoints()
                .anyMatch(Nickname::isInvalidCharacter);
        if (hasInvalidCharacter) {
            throw new InvalidNicknameException(
                    NicknameViolation.INVALID_CHARACTER);
        }
    }

    private static boolean isInvalidCharacter(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isISOControl(codePoint)
                || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Nickname nickname)) {
            return false;
        }
        return value.equals(nickname.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Nickname[value=" + value + "]";
    }
}
