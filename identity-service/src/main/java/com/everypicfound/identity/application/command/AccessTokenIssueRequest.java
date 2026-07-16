package com.everypicfound.identity.application.command;

import com.everypicfound.identity.application.exception.InvalidAccessTokenIssueRequestException;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * 应用服务向 Token 签发端口提交的内部请求。
 */
public final class AccessTokenIssueRequest {

    private final long userId;
    private final String sessionId;
    private final List<String> scopes;
    private final Instant authTime;

    public AccessTokenIssueRequest(
            long userId,
            String sessionId,
            Collection<String> scopes,
            Instant authTime) {
        if (userId <= 0) {
            throw invalid("userId must be positive");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw invalid("sessionId must not be blank");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw invalid("scopes must not be empty");
        }

        TreeSet<String> normalizedScopes = new TreeSet<>();
        for (String scope : scopes) {
            if (scope == null || scope.isBlank()
                    || scope.codePoints().anyMatch(Character::isWhitespace)) {
                throw invalid("scope must be a non-blank token");
            }
            normalizedScopes.add(scope);
        }
        if (authTime == null) {
            throw invalid("authTime must not be null");
        }

        this.userId = userId;
        this.sessionId = sessionId;
        this.scopes = List.copyOf(normalizedScopes);
        this.authTime = authTime;
    }

    public long userId() {
        return userId;
    }

    public String sessionId() {
        return sessionId;
    }

    public List<String> scopes() {
        return scopes;
    }

    public Instant authTime() {
        return authTime;
    }

    @Override
    public String toString() {
        return "AccessTokenIssueRequest[userId=" + userId
                + ", sessionId=PROTECTED, scopes=" + scopes
                + ", authTime=" + authTime + "]";
    }

    private static InvalidAccessTokenIssueRequestException invalid(
            String message) {
        return new InvalidAccessTokenIssueRequestException(message);
    }
}
