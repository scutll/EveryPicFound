package com.everypicfound.interaction.security;

import java.util.Objects;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public final class AuthenticatedUserIdResolver {

    public long resolve(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt must not be null");
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidAuthenticatedUserException();
        }
        try {
            long userId = Long.parseLong(subject);
            if (userId <= 0) {
                throw new InvalidAuthenticatedUserException();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new InvalidAuthenticatedUserException(exception);
        }
    }
}
