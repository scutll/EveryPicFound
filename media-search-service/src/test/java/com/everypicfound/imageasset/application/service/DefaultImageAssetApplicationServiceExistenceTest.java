package com.everypicfound.imageasset.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everypicfound.common.log.LogService;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.imageasset.domain.duplicate.ImageDuplicateChecker;
import com.everypicfound.imageasset.domain.extractor.ImageMetadataExtractor;
import com.everypicfound.imageasset.domain.generator.FileHashCalculator;
import com.everypicfound.imageasset.domain.generator.ImageFileNameGenerator;
import com.everypicfound.imageasset.domain.generator.ImageIdGenerator;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.imageasset.domain.service.OrphanFileLogService;
import com.everypicfound.imageasset.domain.validator.ImageUploadValidator;
import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.vectorization.api.VectorizationTaskPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultImageAssetApplicationServiceExistenceTest {

    @Mock
    private ImageUploadValidator imageUploadValidator;
    @Mock
    private ImageMetadataExtractor imageMetadataExtractor;
    @Mock
    private FileHashCalculator fileHashCalculator;
    @Mock
    private ImageFileNameGenerator imageFileNameGenerator;
    @Mock
    private ImageIdGenerator imageIdGenerator;
    @Mock
    private ImageDuplicateChecker imageDuplicateChecker;
    @Mock
    private ImageAssetRepository imageAssetRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private VectorizationTaskPublisher vectorizationTaskPublisher;
    @Mock
    private OrphanFileLogService orphanFileLogService;
    @Mock
    private LogService logService;
    @Mock
    private MetricRecorder metricRecorder;

    @InjectMocks
    private DefaultImageAssetApplicationService applicationService;

    @Test
    void existsDelegatesPositiveIdToRepository() {
        when(imageAssetRepository.existsById(42L))
                .thenReturn(true);

        assertThat(applicationService.exists(42L)).isTrue();
        verify(imageAssetRepository).existsById(42L);
    }

    @Test
    void existsRejectsInvalidIdWithoutRepositoryCall() {
        assertThat(applicationService.exists(null)).isFalse();
        assertThat(applicationService.exists(0L)).isFalse();
        assertThat(applicationService.exists(-1L)).isFalse();

        verify(imageAssetRepository, never()).existsById(
                org.mockito.ArgumentMatchers.any());
    }
}
