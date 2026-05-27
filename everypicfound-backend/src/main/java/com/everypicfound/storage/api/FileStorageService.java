package com.everypicfound.storage.api;

import com.everypicfound.storage.core.StorageResource;
import com.everypicfound.storage.core.StorageSaveRequest;
import com.everypicfound.storage.core.StoredFile;
public interface FileStorageService {

    // 保存文件到本地文件系统，返回保存结果。
    StoredFile save(StorageSaveRequest request);

    // 根据 storagePath 读取文件。
    StorageResource read(String storagePath);

    // 根据 storagePath 删除文件。
    void delete(String storagePath);

    // 判断文件是否存在。
    boolean exists(String storagePath);

    // 根据 storagePath 生成访问地址。
    String getAccessUrl(String storagePath);
}
