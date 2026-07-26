package com.everypicfound.interaction.picturelike.application;

import com.everypicfound.interaction.picturelike.application.exception.InvalidPictureLikeCursorException;
import com.everypicfound.interaction.picturelike.application.exception.PictureNotFoundException;
import com.everypicfound.interaction.picturelike.application.port.PictureExistencePort;
import com.everypicfound.interaction.picturelike.domain.model.LikedPicture;
import com.everypicfound.interaction.picturelike.domain.model.PictureLike;
import com.everypicfound.interaction.picturelike.domain.model.PictureLikeInsertResult;
import com.everypicfound.interaction.picturelike.domain.repository.PictureLikeRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public final class PictureLikeApplicationService {

    private final PictureExistencePort pictureExistencePort;
    private final PictureLikeRepository pictureLikeRepository;

    public PictureLikeApplicationService(
            PictureExistencePort pictureExistencePort,
            PictureLikeRepository pictureLikeRepository) {
        this.pictureExistencePort = pictureExistencePort;
        this.pictureLikeRepository = pictureLikeRepository;
    }

    public PictureLikeInsertResult like(
            long pictureId,
            long userId) {
        requirePictureExists(pictureId);
        return pictureLikeRepository.insert(pictureId, userId);
    }

    public void unlike(long pictureId, long userId) {
        requirePictureExists(pictureId);
        pictureLikeRepository.delete(pictureId, userId);
    }

    public long count(long pictureId) {
        requirePictureExists(pictureId);
        return pictureLikeRepository.countByPictureId(pictureId);
    }

    public List<PictureLike> findRecentLikers(
            long pictureId,
            int limit) {
        requirePictureExists(pictureId);
        return pictureLikeRepository.findRecentLikers(
                pictureId,
                limit);
    }

    public List<LikedPicture> findRecentLikedPictures(
            long userId,
            LocalDateTime cursorTime,
            Long cursorPictureId,
            int limit) {
        validateCursor(cursorTime, cursorPictureId);
        return pictureLikeRepository.findRecentLikedPictures(
                userId,
                cursorTime,
                cursorPictureId,
                limit);
    }

    private void requirePictureExists(long pictureId) {
        if (!pictureExistencePort.exists(pictureId)) {
            throw new PictureNotFoundException(pictureId);
        }
    }

    private void validateCursor(
            LocalDateTime cursorTime,
            Long cursorPictureId) {
        if ((cursorTime == null) != (cursorPictureId == null)) {
            throw new InvalidPictureLikeCursorException();
        }
    }
}
