package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.everypicfound.identity.domain.enums.AccountStatus;
import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.domain.model.user.UserProfile;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.model.user.UsernameAlreadyExistsException;
import com.everypicfound.identity.domain.repository.UserRepository;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserAccountPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserAccountMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserAccountPo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的用户账户仓储适配器。
 */
@Repository
public class MyBatisUserRepository implements UserRepository {

    private final UserAccountMapper mapper;
    private final UserAccountPersistenceConverter converter;

    public MyBatisUserRepository(
            UserAccountMapper mapper,
            UserAccountPersistenceConverter converter) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.converter = Objects.requireNonNull(converter, "converter");
    }

    @Override
    public boolean existsByUsername(Username username) {
        Objects.requireNonNull(username, "username");
        Long count = mapper.selectCount(
                Wrappers.<UserAccountPo>lambdaQuery()
                        .eq(UserAccountPo::getUsername, username.value()));
        return count > 0;
    }

    @Override
    public Optional<UserAuthentication> findAuthenticationByUsername(
            Username username) {
        Objects.requireNonNull(username, "username");
        UserAccountPo po = mapper.selectOne(
                Wrappers.<UserAccountPo>lambdaQuery()
                        .eq(UserAccountPo::getUsername, username.value()));
        return Optional.ofNullable(po)
                .map(converter::toAuthentication);
    }

    @Override
    public Optional<UserAuthentication> findAuthenticationById(long userId) {
        UserAccountPo po = mapper.selectById(userId);
        return Optional.ofNullable(po)
                .map(converter::toAuthentication);
    }

    @Override
    public Optional<UserProfile> findProfileById(long userId) {
        UserAccountPo po = mapper.selectById(userId);
        if (po == null || !AccountStatus.NORMAL.name().equals(po.getStatus())) {
            return Optional.empty();
        }
        return Optional.of(new UserProfile(
                po.getId(),
                po.getUsername(),
                po.getNickname(),
                po.getAvatarUrl()));
    }

    @Override
    public boolean updateProfile(
            long userId,
            String nickname,
            String avatarUrl,
            Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "updatedAt");
        int affectedRows = mapper.update(
                null,
                Wrappers.<UserAccountPo>update()
                        .set("nickname", nickname)
                        .set("avatar_url", avatarUrl)
                        .set("updated_time", toUtcDateTime(updatedAt))
                        .eq("id", userId)
                        .eq("status", AccountStatus.NORMAL.name()));
        return affectedRows == 1;
    }

    @Override
    public boolean changePassword(
            long userId,
            String passwordHash,
            Instant changedAt) {
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(changedAt, "changedAt");
        LocalDateTime changedTime = toUtcDateTime(changedAt);
        int affectedRows = mapper.update(
                null,
                Wrappers.<UserAccountPo>update()
                        .set("password_hash", passwordHash)
                        .set("auth_valid_after", changedTime)
                        .set("updated_time", changedTime)
                        .setSql("version = version + 1")
                        .eq("id", userId)
                        .eq("status", AccountStatus.NORMAL.name()));
        return affectedRows == 1;
    }

    @Override
    public boolean deleteAccount(
            long userId,
            Instant deletedAt) {
        Objects.requireNonNull(deletedAt, "deletedAt");
        LocalDateTime deletedTime = toUtcDateTime(deletedAt);
        int affectedRows = mapper.update(
                null,
                Wrappers.<UserAccountPo>update()
                        .set("status", AccountStatus.DELETED.name())
                        .set("auth_valid_after", deletedTime)
                        .set("updated_time", deletedTime)
                        .setSql("username = CONCAT('#deleted#', id, '#', username)")
                        .setSql("version = version + 1")
                        .eq("id", userId)
                        .eq("status", AccountStatus.NORMAL.name()));
        return affectedRows == 1;
    }

    @Override
    public long save(UserAccount account) {
        UserAccountPo po = converter.toPo(account);
        try {
            int affectedRows = mapper.insert(po);
            if (affectedRows != 1) {
                throw new IllegalStateException(
                        "expected one inserted user account row");
            }
        } catch (DuplicateKeyException exception) {
            throw new UsernameAlreadyExistsException(exception);
        }

        if (po.getId() == null) {
            throw new IllegalStateException(
                    "generated user account id was not returned");
        }
        return po.getId();
    }

    private static LocalDateTime toUtcDateTime(Instant instant) {
        Instant millisecondPrecision = instant.truncatedTo(ChronoUnit.MILLIS);
        return LocalDateTime.ofInstant(millisecondPrecision, ZoneOffset.UTC);
    }
}
