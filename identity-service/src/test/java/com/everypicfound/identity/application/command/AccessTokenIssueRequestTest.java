package com.everypicfound.identity.application.command;

import com.everypicfound.identity.application.exception.InvalidAccessTokenIssueRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessTokenIssueRequestTest {

    private static final Instant AUTH_TIME =
            Instant.parse("2026-07-16T12:00:00Z");

    @Test
    void normalizesScopesIntoStableDistinctOrder() {
        AccessTokenIssueRequest request = new AccessTokenIssueRequest(
                123L,
                "session-001",
                List.of("image:search", "image:read", "image:search"),
                AUTH_TIME);

        assertThat(request.userId()).isEqualTo(123L);
        assertThat(request.sessionId()).isEqualTo("session-001");
        assertThat(request.scopes()).containsExactly(
                "image:read",
                "image:search");
        assertThat(request.authTime()).isEqualTo(AUTH_TIME);
    }

    @Test
    void rejectsNonPositiveUserId() {
        assertThatThrownBy(() -> request(0L, "session-001",
                List.of("image:read"), AUTH_TIME))
                .isInstanceOf(InvalidAccessTokenIssueRequestException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void rejectsBlankSessionId() {
        assertThatThrownBy(() -> request(1L, " ",
                List.of("image:read"), AUTH_TIME))
                .isInstanceOf(InvalidAccessTokenIssueRequestException.class)
                .hasMessageContaining("sessionId");
    }

    @Test
    void rejectsEmptyScopes() {
        assertThatThrownBy(() -> request(1L, "session-001",
                List.of(), AUTH_TIME))
                .isInstanceOf(InvalidAccessTokenIssueRequestException.class)
                .hasMessageContaining("scopes");
    }

    @Test
    void rejectsScopeContainingWhitespace() {
        assertThatThrownBy(() -> request(1L, "session-001",
                List.of("image:read image:search"), AUTH_TIME))
                .isInstanceOf(InvalidAccessTokenIssueRequestException.class)
                .hasMessageContaining("scope");
    }

    @Test
    void rejectsMissingAuthTime() {
        assertThatThrownBy(() -> request(1L, "session-001",
                List.of("image:read"), null))
                .isInstanceOf(InvalidAccessTokenIssueRequestException.class)
                .hasMessageContaining("authTime");
    }

    @Test
    void protectsSessionIdInDiagnosticText() {
        AccessTokenIssueRequest request = request(
                123L,
                "sensitive-session-001",
                List.of("image:read"),
                AUTH_TIME);

        assertThat(request.toString())
                .contains("sessionId=PROTECTED")
                .doesNotContain("sensitive-session-001");
    }

    private static AccessTokenIssueRequest request(
            long userId,
            String sessionId,
            List<String> scopes,
            Instant authTime) {
        return new AccessTokenIssueRequest(
                userId,
                sessionId,
                scopes,
                authTime);
    }
}
