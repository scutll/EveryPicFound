package com.everypicfound.search.application.service;

import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogService;
import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.application.pipeline.SearchPipeline;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultSearchApplicationService implements SearchApplicationService {
    
    private static final String MODULE = "search";

    private static final String BIZ_TYPE = "SEARCH";

    private static final String OPERATION = "search";

    private final SearchPipeline searchPipeline;

    private final LogService logService;


    @Override
    public SearchResponse search(SearchCommand command) {
        long startTime = System.currentTimeMillis();

        recordStartLog(command);

        try{
            SearchResponse response = searchPipeline.execute(command);
            recordSuccessLog(command, System.currentTimeMillis() - startTime);
            return response;
        } catch (BizException e) {
            recordErrorLog(command, System.currentTimeMillis() - startTime, String.valueOf(e.getErrorCode().getCode()),
                    e.getMessage());
            throw e;
        } catch (Exception e) {
            recordErrorLog(command, System.currentTimeMillis() - startTime, "SYSTEM ERROR", e.getMessage());
            throw e;
        }
    }

    private void recordStartLog(SearchCommand command) {
        logService.recordBizLog(
                LogContext.builder()
                        .requestId(command == null ? null : command.getRequestId())
                        .traceId(command == null ? null : command.getTraceId())
                        .bizType(BIZ_TYPE)
                        .module(MODULE)
                        .operation(OPERATION)
                        .eventName("SEARCH_START")
                        .status("START")
                        .message(command == null
                                ? "search start"
                                : "search start, type=" + command.getSearchType())
                        .build()
        );
    }

    private void recordSuccessLog(SearchCommand command, Long costMs) {
        logService.recordSuccessLog(
                LogContext.builder()
                        .requestId(command == null ? null : command.getRequestId())
                        .traceId(command == null ? null : command.getTraceId())
                        .bizType(BIZ_TYPE)
                        .module(MODULE)
                        .operation(OPERATION)
                        .eventName("SEARCH_SUCCESS")
                        .status("SUCCESS")
                        .costMs(costMs)
                        .errorCode(null)
                        .message(command == null
                                ? "search success"
                                : "search success, msg=" + command.getSearchType())
                        .build()
        );
    }

    private void recordErrorLog(SearchCommand command,
                                Long costMs,
                                String errorCode,
                                String message) {
        logService.recordErrorLog(
                LogContext.builder()
                        .requestId(command == null ? null : command.getRequestId())
                        .traceId(command == null ? null : command.getTraceId())
                        .bizType(BIZ_TYPE)
                        .module(MODULE)
                        .operation(OPERATION)
                        .eventName("SEARCH_FAILED")
                        .status("FAILED")
                        .costMs(costMs)
                        .errorCode(errorCode)
                        .message(message)
                        .build()
        );
    }
}
