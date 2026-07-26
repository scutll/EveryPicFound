package com.everypicfound.interaction.picturelike.interfaces.response;

import java.time.LocalDateTime;

public record PictureLikerResponse(
        long pictureId,
        long userId,
        LocalDateTime likedAt) {
}
