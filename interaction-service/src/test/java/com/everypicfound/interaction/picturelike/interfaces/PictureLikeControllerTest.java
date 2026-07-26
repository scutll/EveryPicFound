package com.everypicfound.interaction.picturelike.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everypicfound.interaction.picturelike.application.PictureLikeApplicationService;
import com.everypicfound.interaction.picturelike.domain.model.LikedPicture;
import com.everypicfound.interaction.picturelike.domain.model.PictureLike;
import com.everypicfound.interaction.picturelike.domain.model.PictureLikeInsertResult;
import com.everypicfound.interaction.picturelike.interfaces.response.PictureLikeCountResponse;
import com.everypicfound.interaction.picturelike.interfaces.response.PictureLikeHistoryResponse;
import com.everypicfound.interaction.picturelike.interfaces.response.PictureLikerListResponse;
import com.everypicfound.interaction.security.AuthenticatedUserIdResolver;
import com.everypicfound.interaction.support.web.InteractionApiResponse;
import com.everypicfound.interaction.support.web.InteractionRequestIdFilter;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class PictureLikeControllerTest {

    @Mock
    private PictureLikeApplicationService applicationService;

    private PictureLikeController controller;

    @BeforeEach
    void setUp() {
        controller = new PictureLikeController(
                applicationService,
                new AuthenticatedUserIdResolver());
    }

    @Test
    void duplicateLikeStillReturnsNoContent() {
        when(applicationService.like(7L, 42L))
                .thenReturn(
                        PictureLikeInsertResult.ALREADY_LIKED);

        assertThat(controller.like(7L, jwt(42L))
                .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(applicationService).like(7L, 42L);
    }

    @Test
    void unlikeUsesAuthenticatedUserAndReturnsNoContent() {
        assertThat(controller.unlike(7L, jwt(42L))
                .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(applicationService).unlike(7L, 42L);
    }

    @Test
    void countUsesStableEnvelopeAndRequestId() {
        when(applicationService.count(7L)).thenReturn(12L);

        InteractionApiResponse<PictureLikeCountResponse> response =
                controller.count(
                        7L,
                        jwt(42L),
                        request("count-request"));

        assertThat(response.code()).isZero();
        assertThat(response.requestId())
                .isEqualTo("count-request");
        assertThat(response.data())
                .isEqualTo(
                        new PictureLikeCountResponse(7L, 12L));
    }

    @Test
    void likersPreserveRepositoryOrder() {
        LocalDateTime firstTime =
                LocalDateTime.of(2026, 7, 24, 12, 0);
        LocalDateTime secondTime =
                firstTime.minusSeconds(1);
        when(applicationService.findRecentLikers(7L, 20))
                .thenReturn(List.of(
                        new PictureLike(7L, 9L, firstTime),
                        new PictureLike(7L, 8L, secondTime)));

        InteractionApiResponse<PictureLikerListResponse> response =
                controller.likers(
                        7L,
                        20,
                        jwt(42L),
                        request("likers-request"));

        assertThat(response.data().items())
                .extracting(item -> item.userId())
                .containsExactly(9L, 8L);
    }

    @Test
    void fullHistoryPageReturnsLastItemAsNextCursor() {
        LocalDateTime firstTime =
                LocalDateTime.of(2026, 7, 24, 12, 0);
        LocalDateTime secondTime =
                firstTime.minusSeconds(1);
        when(applicationService.findRecentLikedPictures(
                42L,
                null,
                null,
                2))
                .thenReturn(List.of(
                        new LikedPicture(9L, firstTime),
                        new LikedPicture(7L, secondTime)));

        InteractionApiResponse<PictureLikeHistoryResponse> response =
                controller.likedPictures(
                        null,
                        null,
                        2,
                        jwt(42L),
                        request("history-request"));

        assertThat(response.data().items())
                .extracting(item -> item.pictureId())
                .containsExactly(9L, 7L);
        assertThat(response.data().nextCursorTime())
                .isEqualTo(secondTime);
        assertThat(response.data().nextCursorPictureId())
                .isEqualTo(7L);
    }

    private Jwt jwt(long userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(String.valueOf(userId))
                .build();
    }

    private MockHttpServletRequest request(String requestId) {
        MockHttpServletRequest request =
                new MockHttpServletRequest();
        request.setAttribute(
                InteractionRequestIdFilter.REQUEST_ATTRIBUTE,
                requestId);
        return request;
    }
}
