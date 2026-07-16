package com.everypicfound.identity.infrastructure.security.jwt.key;

public final class JwtKeyConfigurationException extends RuntimeException {

    public JwtKeyConfigurationException(String message) {
        super(message);
    }

    public JwtKeyConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
