package com.everypicfound.imageasset.domain.service;

import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.BatchImageAssetQuery;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.application.result.ImageAssetBatchQueryResult;
import java.util.List;

public interface ImageAssetQueryService {

    // 查询单张图片。
    ImageAssetDTO getById(Long imageId);

    // 根据 imageId 批量查询。
    ImageAssetBatchQueryResult batchQueryByIds(BatchImageAssetQuery query);

    // 分页查询。
    PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria);

    // 过滤可搜索图片。
    List<ImageAssetDTO> getSearchableAssets(ImageAssetQueryCriteria criteria);
}
