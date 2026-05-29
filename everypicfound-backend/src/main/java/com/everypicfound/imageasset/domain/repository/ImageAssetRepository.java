package com.everypicfound.imageasset.domain.repository;

import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.command.ImageAssetSaveCommand;
import com.everypicfound.imageasset.application.command.ImageStatusUpdateCommand;
import com.everypicfound.imageasset.application.command.VectorStatusUpdateCommand;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import java.util.List;

public interface ImageAssetRepository {

    // 保存图片元数据。
    boolean save(ImageAssetSaveCommand command);

    // 单个查询。
    ImageAssetDTO findById(Long imageId);

    // 批量查询。
    List<ImageAssetDTO> findByIds(List<Long> imageIds);

    // 去重查询。
    boolean existsByFileHash(String fileHash);

    // 分页查询。
    PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria);

    // 更新 image_status。
    boolean updateImageStatus(ImageStatusUpdateCommand command);

    // 更新 vector_status。
    boolean updateVectorStatus(VectorStatusUpdateCommand command);

    // 标记向量 READY。
    boolean updateVectorReady(VectorStatusUpdateCommand command);

    // 标记向量 FAILED。
    boolean updateVectorFailed(VectorStatusUpdateCommand command);
}
