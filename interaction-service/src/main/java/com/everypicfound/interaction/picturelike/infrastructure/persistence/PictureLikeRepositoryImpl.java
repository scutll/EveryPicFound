package com.everypicfound.interaction.picturelike.infrastructure.persistence;

import com.everypicfound.interaction.picturelike.domain.model.LikedPicture;
import com.everypicfound.interaction.picturelike.domain.model.PictureLike;
import com.everypicfound.interaction.picturelike.domain.model.PictureLikeInsertResult;
import com.everypicfound.interaction.picturelike.domain.repository.PictureLikeRepository;
import com.everypicfound.interaction.picturelike.infrastructure.persistence.mapper.PictureLikeMapper;
import com.everypicfound.interaction.picturelike.infrastructure.persistence.po.LikedPictureRow;
import com.everypicfound.interaction.picturelike.infrastructure.persistence.po.PictureLikeRow;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public final class PictureLikeRepositoryImpl
        implements PictureLikeRepository {

    private final PictureLikeMapper pictureLikeMapper;

    public PictureLikeRepositoryImpl(
            PictureLikeMapper pictureLikeMapper) {
        this.pictureLikeMapper = pictureLikeMapper;
    }

    @Override
    public PictureLikeInsertResult insert(
            long pictureId,
            long userId) {
        try {
            int affectedRows = pictureLikeMapper.insert(
                    pictureId,
                    userId);
            if (affectedRows != 1) {
                throw new IllegalStateException(
                        "picture like insert affected "
                                + affectedRows
                                + " rows");
            }
            return PictureLikeInsertResult.INSERTED;
        } catch (DuplicateKeyException exception) {
            return PictureLikeInsertResult.ALREADY_LIKED;
        }
    }

    @Override
    public int delete(long pictureId, long userId) {
        return pictureLikeMapper.delete(pictureId, userId);
    }

    @Override
    public long countByPictureId(long pictureId) {
        return pictureLikeMapper.countByPictureId(pictureId);
    }

    @Override
    public List<PictureLike> findRecentLikers(
            long pictureId,
            int limit) {
        return toDomainList(
                pictureLikeMapper.findRecentLikers(
                        pictureId,
                        limit));
    }

    @Override
    public List<LikedPicture> findRecentLikedPictures(
            long userId,
            LocalDateTime cursorTime,
            Long cursorPictureId,
            int limit) {
        List<LikedPictureRow> rows =
                pictureLikeMapper.findRecentLikedPictures(
                        userId,
                        cursorTime,
                        cursorPictureId,
                        limit);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> new LikedPicture(
                        row.getPictureId(),
                        row.getLikedAt()))
                .toList();
    }

    private List<PictureLike> toDomainList(
            List<PictureLikeRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> new PictureLike(
                        row.getPictureId(),
                        row.getUserId(),
                        row.getCreatedAt()))
                .toList();
    }
}
