package com.everypicfound.interaction.picturelike.infrastructure.client.media;

public final class MediaServiceUnavailableException
        extends RuntimeException {

    public MediaServiceUnavailableException(String message) {
        super(message);
    }

    public MediaServiceUnavailableException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
