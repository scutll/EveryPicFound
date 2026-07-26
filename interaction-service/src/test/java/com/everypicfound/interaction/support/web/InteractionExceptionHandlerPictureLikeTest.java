package com.everypicfound.interaction.support.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.everypicfound.interaction.picturelike.application.exception.InvalidPictureLikeCursorException;
import com.everypicfound.interaction.picturelike.application.exception.PictureNotFoundException;
import com.everypicfound.interaction.picturelike.infrastructure.client.media.MediaServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class InteractionExceptionHandlerPictureLikeTest {

    private final InteractionExceptionHandler handler =
            new InteractionExceptionHandler();

    @Test
    void pictureNotFoundMapsTo404() {
        ResponseEntity<InteractionApiResponse<Void>> response =
                handler.handlePictureNotFound(
                        new PictureNotFoundException(7L),
                        request("not-found-request"));

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo(404);
        assertThat(response.getBody().message())
                .isEqualTo("picture not found");
        assertThat(response.getBody().requestId())
                .isEqualTo("not-found-request");
    }

    @Test
    void mediaFailureMapsTo503() {
        ResponseEntity<InteractionApiResponse<Void>> response =
                handler.handleMediaServiceUnavailable(
                        new MediaServiceUnavailableException(
                                "upstream failed"),
                        request("upstream-request"));

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code()).isEqualTo(503);
    }

    @Test
    void halfCursorMapsTo400() {
        ResponseEntity<InteractionApiResponse<Void>> response =
                handler.handleInvalidCursor(
                        new InvalidPictureLikeCursorException(),
                        request("cursor-request"));

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message())
                .isEqualTo(
                        "cursorTime and cursorPictureId"
                                + " must be provided together");
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
