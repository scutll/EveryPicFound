package com.everypicfound.imageasset.application.service;

import com.everypicfound.common.log.LogService;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.command.ImageUploadCommand;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.application.result.ImageUploadResult;
import com.everypicfound.imageasset.domain.duplicate.ImageDuplicateChecker;
import com.everypicfound.imageasset.domain.extractor.ImageMetadataExtractor;
import com.everypicfound.imageasset.domain.generator.FileHashCalculator;
import com.everypicfound.imageasset.domain.generator.ImageFileNameGenerator;
import com.everypicfound.imageasset.domain.generator.ImageIdGenerator;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.imageasset.domain.service.OrphanFileLogService;
import com.everypicfound.imageasset.domain.validator.ImageUploadValidator;
import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.storage.core.StorageSaveRequest;
import com.everypicfound.storage.core.StoredFile;
import com.everypicfound.vectorization.api.VectorizationTaskPublisher;

public class DefaultImageAssetApplicationService implements ImageAssetApplicationService {

    // 图片上传校验器。
    private ImageUploadValidator imageUploadValidator;

    // 图片元数据解析器。
    private ImageMetadataExtractor imageMetadataExtractor;

    // 文件 hash 计算器。
    private FileHashCalculator fileHashCalculator;

    // 图片文件名生成器。
    private ImageFileNameGenerator imageFileNameGenerator;

    // 图片 ID 生成器。
    private ImageIdGenerator imageIdGenerator;

    // 图片去重检查器。
    private ImageDuplicateChecker imageDuplicateChecker;

    // 图片资产仓储。
    private ImageAssetRepository imageAssetRepository;

    // 文件存储服务。
    private FileStorageService fileStorageService;

    // 文件保存请求。
    private StorageSaveRequest storageSaveRequest;

    // 文件保存成功后的返回结果。
    private StoredFile storedFile;

    // 向量化任务发布器。
    private VectorizationTaskPublisher vectorizationTaskPublisher;

    // 孤儿文件日志服务。
    private OrphanFileLogService orphanFileLogService;

    // 日志服务。
    private LogService logService;

    // 指标记录器。
    private MetricRecorder metricRecorder;

    // 编排图片上传完整流程。
    @Override
    public ImageUploadResult upload(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 查询图片详情并生成访问 URL。
    @Override
    public ImageAssetDTO getDetail(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 分页查询图片列表。
    @Override
    public PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria) {
        throw new UnsupportedOperationException("TODO");
    }

    // 逻辑删除图片资产。
    @Override
    public void delete(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }
}
