package com.everypicfound.imageasset.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.command.ImageAssetSaveCommand;
import com.everypicfound.imageasset.application.command.ImageStatusUpdateCommand;
import com.everypicfound.imageasset.application.command.VectorStatusUpdateCommand;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.imageasset.infrastructure.mapper.ImageAssetMapper;
import com.everypicfound.imageasset.infrastructure.po.ImageAssetPO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ImageAssetRepositoryImpl implements ImageAssetRepository {

    private final ImageAssetMapper imageAssetMapper;

    @Override
    public boolean save(ImageAssetSaveCommand command) {
        ImageAssetPO po = toPO(command);
        return imageAssetMapper.insert(po) > 0;
    }

    @Override
    public ImageAssetDTO findById(Long imageId) {
        if (imageId == null) {
            return null;
        }

        ImageAssetPO po = imageAssetMapper.selectById(imageId);
        return toDTO(po);
    }

    @Override
    public List<ImageAssetDTO> findByIds(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return Collections.emptyList();
        }

        // selectBatchIds被弃用，使用LambdaQueryWrapper帮助批量查询
        LambdaQueryWrapper<ImageAssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ImageAssetPO::getId, imageIds);

        List<ImageAssetPO> records = imageAssetMapper.selectList(wrapper);

        return records.stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public boolean existsByFileHash(String fileHash) {
        if (fileHash == null || fileHash.isBlank()) {
            return false;
        }

        // 这里用上了SQL建立的file_hash索引
        LambdaQueryWrapper<ImageAssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImageAssetPO::getFileHash, fileHash)
                .eq(ImageAssetPO::getImageStatus, ImageStatus.NORMAL.getCode())
                .last("LIMIT 1");
        Long count = imageAssetMapper.selectCount(wrapper);
        return count != null && count > 0L;
    }

    @Override
    public PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria) {
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
                wrapper.eq(ImageAssetPO::getCreatedTime, criteria.getCreatedStartTime());
            }

            if (criteria.getCreatedEndTime() != null) {
                wrapper.eq(ImageAssetPO::getCreatedTime, criteria.getCreatedEndTime());
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
                records
        );

    }

    @Override
    public boolean updateImageStatus(ImageStatusUpdateCommand command) {
        if (command == null || command.getImageId() == null || command.getTargetStatus() == null) {
            return false;
        }

        LambdaUpdateWrapper<ImageAssetPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ImageAssetPO::getId, command.getImageId());


        //这里的乐观锁更新思路是: SET version = version + 1 WHERE id = ? AND version = ?, 
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
            return false;
        }

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

        if (command.getFailReason() != null) {
            wrapper.set(ImageAssetPO::getFailReason, command.getFailReason());
        }

        return imageAssetMapper.update(null, wrapper) > 0;
        

    }

    @Override
    public boolean updateVectorReady(VectorStatusUpdateCommand command) {
        if (command != null) {
            return false;
        }

        command.setTargetStatus(VectorStatus.READY);
        command.setVectorUpdatedTime(LocalDateTime.now());
        command.setFailReason(null);

        return updateVectorStatus(command);
    }

    @Override
    public boolean updateVectorFailed(VectorStatusUpdateCommand command) {
        if (command == null) {
            return false;
        }

        command.setTargetStatus(VectorStatus.FAILED);

        return updateVectorStatus(command);
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
                .build();
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

}
