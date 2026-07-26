package com.everypicfound.interaction.picturelike.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.everypicfound.interaction.picturelike.domain.model.LikedPicture;
import com.everypicfound.interaction.picturelike.domain.model.PictureLike;
import com.everypicfound.interaction.picturelike.domain.model.PictureLikeInsertResult;
import com.everypicfound.interaction.picturelike.infrastructure.persistence.mapper.PictureLikeMapper;
import com.everypicfound.interaction.picturelike.infrastructure.persistence.po.LikedPictureRow;
import com.everypicfound.interaction.picturelike.infrastructure.persistence.po.PictureLikeRow;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class PictureLikeRepositoryImplTest {

    @Mock
    private PictureLikeMapper mapper;

    private PictureLikeRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PictureLikeRepositoryImpl(mapper);
    }

    @Test
    void ordinaryInsertReturnsInserted() {
        when(mapper.insert(7L, 42L)).thenReturn(1);

        assertThat(repository.insert(7L, 42L))
                .isEqualTo(PictureLikeInsertResult.INSERTED);
    }

    @Test
    void duplicateKeyIsConvertedToIdempotentResult() {
        when(mapper.insert(7L, 42L))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThat(repository.insert(7L, 42L))
                .isEqualTo(
                        PictureLikeInsertResult.ALREADY_LIKED);
    }

    @Test
    void unexpectedInsertRowCountIsNotIdempotentSuccess() {
        when(mapper.insert(7L, 42L)).thenReturn(0);

        assertThatThrownBy(() -> repository.insert(7L, 42L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recentLikersAreMappedWithoutChangingOrder() {
        LocalDateTime newer =
                LocalDateTime.of(2026, 7, 24, 12, 0, 1);
        LocalDateTime older = newer.minusSeconds(1);
        PictureLikeRow first = pictureLikeRow(7L, 9L, newer);
        PictureLikeRow second = pictureLikeRow(7L, 8L, older);
        when(mapper.findRecentLikers(7L, 20))
                .thenReturn(List.of(first, second));

        assertThat(repository.findRecentLikers(7L, 20))
                .containsExactly(
                        new PictureLike(7L, 9L, newer),
                        new PictureLike(7L, 8L, older));
    }

    @Test
    void likedPictureHistoryMapsLikedAtAlias() {
        LocalDateTime likedAt =
                LocalDateTime.of(2026, 7, 24, 12, 0);
        LikedPictureRow row = new LikedPictureRow();
        row.setPictureId(7L);
        row.setLikedAt(likedAt);
        when(mapper.findRecentLikedPictures(
                42L,
                null,
                null,
                20))
                .thenReturn(List.of(row));

        assertThat(repository.findRecentLikedPictures(
                42L,
                null,
                null,
                20))
                .containsExactly(new LikedPicture(7L, likedAt));
    }

    private PictureLikeRow pictureLikeRow(
            long pictureId,
            long userId,
            LocalDateTime createdAt) {
        PictureLikeRow row = new PictureLikeRow();
        row.setPictureId(pictureId);
        row.setUserId(userId);
        row.setCreatedAt(createdAt);
        return row;
    }
}
