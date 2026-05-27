package com.everypicfound.imageasset.infrastructure.repository;

import org.springframework.stereotype.Repository;

import com.everypicfound.imageasset.application.command.ImageAssetSaveCommand;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;

@Repository
public class ImageAssetRepositoryImpl implements ImageAssetRepository{
    @Override
    public boolean save(ImageAssetSaveCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean existsByFileHash(String fileHash) {
        throw new UnsupportedOperationException("TODO");
    }
}
