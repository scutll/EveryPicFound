package com.everypicfound.interaction.picturelike.domain.model;

import java.time.LocalDateTime;

public record PictureLike(
        long pictureId,
        long userId,
        LocalDateTime createdAt) {
}
