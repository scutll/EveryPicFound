package com.everypicfound.interaction.support.web;

import com.everypicfound.interaction.picturelike.application.exception.InvalidPictureLikeCursorException;
import com.everypicfound.interaction.picturelike.application.exception.PictureNotFoundException;
import com.everypicfound.interaction.picturelike.infrastructure.client.media.MediaServiceUnavailableException;
import com.everypicfound.interaction.security.InvalidAuthenticatedUserException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public final class InteractionExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            InteractionExceptionHandler.class);

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class
    })
    public ResponseEntity<InteractionApiResponse<Void>>
            handleInvalidRequest(
                    Exception exception,
                    HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "request validation failed",
                request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<InteractionApiResponse<Void>>
            handleMethodNotAllowed(
                    HttpRequestMethodNotSupportedException exception,
                    HttpServletRequest request) {
        return response(
                HttpStatus.METHOD_NOT_ALLOWED,
                "method not allowed",
                request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<InteractionApiResponse<Void>>
            handleResourceNotFound(
                    NoResourceFoundException exception,
                    HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "resource not found",
                request);
    }

    @ExceptionHandler(InvalidPictureLikeCursorException.class)
    public ResponseEntity<InteractionApiResponse<Void>>
            handleInvalidCursor(
                    InvalidPictureLikeCursorException exception,
                    HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(InvalidAuthenticatedUserException.class)
    public ResponseEntity<InteractionApiResponse<Void>>
            handleInvalidAuthenticatedUser(
                    InvalidAuthenticatedUserException exception,
                    HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "authenticated user is invalid",
                request);
    }

    @ExceptionHandler(PictureNotFoundException.class)
    public ResponseEntity<InteractionApiResponse<Void>>
            handlePictureNotFound(
                    PictureNotFoundException exception,
                    HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "picture not found",
                request);
    }

    @ExceptionHandler(MediaServiceUnavailableException.class)
    public ResponseEntity<InteractionApiResponse<Void>>
            handleMediaServiceUnavailable(
                    MediaServiceUnavailableException exception,
                    HttpServletRequest request) {
        LOGGER.warn(
                "Media service failed during picture validation",
                exception);
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "media service unavailable",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<InteractionApiResponse<Void>>
            handleUnexpectedFailure(
                    Exception exception,
                    HttpServletRequest request) {
        LOGGER.error(
                "Interaction request failed with an unexpected exception",
                exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal server error",
                request);
    }

    private ResponseEntity<InteractionApiResponse<Void>> response(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        return ResponseEntity
                .status(status)
                .body(InteractionApiResponse.failure(
                        status.value(),
                        message,
                        InteractionRequestIdFilter.current(request)));
    }
}
