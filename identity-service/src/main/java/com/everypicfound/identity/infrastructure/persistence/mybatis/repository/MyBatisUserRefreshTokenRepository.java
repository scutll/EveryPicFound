package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.everypicfound.identity.domain.model.token.UserRefreshToken;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserRefreshTokenPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserRefreshTokenMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserRefreshTokenPo;
import org.springframework.stereotype.Repository;

import java.util.Objects;

/**
 * 基于 MyBatis-Plus 的 Refresh Token 仓储适配器。
 */
@Repository
public class MyBatisUserRefreshTokenRepository
        implements UserRefreshTokenRepository {

    private final UserRefreshTokenMapper mapper;
    private final UserRefreshTokenPersistenceConverter converter;

    public MyBatisUserRefreshTokenRepository(
            UserRefreshTokenMapper mapper,
            UserRefreshTokenPersistenceConverter converter) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.converter = Objects.requireNonNull(converter, "converter");
    }

    @Override
    public void save(UserRefreshToken refreshToken) {
        UserRefreshTokenPo po = converter.toPo(refreshToken);
        int affectedRows = mapper.insert(po);
        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "expected one inserted refresh token row");
        }
    }
}
