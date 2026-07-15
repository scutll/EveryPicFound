package com.everypicfound.imageasset.domain.checker;

import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import java.time.LocalDateTime;

public interface ImageAssetStateChecker {

    // image_status = NORMAL。
    boolean isDisplayable(ImageStatus imageStatus);

    // image_status = NORMAL && vector_status = READY。
    boolean isSearchable(ImageStatus imageStatus, VectorStatus vectorStatus);

    // image_status = NORMAL && vector_status = PENDING。
    boolean canStartVectorization(ImageStatus imageStatus, VectorStatus vectorStatus);

    // 判断是否需要回退为 PENDING。
    boolean isProcessingTimeout(LocalDateTime processingStartedTime, long timeoutSeconds);
}
