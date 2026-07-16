package com.everypicfound.identity.domain.model.user;

import java.util.regex.Pattern;

/**
 * 大小写敏感的用户名值对象。
 *
 * <p>
 * 创建时去除首尾 Unicode 空白，保留原始大小写。
 * 合法用户名长度为 3～32，只允许英文字母、数字，以及位于
 * 字母或数字段之间的单个下划线。
 * </p>
 */
public record Username(String value) {

    private static final int MAX_LENGTH = 32;
    private static final int MIN_LENGTH = 3;

    private static final Pattern VALID_PATTERN = Pattern.compile("[A-Za-z0-9]+(?:_[A-Za-z0-9]+)*");

    public Username {
        if (value == null) {
            throw new InvalidUsernameException(
                    UsernameViolation.REQUIRED);
        }

        value = value.strip();

        if (value.isEmpty()) {
            throw new InvalidUsernameException(
                    UsernameViolation.REQUIRED);
        }

        int length = value.codePointCount(0, value.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new InvalidUsernameException(
                    UsernameViolation.LENGTH);
        }

        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new InvalidUsernameException(
                    UsernameViolation.FORMAT);
        }
    }


    /**
     * 根据接口层接收到的原始字符串创建用户名。
     *
     * @param rawValue 未规范化的用户名
     * @return 经过规范化和校验的用户名
     * @throws InvalidUsernameException 用户名不符合领域规则时
     */
    public static Username of(String rawValue) {
        return new Username(rawValue);
    }
}
