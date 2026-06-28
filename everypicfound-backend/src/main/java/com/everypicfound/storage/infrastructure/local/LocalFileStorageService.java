package com.everypicfound.storage.infrastructure.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.storage.api.StorageSaveRequest;
import com.everypicfound.storage.api.StoredFile;
import com.everypicfound.storage.core.StoragePathContext;
import com.everypicfound.storage.core.StoragePathGenerator;
import com.everypicfound.storage.core.StorageResource;
import com.everypicfound.storage.error.StorageErrorCode;
import com.everypicfound.storage.infrastructure.config.StorageProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor// 本地文件存储服务实现，使用 Java NIO 操作文件系统。
public class LocalFileStorageService implements FileStorageService {

    //日志/指标相关字段

    private static final String OPERATION_SAVE = "save";
    private static final String OPERATION_READ = "read";
    private static final String OPERATION_DELETE = "delete";
    private static final String OPERATION_EXISTS = "exists";

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_FAILED = "failed";
    private static final String RESULT_NOT_FOUND = "not_found";
    private static final String RESULT_TRUE = "true";
    private static final String RESULT_FALSE = "false";

    private final StorageProperties storageProperties;
    // 本地存储配置对象，提供 basePath、accessUrlPrefix 等存储相关配置。
    //DateBasedStoragePathGenerator -> 生成 storagePath

    private final StoragePathGenerator storagePathGenerator;
    // 存储路径生成器接口，用于根据 imageId、fileExt、uploadTime 生成统一的相对 storagePath。
    //LocalFileStorageService       -> 根据 basePath 保存文件，并生成 accessUrl

    private final MetricRecorder metricRecorder;
    // LocalFileStorageService 记录保存、读取、删除耗时和失败次数等指标。

    @Override
    public StoredFile save(StorageSaveRequest request) {
        long startTime = System.currentTimeMillis();
        String storagePath = "";

        String result = RESULT_FAILED;
        try {
            validateSaveRequest(request);//检查请求是否合法，主要看 request、inputStream、imageId、fileExt 是否为空或非法。
            storagePath=generateStoragePath(request);
            Path targetPath = resolveStoragePath(storagePath);//把相对路径转成真实本地路径，防止路径穿越攻击。

            Files.createDirectories(targetPath.getParent());//创建父目录，如果不存在的话。
            Files.copy(request.getInputStream(),targetPath, StandardCopyOption.REPLACE_EXISTING);//把输入流保存到目标路径，覆盖同名文件。

            StoredFile storedFile = new StoredFile();//创建存储文件对象，准备返回给调用方。
            storedFile.setAccessUrl(getAccessUrl(storagePath));//生成访问 URL，作为存储文件的 accessUrl 属性。

            storedFile.setStoragePath(storagePath);//把相对路径存储到 storagePath 属性，方便后续删除或读取时使用。
            // codex: StoredFile.fileName 表示系统保存后的文件名，应使用 storagePath 的最后一段，而不是用户原始文件名。

            storedFile.setFileName(targetPath.getFileName().toString());
            storedFile.setFileExt(request.getFileExt());//把文件扩展名存储到 fileExt 属性，方便后续使用。
            storedFile.setFileSize(request.getFileSize());//把文件大小存储到 fileSize 属性，方便后续使用。
            storedFile.setMimeType(request.getMimeType());//把文件 MIME 类型存储到 mimeType 属性，方便后续使用。

            result = RESULT_SUCCESS;
            recordStorageFileSize(OPERATION_SAVE, request.getFileSize());
            
            return storedFile;

        } catch (IOException e) {
            /*
             * IOException 第一次转换为项目统一异常。
             * 不直接报告错误而是由上层调用段报告异常栈
             * 必须把 exception 作为 cause 传入。
             */
            throw new SystemException(StorageErrorCode.FILE_SAVE_FAILED, e);
        } finally {
            recordStorageMetrics(OPERATION_SAVE, result, startTime);
        }
    }

