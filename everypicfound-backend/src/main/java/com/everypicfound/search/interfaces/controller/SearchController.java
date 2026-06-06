package com.everypicfound.search.interfaces.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.response.Result;
import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.application.service.SearchApplicationService;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.interfaces.request.HybridSearchRequest;
import com.everypicfound.search.interfaces.request.ImageSearchRequest;
import com.everypicfound.search.interfaces.request.TextSearchRequest;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    //注入SearchApplicationService，负责处理搜索请求的业务逻辑
    private final SearchApplicationService searchApplicationService;

    public SearchController(SearchApplicationService searchApplicationService) {
        this.searchApplicationService = searchApplicationService;
    }

    //  接收ImageSearchRequest对象，并构建SearchCommand对象，调用SearchApplicationService的search方法执行搜索逻辑，最后将SearchResponse封装在Result对象中返回给客户端。
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, path = "/image")
    public Result<SearchResponse> searchByImage(@ModelAttribute ImageSearchRequest request) throws IOException {
        SearchCommand command = buildImageSearchCommand(request);

        SearchResponse response = searchApplicationService.search(command);

        return Result.success(response, getRequestId());
    }

    //  接收TextSearchRequest对象，并构建SearchCommand对象，调用SearchApplicationService的search方法执行搜索逻辑，最后将SearchResponse封装在Result对象中返回给客户端。
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, path = "/text")
    public Result<SearchResponse> searchByText(@RequestBody TextSearchRequest request) {
        SearchCommand command = buildTextSearchCommand(request);

        SearchResponse response = searchApplicationService.search(command);

        return Result.success(response, getRequestId());
    }

    //  接收HybridSearchRequest对象，并构建SearchCommand对象，调用SearchApplicationService的search方法执行搜索逻辑，最后将SearchResponse封装在Result对象中返回给客户端。
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, path = "/hybrid")
    public Result<SearchResponse> searchByHybrid(@ModelAttribute HybridSearchRequest request) throws IOException {
        SearchCommand command = buildHybridSearchCommand(request);

        SearchResponse response = searchApplicationService.search(command);

        return Result.success(response, getRequestId());
    }

    /*
    构建SearchCommand对象，将接收到的ImageSearchRequest对象中的参数赋给SearchCommand对象，并返回该对象。
     */
    private SearchCommand buildImageSearchCommand(ImageSearchRequest request) throws IOException {
        MultipartFile queryImage = request == null ? null : request.getQueryImage();
        return SearchCommand.builder()
                .searchType(SearchType.IMAGE)
                .queryImage(isEmptyFile(queryImage) ? null : queryImage.getInputStream())
                .queryImageOriginalFileName(queryImage == null ? null : queryImage.getOriginalFilename())
                .queryImageFileSize(queryImage == null ? null : queryImage.getSize())
                .queryImageMimeType(queryImage == null ? null : queryImage.getContentType())
                .topK(request == null ? null : request.getTopK())
                .traceId(getTraceId())
                .requestId(getRequestId())
                .build();
    }

    //构建SearchCommand对象，将接收到的TextSearchRequest对象中的参数赋给SearchCommand对象，并返回该对象。
    private SearchCommand buildTextSearchCommand(TextSearchRequest request) {
        return SearchCommand.builder()
                .searchType(SearchType.TEXT)
                .queryText(request == null ? null : request.getQueryText())
                .topK(request == null ? null : request.getTopK())
                .traceId(getTraceId())
                .requestId(getRequestId())
                .build();
    }

    //构建SearchCommand对象，将接收到的HybridSearchRequest对象中的参数赋给SearchCommand对象，并返回该对象。
    private SearchCommand buildHybridSearchCommand(HybridSearchRequest request) throws IOException {
        MultipartFile queryImage = request == null ? null : request.getQueryImage();
        return SearchCommand.builder()
                .searchType(SearchType.HYBRID)
                .queryImage(isEmptyFile(queryImage) ? null : queryImage.getInputStream())
                .queryImageOriginalFileName(queryImage == null ? null : queryImage.getOriginalFilename())
                .queryImageFileSize(queryImage == null ? null : queryImage.getSize())
                .queryImageMimeType(queryImage == null ? null : queryImage.getContentType())
                .queryText(request == null ? null : request.getQueryText())
                .topK(request == null ? null : request.getTopK())
                .traceId(getTraceId())
                .requestId(getRequestId())
                .build();
    }

    private boolean isEmptyFile(MultipartFile file) {
        return file == null || file.isEmpty() || file.getSize() <= 0;
    }

    private String getRequestId() {
        RequestContext context = RequestContextHolder.get();
        return context == null ? null : context.getRequestId();
    }

    private String getTraceId() {
        RequestContext context = RequestContextHolder.get();
        return context == null ? null : context.getTraceId();
    }
}
