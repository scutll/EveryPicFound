package com.everypicfound.interaction.picturelike.interfaces;

import com.everypicfound.interaction.picturelike.application.PictureLikeApplicationService;
import com.everypicfound.interaction.picturelike.domain.model.LikedPicture;
import com.everypicfound.interaction.picturelike.interfaces.response.LikedPictureResponse;
import com.everypicfound.interaction.picturelike.interfaces.response.PictureLikeCountResponse;
import com.everypicfound.interaction.picturelike.interfaces.response.PictureLikeHistoryResponse;
import com.everypicfound.interaction.picturelike.interfaces.response.PictureLikerListResponse;
import com.everypicfound.interaction.picturelike.interfaces.response.PictureLikerResponse;
import com.everypicfound.interaction.security.AuthenticatedUserIdResolver;
import com.everypicfound.interaction.support.web.InteractionApiResponse;
import com.everypicfound.interaction.support.web.InteractionRequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/interactions")
public class PictureLikeController {

    private static final String DEFAULT_LIMIT = "20";
    private static final int MAX_LIMIT = 100;

    private final PictureLikeApplicationService applicationService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public PictureLikeController(
            PictureLikeApplicationService applicationService,
            AuthenticatedUserIdResolver userIdResolver) {
        this.applicationService = applicationService;
        this.userIdResolver = userIdResolver;
    }

    @PostMapping("/pictures/{pictureId}/likes")
    public ResponseEntity<Void> like(
            @PathVariable @Positive long pictureId,
            @AuthenticationPrincipal Jwt jwt) {
        applicationService.like(
                pictureId,
                userIdResolver.resolve(jwt));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pictures/{pictureId}/likes")
    public ResponseEntity<Void> unlike(
            @PathVariable @Positive long pictureId,
            @AuthenticationPrincipal Jwt jwt) {
        applicationService.unlike(
                pictureId,
                userIdResolver.resolve(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pictures/{pictureId}/likes/count")
    public InteractionApiResponse<PictureLikeCountResponse> count(
            @PathVariable @Positive long pictureId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        userIdResolver.resolve(jwt);
        long count = applicationService.count(pictureId);
        return success(
                new PictureLikeCountResponse(pictureId, count),
                request);
    }

    @GetMapping("/pictures/{pictureId}/likers")
    public InteractionApiResponse<PictureLikerListResponse> likers(
            @PathVariable @Positive long pictureId,
            @RequestParam(defaultValue = DEFAULT_LIMIT)
            @Min(1) @Max(MAX_LIMIT) int limit,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        userIdResolver.resolve(jwt);
        List<PictureLikerResponse> items = applicationService
                .findRecentLikers(pictureId, limit)
                .stream()
                .map(like -> new PictureLikerResponse(
                        like.pictureId(),
                        like.userId(),
                        like.createdAt()))
                .toList();
        return success(
                new PictureLikerListResponse(items),
                request);
    }

    @GetMapping("/me/picture-likes")
    public InteractionApiResponse<PictureLikeHistoryResponse>
            likedPictures(
                    @RequestParam(required = false)
                    @DateTimeFormat(
                            iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime cursorTime,
                    @RequestParam(required = false)
                    @Positive Long cursorPictureId,
                    @RequestParam(defaultValue = DEFAULT_LIMIT)
                    @Min(1) @Max(MAX_LIMIT) int limit,
                    @AuthenticationPrincipal Jwt jwt,
                    HttpServletRequest request) {
        long userId = userIdResolver.resolve(jwt);
        List<LikedPicture> likes =
                applicationService.findRecentLikedPictures(
                        userId,
                        cursorTime,
                        cursorPictureId,
                        limit);
        List<LikedPictureResponse> items = likes.stream()
                .map(like -> new LikedPictureResponse(
                        like.pictureId(),
                        like.likedAt()))
                .toList();
        LikedPicture next = likes.size() == limit
                ? likes.get(likes.size() - 1)
                : null;
        return success(
                new PictureLikeHistoryResponse(
                        items,
                        next == null ? null : next.likedAt(),
                        next == null ? null : next.pictureId()),
                request);
    }

    private <T> InteractionApiResponse<T> success(
            T data,
            HttpServletRequest request) {
        return InteractionApiResponse.success(
                data,
                InteractionRequestIdFilter.current(request));
    }
}
