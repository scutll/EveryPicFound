package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.everypicfound.identity.domain.model.token.UserRefreshToken;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserRefreshTokenPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserRefreshTokenMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserRefreshTokenPo;
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

    private MyBatisUserRefreshTokenRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MyBatisUserRefreshTokenRepository(
                mapper,
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
}
