package com.everypicfound.imageasset.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.command.ImageAssetSaveCommand;
import com.everypicfound.imageasset.application.command.ImageStatusUpdateCommand;
import com.everypicfound.imageasset.application.command.VectorStatusUpdateCommand;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.imageasset.infrastructure.mapper.ImageAssetMapper;
import com.everypicfound.imageasset.infrastructure.po.ImageAssetPO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ImageAssetRepositoryImpl implements ImageAssetRepository {

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_FAILED = "failed";
    private static final String RESULT_DUPLICATE = "duplicate";
    private static final String RESULT_NOT_FOUND = "not_found";
    private static final String RESULT_NOT_UPDATED = "not_updated";
    private static final String RESULT_PRESENT = "present";
    private static final String RESULT_ABSENT = "absent";
    private static final String RESULT_SKIPPED = "skipped";

    private final ImageAssetMapper imageAssetMapper;

    private final MetricRecorder metricRecorder;

    @Override
    public boolean save(ImageAssetSaveCommand command) {
        return observeRepositoryOperation("save",
                () -> {
                    ImageAssetPO po = toPO(command);
                    return imageAssetMapper.insert(po) > 0;
                },
                saved -> Boolean.TRUE.equals(saved) ? RESULT_SUCCESS : RESULT_NOT_UPDATED);
    }

    @Override
    public ImageAssetDTO findById(Long imageId) {
        if (imageId == null) {
            recordSkippedRepositoryOperation("find_by_id");
            return null;
        }

        return observeRepositoryOperation(
                "find_by_id",
                () -> toDTO(imageAssetMapper.selectById(imageId)),
                result -> result == null
                        ? RESULT_NOT_FOUND
                        : RESULT_SUCCESS);
    }

    @Override
    public List<ImageAssetDTO> findByIds(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            recordSkippedRepositoryOperation("find_by_ids");
            return Collections.emptyList();
        }

        return observeRepositoryOperation("find_by_ids",
                () -> {
                    // selectBatchIds被弃用，使用LambdaQueryWrapper帮助批量查询
                    LambdaQueryWrapper<ImageAssetPO> wrapper = new LambdaQueryWrapper<>();
                    wrapper.in(ImageAssetPO::getId, imageIds);

                    List<ImageAssetPO> records = imageAssetMapper.selectList(wrapper);

                    return records.stream()
                            .map(this::toDTO)
                            .toList();

                },
                ignored -> RESULT_SUCCESS);

    }

    @Override
    public boolean existsByFileHash(String fileHash) {
        if (fileHash == null || fileHash.isBlank()) {
            recordSkippedRepositoryOperation("exists_by_hash");
            return false;
        }

        return observeRepositoryOperation(
                "exists_by_hash",
                () -> {
                    // 这里用上了SQL建立的file_hash索引
                    LambdaQueryWrapper<ImageAssetPO> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(ImageAssetPO::getFileHash, fileHash)
                            .eq(ImageAssetPO::getImageStatus, ImageStatus.NORMAL.getCode())
                            .last("LIMIT 1");
                    Long count = imageAssetMapper.selectCount(wrapper);

                    return count != null && count > 0L;
                },
                exists -> Boolean.TRUE.equals(exists) ? RESULT_PRESENT : RESULT_ABSENT);

    }

    @Override
    public PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria) {
        return observeRepositoryOperation("page_query", () -> doPageQuery(criteria), ignored -> RESULT_SUCCESS);
    }

    private PageResult<ImageAssetDTO> doPageQuery(ImageAssetQueryCriteria criteria) {
        int pageNo = normalizePageNo(criteria == null ? null : criteria.getPageNo());
        int pageSize = normalizePageSize(criteria == null ? null : criteria.getPageSize());

        LambdaQueryWrapper<ImageAssetPO> wrapper = new LambdaQueryWrapper<>();

        if (criteria != null) {
            if (criteria.getImageStatus() != null) {
                wrapper.eq(ImageAssetPO::getImageStatus, criteria.getImageStatus().getCode());
            }
            if (criteria.getVectorStatus() != null) {
                wrapper.eq(ImageAssetPO::getVectorStatus, criteria.getVectorStatus().getCode());
            }
            if (criteria.getCreatedStartTime() != null) {
                wrapper.ge(ImageAssetPO::getCreatedTime, criteria.getCreatedStartTime());
            }

            if (criteria.getCreatedEndTime() != null) {
                wrapper.le(ImageAssetPO::getCreatedTime, criteria.getCreatedEndTime());
            }
        }

        wrapper.orderByDesc(ImageAssetPO::getCreatedTime);

        Page<ImageAssetPO> page = imageAssetMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);

        List<ImageAssetDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .toList();

        return new PageResult<>(
                page.getTotal(),
                pageNo,
                pageSize,
                records);

    }

    @Override
    public boolean updateImageStatus(ImageStatusUpdateCommand command) {
        if (command == null || command.getImageId() == null || command.getTargetStatus() == null) {
            recordSkippedRepositoryOperation("update_image_status");
            return false;
        }

        return observeRepositoryOperation("update_image_status",
                () -> doUpdateImageStatus(command),
                updated -> Boolean.TRUE.equals(updated) ? RESULT_SUCCESS : RESULT_NOT_UPDATED);

    }

    private boolean doUpdateImageStatus(ImageStatusUpdateCommand command) {

        LambdaUpdateWrapper<ImageAssetPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ImageAssetPO::getId, command.getImageId());

        // 这里的乐观锁更新思路是: SET version = version + 1 WHERE id = ? AND version = ?,
        if (command.getVersion() != null) {
            wrapper.eq(ImageAssetPO::getVersion, command.getVersion());
        }

        wrapper.set(ImageAssetPO::getImageStatus, command.getTargetStatus().getCode())
                .set(ImageAssetPO::getUpdatedTime, LocalDateTime.now())
                .setSql("version = version + 1");

        if (command.getFailReason() != null) {
            wrapper.set(ImageAssetPO::getFailReason, command.getFailReason().name());
        }

        return imageAssetMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean updateVectorStatus(VectorStatusUpdateCommand command) {
        if (command == null || command.getImageId() == null || command.getTargetStatus() == null) {
            recordSkippedRepositoryOperation("update_vector_status");
            return false;
        }

        return observeRepositoryOperation("update_vector_status",
                () -> doUpdateVectorStatus(command),
                updated -> Boolean.TRUE.equals(updated) ? RESULT_SUCCESS : RESULT_NOT_UPDATED);

    }

    private boolean doUpdateVectorStatus(VectorStatusUpdateCommand command) {
        LambdaUpdateWrapper<ImageAssetPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ImageAssetPO::getId, command.getImageId());

        if (command.getVersion() != null) {
            wrapper.eq(ImageAssetPO::getVersion, command.getVersion());
        }

        wrapper.set(ImageAssetPO::getVectorStatus, command.getTargetStatus().getCode())
                .set(ImageAssetPO::getUpdatedTime, LocalDateTime.now())
                .setSql("version = version + 1");

        if (command.getProcessingStartedTime() != null) {
            wrapper.set(ImageAssetPO::getProcessingStartedTime, command.getProcessingStartedTime());
        }

        if (command.getVectorUpdatedTime() != null) {
            wrapper.set(ImageAssetPO::getVectorUpdatedTime, command.getVectorUpdatedTime());
        }

        if (command.getRetryCount() != null) {
            wrapper.set(ImageAssetPO::getRetryCount, command.getRetryCount());
        }

        // updateReady的时候FailReason为null，直接以!=null判断就没法更新数据库
        if (Boolean.TRUE.equals(command.getClearFailReason())) {
            wrapper.set(ImageAssetPO::getFailReason, null);
        } else if (command.getFailReason() != null) {
            wrapper.set(ImageAssetPO::getFailReason, command.getFailReason().name());
        }

        return imageAssetMapper.update(null, wrapper) > 0;

    }

    @Override
    public boolean updateVectorReady(VectorStatusUpdateCommand command) {
        if (command == null) {
            recordSkippedRepositoryOperation("update_vector_ready");
            return false;
        }

        command.setTargetStatus(VectorStatus.READY);
        command.setVectorUpdatedTime(LocalDateTime.now());
        command.setFailReason(null);
        command.setClearFailReason(true);

        return observeRepositoryOperation(
                "update_vector_ready",
                () -> doUpdateVectorStatus(command),
                updated -> Boolean.TRUE.equals(updated) ? RESULT_SUCCESS : RESULT_NOT_UPDATED);

    }

    @Override
    public boolean updateVectorFailed(VectorStatusUpdateCommand command) {
        if (command == null) {
            recordSkippedRepositoryOperation("update_vector_failed");
            return false;
        }

        command.setTargetStatus(VectorStatus.FAILED);

        return observeRepositoryOperation(
                "update_vector_failed",
                () -> doUpdateVectorStatus(command),
                updated -> Boolean.TRUE.equals(updated) ? RESULT_SUCCESS : RESULT_NOT_UPDATED);
    }

    // 累加型字段retry_count使用原子自增，防止并发丢失更新
    @Override
    public boolean increaseRetryCount(Long imageId) {
        if (imageId == null) {
            recordSkippedRepositoryOperation("increase_retry");
            return false;
        }

        return observeRepositoryOperation(
            "increase_retry",
                     ()->{
                         LambdaUpdateWrapper<ImageAssetPO> wrapper = new LambdaUpdateWrapper<>();
                         wrapper.eq(ImageAssetPO::getId, imageId)
                                 .setSql("retry_count = COALESCE(retry_count, 0) + 1")
                                 .setSql("version = version + 1")
                                 .set(ImageAssetPO::getUpdatedTime, LocalDateTime.now());
                 
                         return imageAssetMapper.update(null, wrapper) > 0;

                     }, 
                    updated -> Boolean.TRUE.equals(updated) ? RESULT_SUCCESS : RESULT_NOT_UPDATED)

    }

    private ImageAssetPO toPO(ImageAssetSaveCommand command) {
        LocalDateTime now = LocalDateTime.now();

        return ImageAssetPO.builder()
                .id(command.getId())
                .fileName(command.getFileName())
                .originalFileName(command.getOriginalFileName())
                .fileHash(command.getFileHash())
                .fileSize(command.getFileSize())
                .mimeType(command.getMimeType())
                .fileExt(command.getFileExt())
                .width(command.getWidth())
                .height(command.getHeight())
                .storagePath(command.getStoragePath())
                .thumbnailPath(command.getThumbnailPath())
                .imageStatus(command.getImageStatus().getCode())
                .vectorStatus(command.getVectorStatus().getCode())
                .retryCount(0)
                .version(0)
                .createdTime(now)
                .updatedTime(now)
                .build();
    }

    private ImageAssetDTO toDTO(ImageAssetPO po) {
        if (po == null) {
            return null;
        }

        return ImageAssetDTO.builder()
                .id(po.getId())
                .fileName(po.getFileName())
                .originalFileName(po.getOriginalFileName())
                .fileHash(po.getFileHash())
                .fileSize(po.getFileSize())
                .mimeType(po.getMimeType())
                .fileExt(po.getFileExt())
                .width(po.getWidth())
                .height(po.getHeight())
                .storagePath(po.getStoragePath())
                .thumbnailPath(po.getThumbnailPath())
                .imageStatus(toImageStatus(po.getImageStatus()))
                .vectorStatus(toVectorStatus(po.getVectorStatus()))
                .createdTime(po.getCreatedTime())
                .updatedTime(po.getUpdatedTime())
                .vectorUpdatedTime(po.getVectorUpdatedTime())
                .processingStartedTime(po.getProcessingStartedTime())
                .retryCount(po.getRetryCount())
                .failReason(toFailReason(po.getFailReason()))
                .version(po.getVersion())
                .build();
    }

    private FailReason toFailReason(String failReason) {
        if (failReason == null || failReason.isBlank()) {
            return null;
        }

        return FailReason.valueOf(failReason);
    }

    private ImageStatus toImageStatus(Integer code) {
        if (code == null) {
            return null;
        }

        for (ImageStatus status : ImageStatus.values()) {
            if (Objects.equals(status.getCode(), code)) {
                return status;
            }
        }

        return null;
    }

    private VectorStatus toVectorStatus(Integer code) {
        if (code == null) {
            return null;
        }

        for (VectorStatus status : VectorStatus.values()) {
            if (Objects.equals(status.getCode(), code)) {
                return status;
            }
        }

        return null;

    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < 1) {
            return 1;
        }

        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 0;
        }

        return Math.min(pageSize, 100);
    }

    /*
     * 统一方法包装器，db操作在action中运行，在resultResolver中规定指标result判断方法。
     * 然后统一进行错误捕获和指标记录
     * RuntimeError不进行捕获，转到上层处理
     * 只捕获DuplicateKeyException，其他异常会自动向调用方传播
     */
    private <T> T observeRepositoryOperation(
            String operation,
            Supplier<T> action,
            Function<T, String> resultResolver) {
        long startTime = System.currentTimeMillis();
        String result = RESULT_FAILED;

        try {
            T value = action.get();
            result = resultResolver.apply(value);
            return value;
        } catch (DuplicateKeyException exception) {
            /*
             * 只更新指标结果，然后原样抛出。
             *
             * throw exception 不会增加新的异常层，
             * 也不会破坏 DuplicateKeyException 内部的 cause 链。
             */
            result = RESULT_DUPLICATE;
            throw exception;
        } finally {
            recordRepositoryMetrics(
                    operation,
                    result,
                    startTime);
        }
    }

    private void recordSkippedRepositoryOperation(String opeartion) {
        recordRepositoryMetrics(opeartion, RESULT_SKIPPED, System.currentTimeMillis());
    }

    private void recordRepositoryMetrics(
            String operation,
            String result,
            long startTime) {

        MetricTags tags = MetricTags.builder()
                .tag(MetricTag.OPERATION, operation)
                .tag(MetricTag.RESULT, result)
                .build();

        metricRecorder.increment(
                MetricName.IMAGE_ASSET_REPOSITORY_OPERATIONS,
                tags);

        metricRecorder.recordTimer(
                MetricName.IMAGE_ASSET_REPOSITORY_DURATION,
                System.currentTimeMillis() - startTime,
                tags);
    }

}
