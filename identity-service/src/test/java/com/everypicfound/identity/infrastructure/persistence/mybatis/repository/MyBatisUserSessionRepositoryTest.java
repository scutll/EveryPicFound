package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.everypicfound.identity.domain.model.session.UserSession;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserSessionPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserSessionMapper;
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
class MyBatisUserSessionRepositoryTest {

    @Mock
    private UserSessionMapper mapper;

    private MyBatisUserSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MyBatisUserSessionRepository(
                mapper,
                new UserSessionPersistenceConverter());
    }

    @Test
    void mapsAndInsertsUserSession() {
        when(mapper.insert(any(UserSessionPo.class))).thenReturn(1);
        UserSession session = UserSession.create(
                "session-123",
                42L,
                Instant.parse("2026-07-17T10:00:00.123Z"),
                Instant.parse("2026-07-18T10:00:00.123Z"));

        repository.save(session);

        ArgumentCaptor<UserSessionPo> captor =
                ArgumentCaptor.forClass(UserSessionPo.class);
        verify(mapper).insert(captor.capture());
        UserSessionPo po = captor.getValue();
        assertThat(po.getSessionId()).isEqualTo("session-123");
        assertThat(po.getUserId()).isEqualTo(42L);
        assertThat(po.getStatus()).isEqualTo("ACTIVE");
        assertThat(po.getCreatedTime()).isEqualTo(LocalDateTime.parse(
                "2026-07-17T10:00:00.123"));
        assertThat(po.getExpiresTime()).isEqualTo(LocalDateTime.parse(
                "2026-07-18T10:00:00.123"));
    }
}
