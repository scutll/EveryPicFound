package com.everypicfound.imageasset.interfaces.controller;

import com.everypicfound.common.response.PageResult;
import com.everypicfound.common.response.Result;
import com.everypicfound.imageasset.interfaces.request.ImageAssetPageQuery;
import com.everypicfound.imageasset.interfaces.request.ImageUploadRequest;
import com.everypicfound.imageasset.interfaces.response.ImageDetailResponse;
import com.everypicfound.imageasset.interfaces.response.ImageUploadResponse;

public class ImageAssetController {

    // 接收 POST /api/images 上传请求。
    public Result<ImageUploadResponse> upload(ImageUploadRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    // 查询图片详情。
    public Result<ImageDetailResponse> getDetail(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 分页查询图片列表。
    public Result<PageResult<ImageDetailResponse>> pageQuery(ImageAssetPageQuery query) {
        throw new UnsupportedOperationException("TODO");
    }

    // 逻辑删除图片。
    public Result<Void> delete(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }
}
