package com.everypicfound.imageasset.domain.service;

import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.BatchImageAssetQuery;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.application.result.ImageAssetBatchQueryResult;
import com.everypicfound.imageasset.domain.checker.ImageAssetStateChecker;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.storage.api.FileStorageService;
import java.util.List;

public class DefaultImageAssetQueryService implements ImageAssetQueryService {

    // 图片资产仓储。
    private ImageAssetRepository imageAssetRepository;

    // 图片状态判断器。
    private ImageAssetStateChecker imageAssetStateChecker;

    // 文件存储服务。
    private FileStorageService fileStorageService;

    // 根据 imageId 查询图片。
    @Override
    public ImageAssetDTO getById(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 批量查询图片，供 search 回表。
    @Override
    public ImageAssetBatchQueryResult batchQueryByIds(BatchImageAssetQuery query) {
        throw new UnsupportedOperationException("TODO");
    }

    // 分页查询图片。
    @Override
    public PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria) {
        throw new UnsupportedOperationException("TODO");
    }

    // 返回可搜索图片，即 NORMAL + READY。
    @Override
    public List<ImageAssetDTO> getSearchableAssets(ImageAssetQueryCriteria criteria) {
        throw new UnsupportedOperationException("TODO");
    }
}
