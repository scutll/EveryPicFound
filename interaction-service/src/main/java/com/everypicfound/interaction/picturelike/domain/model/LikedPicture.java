package com.everypicfound.interaction.picturelike.domain.model;

import java.time.LocalDateTime;

public record LikedPicture(
        long pictureId,
        LocalDateTime likedAt) {
}
