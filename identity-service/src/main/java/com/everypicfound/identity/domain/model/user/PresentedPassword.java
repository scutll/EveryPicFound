package com.everypicfound.identity.domain.model.user;

/**
 * 登录时提交的存量密码凭据。
 *
 * <p>该类型只校验是否提供，不重复应用当前注册密码策略，
 * 以保证密码规则演进后旧账户仍能完成摘要匹配。</p>
 */
public final class PresentedPassword {

    private final String value;

    private PresentedPassword(String value) {
        if (value == null || value.isEmpty()) {
            throw new InvalidPasswordException(
                    PasswordViolation.REQUIRED);
        }
        this.value = value;
    }

    public static PresentedPassword of(String value) {
        return new PresentedPassword(value);
    }

    /**
     * 返回供密码验证适配器使用的原始值，不得写入日志或响应。
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "PresentedPassword[PROTECTED]";
    }
}
