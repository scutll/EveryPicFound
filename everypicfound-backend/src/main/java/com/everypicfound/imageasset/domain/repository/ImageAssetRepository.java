package com.everypicfound.imageasset.domain.repository;

import com.everypicfound.imageasset.application.command.ImageAssetSaveCommand;

public interface ImageAssetRepository {
    boolean save(ImageAssetSaveCommand command);

    boolean existsByFileHash(String fileHash);
}
