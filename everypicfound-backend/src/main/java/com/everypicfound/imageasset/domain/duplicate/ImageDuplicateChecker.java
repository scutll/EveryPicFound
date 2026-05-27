package com.everypicfound.imageasset.domain.duplicate;

import com.everypicfound.imageasset.application.dto.ImageAssetDTO;

public interface ImageDuplicateChecker {

    // 重复则抛出 DUPLICATE_IMAGE。
    ImageAssetDTO checkDuplicate(String fileHash);

    // 返回是否存在相同 hash。
    boolean existsByFileHash(String fileHash);
}
