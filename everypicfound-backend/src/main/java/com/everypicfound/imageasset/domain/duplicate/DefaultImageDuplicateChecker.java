package com.everypicfound.imageasset.domain.duplicate;

import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;

public class DefaultImageDuplicateChecker implements ImageDuplicateChecker {

    // 图片资产仓储。
    private ImageAssetRepository imageAssetRepository;

    // 记录去重指标。
    private MetricRecorder metricRecorder;

    // 根据 file_hash 判断图片是否重复。
    @Override
    public ImageAssetDTO checkDuplicate(String fileHash) {
        throw new UnsupportedOperationException("TODO");
    }

    // 判断 hash 是否已存在。
    @Override
    public boolean existsByFileHash(String fileHash) {
        throw new UnsupportedOperationException("TODO");
    }
}
