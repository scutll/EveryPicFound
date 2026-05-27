package com.everypicfound.storage.infrastructure.local;

import com.everypicfound.common.log.LogService;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.storage.core.StoragePathGenerator;
import com.everypicfound.storage.core.StorageResource;
import com.everypicfound.storage.core.StorageSaveRequest;
import com.everypicfound.storage.core.StoredFile;
import com.everypicfound.storage.infrastructure.config.StorageProperties;
public class LocalFileStorageService implements FileStorageService {

    // 统一生成图片存储路径，避免业务模块拼接路径。
    private StoragePathGenerator storagePathGenerator;

    // 存储配置。
    private StorageProperties storageProperties;

    // 记录文件操作日志。
    private LogService logService;

    // 记录文件操作指标。
    private MetricRecorder metricRecorder;

    // 保存文件到本地文件系统，返回保存结果。
    @Override
    public StoredFile save(StorageSaveRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    // 根据 storagePath 读取文件。
    @Override
    public StorageResource read(String storagePath) {
        throw new UnsupportedOperationException("TODO");
    }

    // 根据 storagePath 删除文件。
    @Override
    public void delete(String storagePath) {
        throw new UnsupportedOperationException("TODO");
    }

    // 判断文件是否存在。
    @Override
    public boolean exists(String storagePath) {
        throw new UnsupportedOperationException("TODO");
    }

    // 根据 storagePath 生成访问地址。
    @Override
    public String getAccessUrl(String storagePath) {
        throw new UnsupportedOperationException("TODO");
    }
}
