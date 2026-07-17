package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.everypicfound.identity.domain.model.session.UserSession;
import com.everypicfound.identity.domain.repository.UserSessionRepository;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserSessionPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserSessionMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserSessionPo;
import org.springframework.stereotype.Repository;

import java.util.Objects;

/**
 * 基于 MyBatis-Plus 的用户会话仓储适配器。
 */
@Repository
public class MyBatisUserSessionRepository implements UserSessionRepository {

    private final UserSessionMapper mapper;
    private final UserSessionPersistenceConverter converter;

    public MyBatisUserSessionRepository(
            UserSessionMapper mapper,
            UserSessionPersistenceConverter converter) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.converter = Objects.requireNonNull(converter, "converter");
    }

    @Override
    public void save(UserSession session) {
        UserSessionPo po = converter.toPo(session);
        int affectedRows = mapper.insert(po);
        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "expected one inserted user session row");
        }
    }
}
