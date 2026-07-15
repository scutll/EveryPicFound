package com.everypicfound.imageasset.domain.service;

import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.BatchImageAssetQuery;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.application.result.ImageAssetBatchQueryResult;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class DefaultImageAssetQueryService implements ImageAssetQueryService {

    private final ImageAssetRepository imageAssetRepository;

    // 根据 imageId 查询图片。
    @Override
    public ImageAssetDTO getById(Long imageId) {
        if (imageId == null) {
            return null;
        }

        return imageAssetRepository.findById(imageId);
    }

    // 批量查询图片，供 search 回表。
    @Override
    public ImageAssetBatchQueryResult batchQueryByIds(BatchImageAssetQuery query) {
        if (query == null || query.getImageIds() == null || query.getImageIds().isEmpty()) {
            return ImageAssetBatchQueryResult.builder()
                    .items(Collections.emptyList())
                    .build();
        }
        
        List<ImageAssetDTO> items = imageAssetRepository.findByIds(query.getImageIds());

        return ImageAssetBatchQueryResult.builder()
                .items(items == null? Collections.emptyList() : items)
                .build();
    }

    // 分页查询图片。
    @Override
    public PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria) {
        return imageAssetRepository.pageQuery(criteria);
    }

    // 返回可搜索图片，即 NORMAL + READY。
    @Override
    public List<ImageAssetDTO> getSearchableAssets(ImageAssetQueryCriteria criteria) {
        return imageAssetRepository.pageQuery(criteria).getRecords();
    }
}
