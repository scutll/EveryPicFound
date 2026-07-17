package com.everypicfound.security.contract.jwt;

public final class JwtKeyLoadingException extends RuntimeException {

    public JwtKeyLoadingException(String message) {
        super(message);
    }

    public JwtKeyLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
