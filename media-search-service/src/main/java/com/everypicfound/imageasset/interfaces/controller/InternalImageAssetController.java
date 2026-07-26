package com.everypicfound.imageasset.interfaces.controller;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.response.Result;
import com.everypicfound.imageasset.application.service.ImageAssetApplicationService;
import com.everypicfound.imageasset.interfaces.response.ImageExistenceResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/images")
public class InternalImageAssetController {

    private final ImageAssetApplicationService imageAssetApplicationService;

    public InternalImageAssetController(
            ImageAssetApplicationService imageAssetApplicationService) {
        this.imageAssetApplicationService = imageAssetApplicationService;
    }

    @GetMapping("/{pictureId}/exists")
    public Result<ImageExistenceResponse> exists(
            @PathVariable @Positive long pictureId) {
        ImageExistenceResponse response = new ImageExistenceResponse(
                pictureId,
                imageAssetApplicationService.exists(pictureId));
        return Result.success(response, currentRequestId());
    }

    private String currentRequestId() {
        RequestContext context = RequestContextHolder.get();
        return context == null ? null : context.getRequestId();
    }
}
