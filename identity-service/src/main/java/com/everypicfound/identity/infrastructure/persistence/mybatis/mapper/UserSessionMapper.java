package com.everypicfound.identity.infrastructure.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserSessionPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户会话 MyBatis-Plus Mapper。
 */
@Mapper
public interface UserSessionMapper extends BaseMapper<UserSessionPo> {
}
