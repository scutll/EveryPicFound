package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.UpdateMyProfileCommand;
import com.everypicfound.identity.application.result.UserProfileResult;
import com.everypicfound.identity.domain.model.user.UserProfile;
import com.everypicfound.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(
                userRepository,
                Clock.fixed(
                        Instant.parse("2026-07-18T03:00:00Z"),
                        ZoneOffset.UTC));
    }

    @Test
    void returnsCurrentUserProfile() {
        when(userRepository.findProfileById(42L))
                .thenReturn(Optional.of(new UserProfile(
                        42L,
                        "User_01",
                        "探索者",
                        "https://cdn.example.com/avatar.png")));

        UserProfileResult result = service.getCurrentUser(42L);

        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.username()).isEqualTo("User_01");
        assertThat(result.nickname()).isEqualTo("探索者");
        assertThat(result.displayName()).isEqualTo("探索者");
        assertThat(result.avatarUrl())
                .isEqualTo("https://cdn.example.com/avatar.png");
    }

    @Test
    void updatesNicknameAndAvatarUrl() {
        when(userRepository.updateProfile(
                42L,
                "图友",
                "https://cdn.example.com/new.png",
                Instant.parse("2026-07-18T03:00:00Z")))
                .thenReturn(true);
        when(userRepository.findProfileById(42L))
                .thenReturn(Optional.of(new UserProfile(
                        42L,
                        "User_01",
                        "图友",
                        "https://cdn.example.com/new.png")));

        UserProfileResult result = service.updateMyProfile(
                new UpdateMyProfileCommand(
                        42L,
                        "  图友  ",
                        "https://cdn.example.com/new.png"));

        verify(userRepository).updateProfile(
                42L,
                "图友",
                "https://cdn.example.com/new.png",
                Instant.parse("2026-07-18T03:00:00Z"));
        assertThat(result.displayName()).isEqualTo("图友");
        assertThat(result.avatarUrl())
                .isEqualTo("https://cdn.example.com/new.png");
    }

    @Test
    void clearingNicknameMakesDisplayNameFallBackToUsername() {
        when(userRepository.updateProfile(
                42L,
                null,
                null,
                Instant.parse("2026-07-18T03:00:00Z")))
                .thenReturn(true);
        when(userRepository.findProfileById(42L))
                .thenReturn(Optional.of(new UserProfile(
                        42L,
                        "User_01",
                        null,
                        null)));

        UserProfileResult result = service.updateMyProfile(
                new UpdateMyProfileCommand(42L, " ", null));

        assertThat(result.nickname()).isNull();
        assertThat(result.displayName()).isEqualTo("User_01");
    }
}
