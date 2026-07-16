package com.everypicfound.identity.domain.repository;

import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.Username;

/**
 * 用户账户持久化端口。
 */
public interface UserRepository {

    boolean existsByUsername(Username username);

    long save(UserAccount account);
}
