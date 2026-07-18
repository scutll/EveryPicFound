package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.everypicfound.identity.domain.model.token.RefreshTokenSession;
import com.everypicfound.identity.domain.model.token.UserRefreshToken;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserSessionMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserRefreshTokenPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserRefreshTokenMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserRefreshTokenPo;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserSessionPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyBatisUserRefreshTokenRepositoryTest {

    @Mock
    private UserRefreshTokenMapper mapper;

    @Mock
    private UserSessionMapper sessionMapper;

    private MyBatisUserRefreshTokenRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MyBatisUserRefreshTokenRepository(
                mapper,
                sessionMapper,
                new UserRefreshTokenPersistenceConverter());
    }

    @Test
    void mapsAndInsertsRefreshToken() {
        when(mapper.insert(any(UserRefreshTokenPo.class))).thenReturn(1);
        UserRefreshToken refreshToken = UserRefreshToken.issue(
                "session-123",
                "refresh-token-hash",
                Instant.parse("2026-07-17T10:00:00.123Z"),
                Instant.parse("2026-07-17T11:00:00.123Z"));

        repository.save(refreshToken);

        ArgumentCaptor<UserRefreshTokenPo> captor =
                ArgumentCaptor.forClass(UserRefreshTokenPo.class);
        verify(mapper).insert(captor.capture());
        UserRefreshTokenPo po = captor.getValue();
        assertThat(po.getSessionId()).isEqualTo("session-123");
        assertThat(po.getTokenHash()).isEqualTo("refresh-token-hash");
        assertThat(po.getStatus()).isEqualTo("ACTIVE");
        assertThat(po.getIssuedTime()).isEqualTo(LocalDateTime.parse(
                "2026-07-17T10:00:00.123"));
        assertThat(po.getExpiresTime()).isEqualTo(LocalDateTime.parse(
                "2026-07-17T11:00:00.123"));
    }

    @Test
    void findsRefreshTokenSessionViewByTokenHash() {
        UserRefreshTokenPo refreshTokenPo = new UserRefreshTokenPo();
        refreshTokenPo.setTokenHash("refresh-token-hash");
        refreshTokenPo.setSessionId("session-123");
        refreshTokenPo.setStatus("ACTIVE");
        refreshTokenPo.setExpiresTime(LocalDateTime.parse(
                "2026-07-17T11:00:00.123"));
        UserSessionPo sessionPo = new UserSessionPo();
        sessionPo.setSessionId("session-123");
        sessionPo.setUserId(42L);
        sessionPo.setStatus("ACTIVE");
        sessionPo.setCreatedTime(LocalDateTime.parse(
                "2026-07-17T10:00:00.123"));
        sessionPo.setExpiresTime(LocalDateTime.parse(
                "2026-07-18T10:00:00.123"));
        when(mapper.selectById("refresh-token-hash"))
                .thenReturn(refreshTokenPo);
        when(sessionMapper.selectById("session-123"))
                .thenReturn(sessionPo);

        RefreshTokenSession session = repository
                .findSessionByTokenHash("refresh-token-hash")
                .orElseThrow();

        assertThat(session.tokenHash()).isEqualTo("refresh-token-hash");
        assertThat(session.tokenStatus().name()).isEqualTo("ACTIVE");
        assertThat(session.sessionId()).isEqualTo("session-123");
        assertThat(session.userId()).isEqualTo(42L);
        assertThat(session.sessionStatus().name()).isEqualTo("ACTIVE");
        assertThat(session.sessionCreatedAt()).isEqualTo(Instant.parse(
                "2026-07-17T10:00:00.123Z"));
        assertThat(session.toString())
                .contains("tokenHash=PROTECTED")
                .doesNotContain("refresh-token-hash");
    }

    @Test
    void marksOnlyActiveTokenAsUsed() {
        when(mapper.update(any(), any())).thenReturn(1, 0);

        assertThat(repository.markActiveTokenAsUsed(
                "refresh-token-hash",
                Instant.parse("2026-07-17T10:40:00.123Z")))
                .isTrue();
        assertThat(repository.markActiveTokenAsUsed(
                "refresh-token-hash",
                Instant.parse("2026-07-17T10:40:00.123Z")))
                .isFalse();
    }

    @Test
    void revokesActiveRefreshTokensBySessionId() {
        when(mapper.update(any(), any())).thenReturn(2);

        int affectedRows = repository.revokeActiveTokensBySessionId(
                "session-123",
                Instant.parse("2026-07-18T02:00:00.123Z"));

        assertThat(affectedRows).isEqualTo(2);
    }
}
