package com.everypicfound.interaction.security;

public final class InvalidAuthenticatedUserException
        extends RuntimeException {

    public InvalidAuthenticatedUserException() {
        super("authenticated user id is invalid");
    }

    public InvalidAuthenticatedUserException(Throwable cause) {
        super("authenticated user id is invalid", cause);
    }
}
