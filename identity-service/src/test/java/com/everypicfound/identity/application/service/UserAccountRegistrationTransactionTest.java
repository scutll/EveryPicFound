package com.everypicfound.identity.application.service;

import com.everypicfound.identity.domain.model.user.Nickname;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountRegistrationTransactionTest {

    @Test
    void delegatesAccountInsertToRepository() {
        UserRepository repository = mock(UserRepository.class);
        UserAccount account = newAccount();
        when(repository.save(account)).thenReturn(42L);
        UserAccountRegistrationTransaction transaction =
                new UserAccountRegistrationTransaction(repository);

        long userId = transaction.save(account);

        assertThat(userId).isEqualTo(42L);
        verify(repository).save(account);
    }

    @Test
    void saveDefinesTheShortTransactionBoundary() throws Exception {
        Method saveMethod = UserAccountRegistrationTransaction.class
                .getMethod("save", UserAccount.class);

        assertThat(saveMethod.isAnnotationPresent(Transactional.class))
                .isTrue();
    }

    private static UserAccount newAccount() {
        return UserAccount.register(
                Username.of("User01"),
                PasswordHash.of("{bcrypt}encoded-password"),
                Nickname.optionalOf(null),
                Clock.fixed(
                        Instant.parse("2026-07-16T08:00:00.123Z"),
                        ZoneOffset.UTC));
    }
}
