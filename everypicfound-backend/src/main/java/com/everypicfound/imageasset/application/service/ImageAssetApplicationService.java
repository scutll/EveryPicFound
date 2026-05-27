package com.everypicfound.imageasset.application.service;

import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.command.ImageUploadCommand;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.application.result.ImageUploadResult;

public interface ImageAssetApplicationService {

    // 上传图片并写入元数据。
    ImageUploadResult upload(ImageUploadCommand command);

    // 查询图片详情。
    ImageAssetDTO getDetail(Long imageId);

    // 分页查询图片。
    PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria);

    // 删除图片资产，MVP 先逻辑删除。
    void delete(Long imageId);
}
