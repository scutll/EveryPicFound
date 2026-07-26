package com.everypicfound.interaction.picturelike.infrastructure.persistence.mapper;

import com.everypicfound.interaction.picturelike.infrastructure.persistence.po.LikedPictureRow;
import com.everypicfound.interaction.picturelike.infrastructure.persistence.po.PictureLikeRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PictureLikeMapper {

    int insert(
            @Param("pictureId") long pictureId,
            @Param("userId") long userId);

    int delete(
            @Param("pictureId") long pictureId,
            @Param("userId") long userId);

    long countByPictureId(
            @Param("pictureId") long pictureId);

    List<PictureLikeRow> findRecentLikers(
            @Param("pictureId") long pictureId,
            @Param("limit") int limit);

    List<LikedPictureRow> findRecentLikedPictures(
            @Param("userId") long userId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorPictureId") Long cursorPictureId,
            @Param("limit") int limit);
}
