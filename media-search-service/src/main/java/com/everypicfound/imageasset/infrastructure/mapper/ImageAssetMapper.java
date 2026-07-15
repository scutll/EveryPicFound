package com.everypicfound.imageasset.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.everypicfound.imageasset.infrastructure.po.ImageAssetPO;


@Mapper
public interface ImageAssetMapper extends BaseMapper<ImageAssetPO>{
    
}
