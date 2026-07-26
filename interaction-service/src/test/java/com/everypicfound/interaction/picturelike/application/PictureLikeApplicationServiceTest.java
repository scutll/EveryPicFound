package com.everypicfound.interaction.picturelike.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everypicfound.interaction.picturelike.application.exception.InvalidPictureLikeCursorException;
import com.everypicfound.interaction.picturelike.application.exception.PictureNotFoundException;
import com.everypicfound.interaction.picturelike.application.port.PictureExistencePort;
import com.everypicfound.interaction.picturelike.domain.model.LikedPicture;
import com.everypicfound.interaction.picturelike.domain.model.PictureLikeInsertResult;
import com.everypicfound.interaction.picturelike.domain.repository.PictureLikeRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PictureLikeApplicationServiceTest {

    @Mock
    private PictureExistencePort pictureExistencePort;
    @Mock
    private PictureLikeRepository pictureLikeRepository;

    private PictureLikeApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PictureLikeApplicationService(
                pictureExistencePort,
                pictureLikeRepository);
    }

    @Test
    void likeValidatesPictureBeforeInsert() {
        when(pictureExistencePort.exists(7L)).thenReturn(true);
        when(pictureLikeRepository.insert(7L, 42L))
                .thenReturn(PictureLikeInsertResult.INSERTED);

        PictureLikeInsertResult result = service.like(7L, 42L);

        assertThat(result)
                .isEqualTo(PictureLikeInsertResult.INSERTED);
        verify(pictureExistencePort).exists(7L);
        verify(pictureLikeRepository).insert(7L, 42L);
    }

    @Test
    void missingPictureStopsBeforeRepository() {
        when(pictureExistencePort.exists(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.like(7L, 42L))
                .isInstanceOf(PictureNotFoundException.class);

        verify(pictureLikeRepository, never())
                .insert(7L, 42L);
    }

    @Test
    void unlikeTreatsZeroDeletedRowsAsSuccess() {
        when(pictureExistencePort.exists(7L)).thenReturn(true);
        when(pictureLikeRepository.delete(7L, 42L))
                .thenReturn(0);

        service.unlike(7L, 42L);

        verify(pictureLikeRepository).delete(7L, 42L);
    }

    @Test
    void historyDoesNotPerformPerPictureExistenceChecks() {
        LocalDateTime cursorTime =
                LocalDateTime.of(2026, 7, 24, 12, 0);
        List<LikedPicture> expected = List.of(
                new LikedPicture(7L, cursorTime.minusSeconds(1)));
        when(pictureLikeRepository.findRecentLikedPictures(
                42L,
                cursorTime,
                7L,
                20))
                .thenReturn(expected);

        assertThat(service.findRecentLikedPictures(
                42L,
                cursorTime,
                7L,
                20))
                .isEqualTo(expected);
        verify(pictureExistencePort, never())
                .exists(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void halfCursorIsRejectedBeforeRepository() {
        LocalDateTime cursorTime =
                LocalDateTime.of(2026, 7, 24, 12, 0);

        assertThatThrownBy(() ->
                service.findRecentLikedPictures(
                        42L,
                        cursorTime,
                        null,
                        20))
                .isInstanceOf(
                        InvalidPictureLikeCursorException.class);
        assertThatThrownBy(() ->
                service.findRecentLikedPictures(
                        42L,
                        null,
                        7L,
                        20))
                .isInstanceOf(
                        InvalidPictureLikeCursorException.class);

        verify(pictureLikeRepository, never())
                .findRecentLikedPictures(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt());
    }
}
