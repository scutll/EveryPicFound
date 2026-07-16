package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.domain.model.user.UsernameAlreadyExistsException;
import com.everypicfound.identity.domain.repository.UserRepository;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserAccountPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserAccountMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserAccountPo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Objects;

/**
 * 基于 MyBatis-Plus 的用户账户仓储适配器。
 */
@Repository
public class MyBatisUserRepository implements UserRepository {

    private final UserAccountMapper mapper;
    private final UserAccountPersistenceConverter converter;

    public MyBatisUserRepository(
            UserAccountMapper mapper,
            UserAccountPersistenceConverter converter) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.converter = Objects.requireNonNull(converter, "converter");
    }

    @Override
    public boolean existsByUsername(Username username) {
        Objects.requireNonNull(username, "username");
        Long count = mapper.selectCount(
                Wrappers.<UserAccountPo>lambdaQuery()
                        .eq(UserAccountPo::getUsername, username.value()));
        return count > 0;
    }

    @Override
    public long save(UserAccount account) {
        UserAccountPo po = converter.toPo(account);
        try {
            int affectedRows = mapper.insert(po);
            if (affectedRows != 1) {
                throw new IllegalStateException(
                        "expected one inserted user account row");
            }
        } catch (DuplicateKeyException exception) {
            throw new UsernameAlreadyExistsException(exception);
        }

        if (po.getId() == null) {
            throw new IllegalStateException(
                    "generated user account id was not returned");
        }
        return po.getId();
    }
}
