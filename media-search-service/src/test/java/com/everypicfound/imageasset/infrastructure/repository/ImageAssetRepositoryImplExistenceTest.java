package com.everypicfound.imageasset.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.imageasset.infrastructure.mapper.ImageAssetMapper;
import com.everypicfound.imageasset.infrastructure.po.ImageAssetPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageAssetRepositoryImplExistenceTest {

    @Mock
    private ImageAssetMapper imageAssetMapper;
    @Mock
    private MetricRecorder metricRecorder;

    private ImageAssetRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ImageAssetRepositoryImpl(
                imageAssetMapper,
                metricRecorder);
    }

    @Test
    void existsByIdReturnsTrueWhenCountIsPositive() {
        when(imageAssetMapper.selectCount(
                any(LambdaQueryWrapper.class)))
                .thenReturn(1L);

        assertThat(repository.existsById(42L)).isTrue();
    }

    @Test
    void existsByIdReturnsFalseWhenCountIsZero() {
        when(imageAssetMapper.selectCount(
                any(LambdaQueryWrapper.class)))
                .thenReturn(0L);

        assertThat(repository.existsById(42L)).isFalse();
    }

    @Test
    void existsByIdRejectsInvalidIdWithoutMapperCall() {
        assertThat(repository.existsById(null)).isFalse();
        assertThat(repository.existsById(0L)).isFalse();

        verify(imageAssetMapper, never()).selectCount(any());
    }
}