    @Override
    public boolean delete(String storagePath) {
        long startTime = System.currentTimeMillis();
        String result = RESULT_FAILED;
        try {
            validateStoragePath(storagePath);//检查 storagePath 不能为空。
            Path targetPath = resolveStoragePath(storagePath);
            boolean deleted = Files.deleteIfExists(targetPath);


            if (!deleted) {
                result = RESULT_NOT_FOUND;
                return false;
            }

            result = RESULT_SUCCESS;
            return true;
        } catch (IOException e) {
            throw new SystemException(StorageErrorCode.FILE_DELETE_FAILED, e);
        } finally {
            recordStorageMetrics(OPERATION_DELETE, result, startTime);
        }
    }

    @Override
    public String getAccessUrl(String storagePath) {
        validateStoragePath(storagePath);//检查 storagePath 不能为空。

        String prefix = storageProperties.getAccessUrlPrefix();//获取配置的访问 URL 前缀。
        if(prefix == null||prefix.isBlank()){
            prefix = "";//如果前缀为空，则使用空字符串，直接返回 storagePath 作为访问 URL。
        }

        String normalizedPrefix =trimTrailingSlash(prefix.trim());//去掉前缀末尾的 /，防止重复斜杠。
        String normalizedPath = trimLeadingSlash(normalizeStoragePath(storagePath));//去掉 storagePath 开头的 /，防止重复斜杠。

        if(normalizedPrefix.isEmpty()) {
            return "/"+ normalizedPath; //如果前缀为空，则直接返回 storagePath，确保以 / 开头。
        } else {
            return normalizedPrefix + "/" + normalizedPath;//否则，拼接前缀和路径，返回完整的访问 URL。
        }
    }

    @Override
    public StorageResource read(String storagePath) {
        long startTime = System.currentTimeMillis();
        String result = RESULT_FAILED;
        try {
            Path targetPath = resolveStoragePath(storagePath);//把相对路径转成真实本地路径，防止路径穿越攻击。

            if (!Files.exists(targetPath)) {
                result = RESULT_NOT_FOUND;

                throw new SystemException(StorageErrorCode.FILE_NOT_FOUND);
            }

            long fileSize = Files.size(targetPath);
            String mimeType = Files.probeContentType(targetPath);
            String fileExt = extractFileExt(targetPath.getFileName().toString());
            String accessUrl = getAccessUrl(storagePath);

            StorageResource resource = new StorageResource();
            resource.setFileSize(fileSize);// 获取文件大小
            resource.setMimeType(mimeType);// 获取 MIME 类型
            resource.setFileExt(fileExt);//从文件名里提取扩展名，作为资源的 fileExt 属性。
            resource.setAccessUrl(accessUrl);//生成访问 URL，作为资源的 accessUrl 属性。
            resource.setInputStream(Files.newInputStream(targetPath));// 获取文件输入流
            
            
            result = RESULT_SUCCESS;

            recordStorageFileSize(OPERATION_READ, fileSize);

            return resource;
        } catch (IOException e) {
            throw new SystemException(StorageErrorCode.FILE_READ_FAILED, e);
        } finally {
            recordStorageMetrics(OPERATION_READ, result, startTime);
        }
    }


    @Override
    public boolean exists(String storagePath) {
        long startTime = System.currentTimeMillis();
        String result = RESULT_FAILED;

        try{
            Path targetPath = resolveStoragePath(storagePath);//把相对路径转成真实本地路径，防止路径穿越攻击。
            boolean exists = Files.exists(targetPath);

            result = exists? RESULT_TRUE:RESULT_FALSE;
            
            return exists;
        } finally {
            recordStorageMetrics(OPERATION_EXISTS, result, startTime);
        }
    }

    //保存前检查请求是否合法。主要看 request、inputStream、imageId、fileExt 是否为空或非法。
    private void validateSaveRequest(StorageSaveRequest request){
        if(request == null||request.getInputStream() == null){
            throw new SystemException(StorageErrorCode.FILE_SAVE_FAILED);
    }

        if(request.getImageId() == null||request.getImageId() <= 0){
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
    }

        // codex: 扩展名不能只判断 empty，空白字符串也不能参与 storagePath 生成。
        if(request.getFileExt() == null||request.getFileExt().isBlank()){
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
    }
    }

