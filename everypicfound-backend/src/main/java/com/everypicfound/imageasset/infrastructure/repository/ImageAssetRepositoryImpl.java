package com.everypicfound.imageasset.infrastructure.repository;

import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.command.ImageAssetSaveCommand;
import com.everypicfound.imageasset.application.command.ImageStatusUpdateCommand;
import com.everypicfound.imageasset.application.command.VectorStatusUpdateCommand;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.imageasset.infrastructure.po.ImageAssetPO;
import java.util.List;

public class ImageAssetRepositoryImpl implements ImageAssetRepository {

    // 对应 image_asset 表的持久化对象。
    private ImageAssetPO imageAssetPO;

    // 新增图片资产记录。
    @Override
    public void save(ImageAssetSaveCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 根据 ID 查询图片。
    @Override
    public ImageAssetDTO findById(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 批量查询图片。
    @Override
    public List<ImageAssetDTO> findByIds(List<Long> imageIds) {
        throw new UnsupportedOperationException("TODO");
    }

    // 根据 hash 判断是否存在。
    @Override
    public boolean existsByFileHash(String fileHash) {
        throw new UnsupportedOperationException("TODO");
    }

    // 分页查询图片。
    @Override
    public PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria) {
        throw new UnsupportedOperationException("TODO");
    }

    // 更新图片状态。
    @Override
    public boolean updateImageStatus(ImageStatusUpdateCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 更新向量状态。
    @Override
    public boolean updateVectorStatus(VectorStatusUpdateCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 更新向量完成状态。
    @Override
    public boolean updateVectorReady(VectorStatusUpdateCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 更新向量失败状态。
    @Override
    public boolean updateVectorFailed(VectorStatusUpdateCommand command) {
        throw new UnsupportedOperationException("TODO");
    }
}
