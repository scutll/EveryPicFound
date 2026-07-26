package com.everypicfound.interaction.picturelike.domain.repository;

import com.everypicfound.interaction.picturelike.domain.model.LikedPicture;
import com.everypicfound.interaction.picturelike.domain.model.PictureLike;
import com.everypicfound.interaction.picturelike.domain.model.PictureLikeInsertResult;
import java.time.LocalDateTime;
import java.util.List;

public interface PictureLikeRepository {

    PictureLikeInsertResult insert(long pictureId, long userId);

    int delete(long pictureId, long userId);

    long countByPictureId(long pictureId);

    List<PictureLike> findRecentLikers(long pictureId, int limit);

    List<LikedPicture> findRecentLikedPictures(
            long userId,
            LocalDateTime cursorTime,
            Long cursorPictureId,
            int limit);
}
