package com.everypicfound.identity.application.service;

import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 只包围用户账户写入的短事务边界。
 */
@Service
public class UserAccountRegistrationTransaction {

    private final UserRepository userRepository;

    public UserAccountRegistrationTransaction(
            UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository");
    }

    @Transactional
    public long save(UserAccount account) {
        return userRepository.save(account);
    }
}
