package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.everypicfound.identity.domain.model.user.Nickname;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.UserAccount;
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
