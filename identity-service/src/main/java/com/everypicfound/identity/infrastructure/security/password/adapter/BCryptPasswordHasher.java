package com.everypicfound.identity.infrastructure.security.password.adapter;

import com.everypicfound.identity.application.port.out.PasswordHasher;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.RawPassword;
import com.everypicfound.identity.support.exception.PasswordHashingException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 基于 Spring Security DelegatingPasswordEncoder 的密码适配器。
 */
@Component
public final class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(
                passwordEncoder,
                "passwordEncoder");
    }

    @Override
    public PasswordHash hash(RawPassword rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        try {
            return PasswordHash.of(
                    passwordEncoder.encode(rawPassword.value()));
        } catch (RuntimeException exception) {
            throw new PasswordHashingException(
                    "password hashing failed",
                    exception);
        }
    }

    @Override
    public boolean matches(
            RawPassword rawPassword,
            PasswordHash passwordHash) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        Objects.requireNonNull(passwordHash, "passwordHash");
        try {
            return passwordEncoder.matches(
                    rawPassword.value(),
                    passwordHash.value());
        } catch (RuntimeException exception) {
            throw new PasswordHashingException(
                    "password verification failed",
                    exception);
        }
    }
}
