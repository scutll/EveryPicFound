package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.everypicfound.identity.domain.model.user.Nickname;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.domain.model.user.UserProfile;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.model.user.UsernameAlreadyExistsException;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserAccountPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserAccountMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserAccountPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyBatisUserRepositoryTest {

    @Mock
    private UserAccountMapper mapper;

    private MyBatisUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MyBatisUserRepository(
                mapper,
                new UserAccountPersistenceConverter());
    }

    @Test
    void reportsWhetherUsernameExists() {
        when(mapper.selectCount(any())).thenReturn(1L, 0L);

        assertThat(repository.existsByUsername(Username.of("User01")))
                .isTrue();
        assertThat(repository.existsByUsername(Username.of("user01")))
                .isFalse();
    }

    @Test
    void findsMinimalAuthenticationViewByCaseSensitiveUsername() {
        UserAccountPo po = new UserAccountPo();
        po.setId(42L);
        po.setUsername("User01");
        po.setPasswordHash("{bcrypt}encoded-password");
        po.setStatus("NORMAL");
        po.setAuthValidAfter(LocalDateTime.parse(
                "2026-07-16T07:00:00.123"));
        when(mapper.selectOne(any())).thenReturn(po);

        UserAuthentication authentication = repository
                .findAuthenticationByUsername(Username.of("User01"))
                .orElseThrow();

        assertThat(authentication.userId()).isEqualTo(42L);
        assertThat(authentication.passwordHash().value())
                .isEqualTo("{bcrypt}encoded-password");
        assertThat(authentication.status().name()).isEqualTo("NORMAL");
        assertThat(authentication.authValidAfter())
                .isEqualTo(Instant.parse("2026-07-16T07:00:00.123Z"));
        assertThat(authentication.toString())
                .contains("passwordHash=PROTECTED")
                .doesNotContain("encoded-password");
    }

    @Test
    void returnsEmptyWhenUsernameCannotBeAuthenticated() {
        when(mapper.selectOne(any())).thenReturn(null);

        assertThat(repository.findAuthenticationByUsername(
                Username.of("Missing01"))).isEmpty();
    }

    @Test
    void findsUserProfileById() {
        UserAccountPo po = new UserAccountPo();
        po.setId(42L);
        po.setUsername("User01");
        po.setNickname("探索者");
        po.setAvatarUrl("https://cdn.example.com/avatar.png");
        po.setStatus("NORMAL");
        when(mapper.selectById(42L)).thenReturn(po);

        UserProfile profile = repository.findProfileById(42L).orElseThrow();

        assertThat(profile.userId()).isEqualTo(42L);
        assertThat(profile.username()).isEqualTo("User01");
        assertThat(profile.nickname()).isEqualTo("探索者");
        assertThat(profile.displayName()).isEqualTo("探索者");
        assertThat(profile.avatarUrl())
                .isEqualTo("https://cdn.example.com/avatar.png");
    }

    @Test
    void updatesNicknameAndAvatarUrlByUserId() {
        when(mapper.update(any(), any())).thenReturn(1, 0);

        assertThat(repository.updateProfile(
                42L,
                "图友",
                "https://cdn.example.com/new.png",
                Instant.parse("2026-07-18T03:00:00.123Z")))
                .isTrue();
        assertThat(repository.updateProfile(
                99L,
                null,
                null,
                Instant.parse("2026-07-18T03:00:00.123Z")))
                .isFalse();
    }

    @Test
    void returnsGeneratedIdAfterInsert() {
        UserAccount account = newAccount();
        when(mapper.insert(any(UserAccountPo.class)))
                .thenAnswer(invocation -> {
                    UserAccountPo po = invocation.getArgument(0);
                    po.setId(42L);
                    return 1;
                });

        long userId = repository.save(account);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void translatesDuplicateKeyToUsernameAlreadyExists() {
        when(mapper.insert(any(UserAccountPo.class)))
                .thenThrow(new DuplicateKeyException("duplicate username"));

        assertThatThrownBy(() -> repository.save(newAccount()))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasCauseInstanceOf(DuplicateKeyException.class);
    }

    private static UserAccount newAccount() {
        return UserAccount.register(
                Username.of("User01"),
                PasswordHash.of("{bcrypt}encoded-password"),
                Nickname.optionalOf(null),
                Clock.fixed(
                        Instant.parse("2026-07-16T07:00:00.123Z"),
                        ZoneOffset.UTC));
    }
}
