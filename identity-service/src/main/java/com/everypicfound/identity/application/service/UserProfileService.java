package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.UpdateMyProfileCommand;
import com.everypicfound.identity.application.exception.UserProfileNotFoundException;
import com.everypicfound.identity.application.port.in.GetCurrentUserUseCase;
import com.everypicfound.identity.application.port.in.UpdateMyProfileUseCase;
import com.everypicfound.identity.application.result.UserProfileResult;
import com.everypicfound.identity.domain.model.user.Nickname;
import com.everypicfound.identity.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Objects;

/**
 * 当前用户资料用例编排。
 */
@Service
public final class UserProfileService
        implements GetCurrentUserUseCase, UpdateMyProfileUseCase {

    private final UserRepository userRepository;
    private final Clock clock;

    public UserProfileService(UserRepository userRepository, Clock clock) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public UserProfileResult getCurrentUser(long userId) {
        return userRepository.findProfileById(userId)
                .map(UserProfileResult::from)
                .orElseThrow(UserProfileNotFoundException::new);
    }

    @Override
    public UserProfileResult updateMyProfile(
            UpdateMyProfileCommand command) {
        Objects.requireNonNull(command, "command");

        String nickname = Nickname.optionalOf(command.nickname())
                .map(Nickname::value)
                .orElse(null);
        String avatarUrl = normalizeOptionalText(command.avatarUrl());
        boolean updated = userRepository.updateProfile(
                command.userId(),
                nickname,
                avatarUrl,
                clock.instant());
        if (!updated) {
            throw new UserProfileNotFoundException();
        }
        return getCurrentUser(command.userId());
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }
}
