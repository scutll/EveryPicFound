package com.everypicfound.interaction.picturelike.application.exception;

public final class PictureNotFoundException extends RuntimeException {

    private final long pictureId;

    public PictureNotFoundException(long pictureId) {
        super("picture not found: " + pictureId);
        this.pictureId = pictureId;
    }

    public long pictureId() {
        return pictureId;
    }
}
