package com.everypicfound.identity.domain.repository;

import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.domain.model.user.UserProfile;
import com.everypicfound.identity.domain.model.user.Username;

import java.time.Instant;
import java.util.Optional;

/**
 * 用户账户持久化端口。
 */
public interface UserRepository {

    boolean existsByUsername(Username username);

    Optional<UserAuthentication> findAuthenticationByUsername(
            Username username);

    Optional<UserProfile> findProfileById(long userId);

    boolean updateProfile(
            long userId,
            String nickname,
            String avatarUrl,
            Instant updatedAt);

    long save(UserAccount account);
}
