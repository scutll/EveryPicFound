package com.everypicfound.imageasset.domain.checker;

import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import java.time.LocalDateTime;

public class DefaultImageAssetStateChecker implements ImageAssetStateChecker {

    // 判断图片是否可展示。
    @Override
    public boolean isDisplayable(ImageStatus imageStatus) {
        throw new UnsupportedOperationException("TODO");
    }

    // 判断图片是否可搜索。
    @Override
    public boolean isSearchable(ImageStatus imageStatus, VectorStatus vectorStatus) {
        throw new UnsupportedOperationException("TODO");
    }

    // 判断是否允许开始向量化。
    @Override
    public boolean canStartVectorization(ImageStatus imageStatus, VectorStatus vectorStatus) {
        throw new UnsupportedOperationException("TODO");
    }

    // 判断 PROCESSING 是否超时。
    @Override
    public boolean isProcessingTimeout(LocalDateTime processingStartedTime, long timeoutSeconds) {
        throw new UnsupportedOperationException("TODO");
    }
}
