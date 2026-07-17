package com.everypicfound.identity.infrastructure.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserRefreshTokenPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * Refresh Token MyBatis-Plus Mapper。
 */
@Mapper
public interface UserRefreshTokenMapper extends BaseMapper<UserRefreshTokenPo> {
}
