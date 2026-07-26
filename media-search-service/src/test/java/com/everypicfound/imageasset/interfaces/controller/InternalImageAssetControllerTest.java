package com.everypicfound.imageasset.interfaces.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.everypicfound.common.response.Result;
import com.everypicfound.imageasset.application.service.ImageAssetApplicationService;
import com.everypicfound.imageasset.interfaces.response.ImageExistenceResponse;
import org.junit.jupiter.api.Test;

class InternalImageAssetControllerTest {

    @Test
    void existsReturnsPictureIdAndPhysicalExistence() {
        ImageAssetApplicationService applicationService =
                mock(ImageAssetApplicationService.class);
        when(applicationService.exists(42L)).thenReturn(true);
        InternalImageAssetController controller =
                new InternalImageAssetController(applicationService);

        Result<ImageExistenceResponse> result =
                controller.exists(42L);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData().pictureId()).isEqualTo(42L);
        assertThat(result.getData().exists()).isTrue();
    }
}
