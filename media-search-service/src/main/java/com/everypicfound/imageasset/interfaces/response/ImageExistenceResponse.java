package com.everypicfound.imageasset.interfaces.response;

public record ImageExistenceResponse(
        long pictureId,
        boolean exists) {
}
