package com.everypicfound.interaction.picturelike.interfaces.response;

import java.time.LocalDateTime;
import java.util.List;

public record PictureLikeHistoryResponse(
        List<LikedPictureResponse> items,
        LocalDateTime nextCursorTime,
        Long nextCursorPictureId) {
}
