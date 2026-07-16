package com.everypicfound.identity.infrastructure.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserAccountPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户账户 MyBatis-Plus Mapper。
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountPo> {
}
