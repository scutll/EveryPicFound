package com.everypicfound.imageasset.interfaces.controller;

import com.everypicfound.common.context.RequestContext;
import org.springframework.web.bind.annotation.*;


import com.everypicfound.imageasset.application.service.ImageAssetApplicationService;
import com.everypicfound.imageasset.interfaces.request.ImageUploadRequest;
import com.everypicfound.imageasset.interfaces.response.ImageUploadResponse;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.response.Result;
import com.everypicfound.imageasset.application.command.ImageUploadCommand;
import com.everypicfound.imageasset.application.result.ImageUploadResult;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;


import java.io.IOException;



@RestController
@RequestMapping("/api/images")
public class ImageAssetController {


    private final ImageAssetApplicationService imageAssetApplicationService;

    public ImageAssetController(ImageAssetApplicationService imageAssetApplicationService) {
        this.imageAssetApplicationService = imageAssetApplicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, path = "/upload")
    public Result<ImageUploadResponse> upload(@ModelAttribute ImageUploadRequest request) throws IOException {
        ImageUploadCommand command = buildUploadCommand(request);

        ImageUploadResult result = imageAssetApplicationService.upload(command);

        ImageUploadResponse response = buildUploadResponse(result);

        return Result.success(response, getRequestId());
    }
    
    private ImageUploadCommand buildUploadCommand(ImageUploadRequest request) throws IOException {
        return ImageUploadCommand.builder()
                .inputStream(request.getImageFile().getInputStream())
                .originalFileName(request.getImageFile().getOriginalFilename())
                .fileSize(request.getImageFile().getSize())
                .mimeType(request.getImageFile().getContentType())
                .build();
    }

    private ImageUploadResponse buildUploadResponse(ImageUploadResult result) {
        return ImageUploadResponse.builder()
                .imageId(result.getImageId())
                .originalFileName(result.getOriginalFileName())
                .imageUrl(result.getImageUrl())
                .imageStatus(result.getImageStatus())
                .vectorStatus(result.getVectorStatus())
                .build();
    }

    private String getRequestId() {
        RequestContext context = RequestContextHolder.get();
        return context == null ? null : context.getRequestId();
    }


    
    
}
