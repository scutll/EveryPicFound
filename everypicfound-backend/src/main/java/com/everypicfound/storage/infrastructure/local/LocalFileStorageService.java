package com.everypicfound.storage.infrastructure.local;

import org.springframework.stereotype.Service;

import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.storage.api.StorageSaveRequest;
import com.everypicfound.storage.api.StoredFile;

@Service
public class LocalFileStorageService implements FileStorageService {
    
    @Override
    public StoredFile save(StorageSaveRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean delete(String storagePath) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String getAccessUrl(String storagePath) {
        throw new UnsupportedOperationException("TODO");
    }
}
