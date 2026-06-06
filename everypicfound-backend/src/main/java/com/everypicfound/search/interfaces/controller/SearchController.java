package com.everypicfound.search.interfaces.controller;

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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchApplicationService searchApplicationService;

    public SearchController(SearchApplicationService searchApplicationService) {
        this.searchApplicationService = searchApplicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, path = "/image")
    public Result<SearchResponse> searchByImage(@ModelAttribute ImageSearchRequest request) throws IOException {
        SearchCommand command = buildImageSearchCommand(request);

        SearchResponse response = searchApplicationService.search(command);

        return Result.success(response, getRequestId());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, path = "/text")
    public Result<SearchResponse> searchByText(@RequestBody TextSearchRequest request) {
        SearchCommand command = buildTextSearchCommand(request);

        SearchResponse response = searchApplicationService.search(command);

        return Result.success(response, getRequestId());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, path = "/hybrid")
    public Result<SearchResponse> searchByHybrid(@ModelAttribute HybridSearchRequest request) throws IOException {
        SearchCommand command = buildHybridSearchCommand(request);

        SearchResponse response = searchApplicationService.search(command);

        return Result.success(response, getRequestId());
    }

    private SearchCommand buildImageSearchCommand(ImageSearchRequest request) throws IOException {
        return SearchCommand.builder()
                .searchType(SearchType.IMAGE)
                .queryImage(request.getQueryImage().getInputStream())
                .queryImageOriginalFileName(request.getQueryImage().getOriginalFilename())
                .queryImageFileSize(request.getQueryImage().getSize())
                .queryImageMimeType(request.getQueryImage().getContentType())
                .topK(request.getTopK())
                .traceId(getTraceId())
                .requestId(getRequestId())
                .build();
    }

    private SearchCommand buildTextSearchCommand(TextSearchRequest request) {
        return SearchCommand.builder()
                .searchType(SearchType.TEXT)
                .queryText(request.getQueryText())
                .topK(request.getTopK())
                .traceId(getTraceId())
                .requestId(getRequestId())
                .build();
    }

    private SearchCommand buildHybridSearchCommand(HybridSearchRequest request) throws IOException {
        return SearchCommand.builder()
                .searchType(SearchType.HYBRID)
                .queryImage(request.getQueryImage().getInputStream())
                .queryImageOriginalFileName(request.getQueryImage().getOriginalFilename())
                .queryImageFileSize(request.getQueryImage().getSize())
                .queryImageMimeType(request.getQueryImage().getContentType())
                .queryText(request.getQueryText())
                .topK(request.getTopK())
                .traceId(getTraceId())
                .requestId(getRequestId())
                .build();
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
