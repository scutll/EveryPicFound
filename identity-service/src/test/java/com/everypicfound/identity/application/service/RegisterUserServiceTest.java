package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.RegisterUserCommand;
import com.everypicfound.identity.application.port.out.PasswordHasher;
import com.everypicfound.identity.application.result.RegisterUserResult;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.RawPassword;
import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.model.user.UsernameAlreadyExistsException;
import com.everypicfound.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    private static final Instant REGISTERED_AT =
            Instant.parse("2026-07-16T08:00:00.123Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private UserAccountRegistrationTransaction registrationTransaction;

    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(
                userRepository,
                passwordHasher,
                registrationTransaction,
                Clock.fixed(REGISTERED_AT, ZoneOffset.UTC));
    }

    @Test
    void registersNormalizedAccountAndReturnsPublicResult() {
        when(userRepository.existsByUsername(any(Username.class)))
                .thenReturn(false);
        when(passwordHasher.hash(any(RawPassword.class)))
                .thenReturn(PasswordHash.of("{bcrypt}encoded-password"));
        when(registrationTransaction.save(any(UserAccount.class)))
                .thenReturn(42L);

        RegisterUserResult result = service.register(
                new RegisterUserCommand(
                        "  User_01  ",
                        "secret123",
                        "  探索者  "));

        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.username()).isEqualTo("User_01");
        assertThat(result.nickname()).isEqualTo("探索者");
        assertThat(result.displayName()).isEqualTo("探索者");

        ArgumentCaptor<UserAccount> accountCaptor =
                ArgumentCaptor.forClass(UserAccount.class);
        InOrder order = inOrder(
                userRepository,
                passwordHasher,
                registrationTransaction);
        order.verify(userRepository)
                .existsByUsername(argThat(username ->
                        username.value().equals("User_01")));
        order.verify(passwordHasher)
                .hash(any(RawPassword.class));
        order.verify(registrationTransaction)
                .save(accountCaptor.capture());

        UserAccount savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.id()).isNull();
        assertThat(savedAccount.passwordHash().value())
                .isEqualTo("{bcrypt}encoded-password");
        assertThat(savedAccount.createdTime()).isEqualTo(REGISTERED_AT);
        assertThat(savedAccount.authValidAfter()).isEqualTo(REGISTERED_AT);
    }

    @Test
    void rejectsExistingUsernameBeforeHashingOrOpeningWriteTransaction() {
        when(userRepository.existsByUsername(any(Username.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand(
                        "User01",
                        "secret123",
                        null)))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verifyNoInteractions(passwordHasher, registrationTransaction);
    }
}
