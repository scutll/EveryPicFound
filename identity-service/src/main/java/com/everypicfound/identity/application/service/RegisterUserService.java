package com.everypicfound.identity.application.service;

import com.everypicfound.identity.application.command.RegisterUserCommand;
import com.everypicfound.identity.application.port.in.RegisterUserUseCase;
import com.everypicfound.identity.application.port.out.PasswordHasher;
import com.everypicfound.identity.application.result.RegisterUserResult;
import com.everypicfound.identity.domain.model.user.Nickname;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.RawPassword;
import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.model.user.UsernameAlreadyExistsException;
import com.everypicfound.identity.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * 用户注册用例编排。
 */
@Service
public final class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final UserAccountRegistrationTransaction registrationTransaction;
    private final Clock clock;

    public RegisterUserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            UserAccountRegistrationTransaction registrationTransaction,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository");
        this.passwordHasher = Objects.requireNonNull(
                passwordHasher,
                "passwordHasher");
        this.registrationTransaction = Objects.requireNonNull(
                registrationTransaction,
                "registrationTransaction");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public RegisterUserResult register(RegisterUserCommand command) {
        Objects.requireNonNull(command, "command");

        Username username = Username.of(command.username());
        RawPassword rawPassword = RawPassword.of(command.rawPassword());
        Optional<Nickname> nickname = Nickname.optionalOf(
                command.nickname());

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }

        PasswordHash passwordHash = passwordHasher.hash(rawPassword);
        UserAccount account = UserAccount.register(
                username,
                passwordHash,
                nickname,
                clock);
        long userId = registrationTransaction.save(account);

        return new RegisterUserResult(
                userId,
                account.username().value(),
                account.nickname().map(Nickname::value).orElse(null),
                account.displayName());
    }
}
