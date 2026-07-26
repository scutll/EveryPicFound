package com.everypicfound.interaction.picturelike.application.exception;

public final class InvalidPictureLikeCursorException
        extends RuntimeException {

    public InvalidPictureLikeCursorException() {
        super("cursorTime and cursorPictureId must be provided together");
    }
}