    //把 request 里的 imageId / fileExt 组装成 StoragePathContext，然后交给 StoragePathGenerator 生成相对路径。
    private String generateStoragePath(StorageSaveRequest request){
        StoragePathContext context = new StoragePathContext();// 生成存储路径上下文对象,调用接口获取图片ID和文件扩展名。
        context.setImageId(request.getImageId());
        context.setFileExt(request.getFileExt());
        context.setUploadTime(LocalDateTime.now());

        return storagePathGenerator.generatePath(context);
    }
    //把相对路径转成真实本地路径，比如：
    /*storagePath = 2026/05/28/100001.jpg
    basePath = ./data/images
    targetPath = ./data/images/2026/05/28/100001.jpg */
    private Path resolveStoragePath(String storagePath){
        validateStoragePath(storagePath);

        Path basePath = resolveBasePath();//获取配置的存储路径。
        Path targetPath = basePath.resolve(normalizeStoragePath(storagePath)).normalize();//获取绝对路径

        // codex: 防止传入 "../" 或绝对路径逃逸出本地存储根目录。
        if (!targetPath.startsWith(basePath)) {
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
        }

        return targetPath;
    }

    //读取 storageProperties.getBasePath()，转成绝对路径并标准化。
    private Path resolveBasePath(){

        String basePath = storageProperties.getBasePath();
        // codex: basePath 为空白时同样表示存储不可用，避免 Path.of("   ") 生成无意义路径。
        if(basePath == null||basePath.isBlank()){
            throw new SystemException(StorageErrorCode.STORAGE_UNAVAILABLE);
        }
        return Path.of(basePath).toAbsolutePath().normalize();//把 basePath 转成绝对路径并标准化，返回 Path 对象。
    }

    //检查 storagePath 不能为空。
    private void validateStoragePath(String storagePath){
        // codex: storagePath 为空白时属于非法路径，避免被 trim 后变成空路径指向存储根目录。
        if(storagePath == null||storagePath.isBlank()){
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
        }
    }

    //统一路径分隔符，把 \ 转成 /，并去掉开头的 /
    private String normalizeStoragePath(String storagePath){
        String normalized = storagePath.trim().replace("\\", "/");
        return trimLeadingSlash(normalized);//去掉开头的 /，防止 resolve 时被当成绝对路径。
    }

    //去掉字符串开头的 /。
    private String trimLeadingSlash(String value){
        String result = value;
        while(result.startsWith("/")){
            result = result.substring(1);

    }
        return result;
    }

    //去掉字符串结尾的 /。
    private String trimTrailingSlash(String value){
        String result = value;
        while(result.endsWith("/")){
            result = result.substring(0,result.length()-1);
        }
        return result;
    }



    //从文件名里提取扩展名，比如：
    private String extractFileExt(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        // codex: 没有点号或点号在最后一位时都没有合法扩展名，不能把整个文件名误当作扩展名。
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);//返回扩展名，不带点。substring 从 dotIndex + 1 开始，直到字符串末尾。
    }
    
    private void recordStorageMetrics(
            String operation,
            String result,
            long startTime
    ) {
        MetricTags tags = MetricTags.builder()
                .tag(MetricTag.OPERATION, operation)
                .tag(MetricTag.RESULT, result)
                .build();

        metricRecorder.increment(
                MetricName.STORAGE_OPERATIONS,
                tags);

        metricRecorder.recordTimer(
                MetricName.STORAGE_DURATION,
                System.currentTimeMillis() - startTime,
                tags);
    }
    
    private void recordStorageFileSize(
            String operation,
            Number fileSize) {

        metricRecorder.recordValue(
                MetricName.STORAGE_FILE_SIZE,
                fileSize,
                MetricTags.builder()
                        .tag(MetricTag.OPERATION, operation)
                        .build());
    }
}
