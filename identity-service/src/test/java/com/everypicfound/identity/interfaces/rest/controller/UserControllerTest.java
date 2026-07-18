package com.everypicfound.identity.interfaces.rest.controller;

import com.everypicfound.identity.application.command.UpdateMyProfileCommand;
import com.everypicfound.identity.application.port.in.GetCurrentUserUseCase;
import com.everypicfound.identity.application.port.in.UpdateMyProfileUseCase;
import com.everypicfound.identity.application.result.UserProfileResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @MockitoBean
    private UpdateMyProfileUseCase updateMyProfileUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void returnsCurrentUserProfile() throws Exception {
        when(jwtDecoder.decode("token-value"))
                .thenReturn(jwt("token-value", "42"));
        when(getCurrentUserUseCase.getCurrentUser(42L))
                .thenReturn(new UserProfileResult(
                        42L,
                        "User_01",
                        "探索者",
                        "探索者",
                        "https://cdn.example.com/avatar.png"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer token-value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42L))
                .andExpect(jsonPath("$.username").value("User_01"))
                .andExpect(jsonPath("$.nickname").value("探索者"))
                .andExpect(jsonPath("$.displayName").value("探索者"))
                .andExpect(jsonPath("$.avatarUrl")
                        .value("https://cdn.example.com/avatar.png"));
    }

    @Test
    void updatesCurrentUserProfile() throws Exception {
        when(jwtDecoder.decode("token-value"))
                .thenReturn(jwt("token-value", "42"));
        when(updateMyProfileUseCase.updateMyProfile(
                any(UpdateMyProfileCommand.class)))
                .thenReturn(new UserProfileResult(
                        42L,
                        "User_01",
                        "图友",
                        "图友",
                        "https://cdn.example.com/new.png"));

        mockMvc.perform(patch("/api/users/me/profile")
                        .header("Authorization", "Bearer token-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "  图友  ",
                                  "avatarUrl": "https://cdn.example.com/new.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("图友"))
                .andExpect(jsonPath("$.avatarUrl")
                        .value("https://cdn.example.com/new.png"));

        verify(updateMyProfileUseCase).updateMyProfile(
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.userId() == 42L
                                && command.nickname().equals("  图友  ")
                                && command.avatarUrl().equals(
                                "https://cdn.example.com/new.png")));
    }

    private static Jwt jwt(String tokenValue, String subject) {
        return new Jwt(
                tokenValue,
                Instant.parse("2026-07-18T03:00:00Z"),
                Instant.parse("2026-07-18T03:30:00Z"),
                Map.of("alg", "RS256"),
                Map.of("sub", subject, "sid", "session-123"));
    }
}
