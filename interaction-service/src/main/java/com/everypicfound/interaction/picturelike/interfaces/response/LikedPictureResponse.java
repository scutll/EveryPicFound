package com.everypicfound.interaction.picturelike.interfaces.response;

import java.time.LocalDateTime;

public record LikedPictureResponse(
        long pictureId,
        LocalDateTime likedAt) {
}
