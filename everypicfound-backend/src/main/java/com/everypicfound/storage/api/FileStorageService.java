package com.everypicfound.storage.api;

public interface FileStorageService {
    
    StoredFile save(StorageSaveRequest request);

    boolean delete(String storagePath);

    String getAccessUrl(String storagePath);
}
