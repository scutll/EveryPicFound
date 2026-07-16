package com.everypicfound.identity.domain.model.user;

import com.everypicfound.identity.domain.enums.AccountStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 用户账户聚合。
 */
public final class UserAccount {

    private final Long id;
    private final Username username;
    private final PasswordHash passwordHash;
    private final Nickname nickname;
    private final String avatarUrl;
    private final AccountStatus status;
    private final Instant authValidAfter;
    private final Instant lastLoginTime;
    private final int version;
    private final Instant createdTime;
    private final Instant updatedTime;

    private UserAccount(
            Long id,
            Username username,
            PasswordHash passwordHash,
            Nickname nickname,
            String avatarUrl,
            AccountStatus status,
            Instant authValidAfter,
            Instant lastLoginTime,
            int version,
            Instant createdTime,
            Instant updatedTime) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.authValidAfter = authValidAfter;
        this.lastLoginTime = lastLoginTime;
        this.version = version;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
    }

    /**
     * 创建尚未持久化的新账户。
     *
     * <p>Clock 只读取一次，以保证创建时间、更新时间与认证
     * 分界时间严格相同。自增 ID 在持久化后产生。</p>
     */
    public static UserAccount register(
            Username username,
            PasswordHash passwordHash,
            Optional<Nickname> nickname,
            Clock clock) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(nickname, "nickname");
        Objects.requireNonNull(clock, "clock");

        Instant registeredAt = clock.instant();
        return new UserAccount(
                null,
                username,
                passwordHash,
                nickname.orElse(null),
                null,
                AccountStatus.NORMAL,
                registeredAt,
                null,
                0,
                registeredAt,
                registeredAt);
    }

    public Long id() {
        return id;
    }

    public Username username() {
        return username;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Optional<Nickname> nickname() {
        return Optional.ofNullable(nickname);
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant authValidAfter() {
        return authValidAfter;
    }

    public Instant lastLoginTime() {
        return lastLoginTime;
    }

    public int version() {
        return version;
    }

    public Instant createdTime() {
        return createdTime;
    }

    public Instant updatedTime() {
        return updatedTime;
    }

    public String displayName() {
        return nickname == null
                ? username.value()
                : nickname.value();
    }
}
