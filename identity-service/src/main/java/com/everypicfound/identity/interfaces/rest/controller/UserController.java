package com.everypicfound.identity.interfaces.rest.controller;

import com.everypicfound.identity.application.exception.InvalidAccessTokenException;
import com.everypicfound.identity.application.port.in.ChangeMyPasswordUseCase;
import com.everypicfound.identity.application.port.in.DeleteMyAccountUseCase;
import com.everypicfound.identity.application.port.in.GetCurrentUserUseCase;
import com.everypicfound.identity.application.port.in.UpdateMyProfileUseCase;
import com.everypicfound.identity.application.result.UserProfileResult;
import com.everypicfound.identity.interfaces.rest.request.ChangeMyPasswordRequest;
import com.everypicfound.identity.interfaces.rest.request.DeleteMyAccountRequest;
import com.everypicfound.identity.interfaces.rest.request.UpdateMyProfileRequest;
import com.everypicfound.identity.interfaces.rest.response.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 当前用户资料 HTTP 接口。
 */
@RestController
@RequestMapping("/api/users")
public final class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final ChangeMyPasswordUseCase changeMyPasswordUseCase;
    private final DeleteMyAccountUseCase deleteMyAccountUseCase;
    private final JwtDecoder jwtDecoder;

    public UserController(
            GetCurrentUserUseCase getCurrentUserUseCase,
            UpdateMyProfileUseCase updateMyProfileUseCase,
            ChangeMyPasswordUseCase changeMyPasswordUseCase,
            DeleteMyAccountUseCase deleteMyAccountUseCase,
            JwtDecoder jwtDecoder) {
        this.getCurrentUserUseCase = Objects.requireNonNull(
                getCurrentUserUseCase,
                "getCurrentUserUseCase");
        this.updateMyProfileUseCase = Objects.requireNonNull(
                updateMyProfileUseCase,
                "updateMyProfileUseCase");
        this.changeMyPasswordUseCase = Objects.requireNonNull(
                changeMyPasswordUseCase,
                "changeMyPasswordUseCase");
        this.deleteMyAccountUseCase = Objects.requireNonNull(
                deleteMyAccountUseCase,
                "deleteMyAccountUseCase");
        this.jwtDecoder = Objects.requireNonNull(jwtDecoder, "jwtDecoder");
    }

    @GetMapping("/me")
    public UserProfileResponse me(
            @RequestHeader(name = "Authorization", required = false)
            String authorizationHeader) {
        long userId = currentUserId(authorizationHeader);
        UserProfileResult result = getCurrentUserUseCase.getCurrentUser(
                userId);
        return UserProfileResponse.from(result);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(name = "Authorization", required = false)
            String authorizationHeader,
            @RequestBody ChangeMyPasswordRequest request) {
        long userId = currentUserId(authorizationHeader);
        changeMyPasswordUseCase.changeMyPassword(request.toCommand(userId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            @RequestHeader(name = "Authorization", required = false)
            String authorizationHeader,
            @RequestBody DeleteMyAccountRequest request) {
        long userId = currentUserId(authorizationHeader);
        deleteMyAccountUseCase.deleteMyAccount(request.toCommand(userId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/profile")
    public UserProfileResponse updateProfile(
            @RequestHeader(name = "Authorization", required = false)
            String authorizationHeader,
            @RequestBody UpdateMyProfileRequest request) {
        long userId = currentUserId(authorizationHeader);
        UserProfileResult result = updateMyProfileUseCase.updateMyProfile(
                request.toCommand(userId));
        return UserProfileResponse.from(result);
    }

    private long currentUserId(String authorizationHeader) {
        Jwt jwt = decodeBearerToken(authorizationHeader);
        return parseUserId(jwt.getSubject());
    }

    private Jwt decodeBearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidAccessTokenException();
        }
        String tokenValue = authorizationHeader.substring("Bearer ".length());
        if (tokenValue.isBlank()) {
            throw new InvalidAccessTokenException();
        }
        try {
            return jwtDecoder.decode(tokenValue);
        } catch (JwtException exception) {
            throw new InvalidAccessTokenException();
        }
    }

    private static long parseUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new InvalidAccessTokenException();
        }
        try {
            long userId = Long.parseLong(subject);
            if (userId <= 0) {
                throw new InvalidAccessTokenException();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new InvalidAccessTokenException();
        }
    }
}
