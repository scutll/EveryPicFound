package com.everypicfound.common.filter;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.util.TraceIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 请求上下文初始化过滤器。
 * Spring自动扫描并注册到Web过滤链中
 * <p>该过滤器在每个请求进入 Controller 前初始化 RequestContext和MDC，
 * 并把 requestId、traceId 写入 MDC，便于后续日志统一携带链路字段。
 * 请求后进行清理
 * </p>
 */
@Component("everypicfoundRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)// 最高优先级：因为拦截器会拦截所有请求，所以需要最高优先级
@RequiredArgsConstructor
public class RequestContextFilter extends OncePerRequestFilter{

    private static final String HEADER_REQUEST_ID = "X-Request-Id";// 请求头中用于传递 requestId 的字段名
    private static final String HEADER_TRACE_ID = "X-Trace-Id";// 请求头中用于传递 traceId 的字段名

    private static final String MDC_REQUEST_ID = "requestId";// MDC 中用于保存 requestId 的字段名
    private static final String MDC_TRACE_ID = "traceId";// MDC 中用于保存 traceId 的字段名

    private static final String DEFAULT_MODULE = "unknown";// 默认模块名
    private static final String DEFAULT_OPERATION = "unknown";// 默认操作名

    private final TraceIdGenerator traceIdGenerator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        /*
         * Servlet 工作线程会被线程池复用。
         * 进入新请求时先主动清理，防止上一次请求异常退出后留下脏数据。
         */
        clearContext();

        try{
            String requestId = getOrGenerateRequestId(request.getHeader(HEADER_REQUEST_ID));
            String traceId = getOrGenerateTraceId(request.getHeader(HEADER_TRACE_ID));

            // 请求进入系统时：初始化上下文
            RequestContext requestContext = RequestContext.builder()
                    .requestId(requestId)
                    .traceId(traceId)
                    .module(DEFAULT_MODULE)
                    .operation(DEFAULT_OPERATION)
                    .build();

            bindContext(requestContext);

            response.setHeader(HEADER_REQUEST_ID, requestId);
            response.setHeader(HEADER_TRACE_ID, traceId);

            filterChain.doFilter(request, response);

        } finally {
            clearContext();
        }

        

    }

    private void bindContext(RequestContext requestContext) {
        RequestContextHolder.set(requestContext);

        putMdcValue(MDC_REQUEST_ID, requestContext.getRequestId());
        putMdcValue(MDC_TRACE_ID, requestContext.getTraceId());
    }

    private void putMdcValue(String key, String value) {
        if (StringUtils.hasText(value)) {
            MDC.put(key, value);
        } else {
            MDC.remove(key);
        }
    }

            
    private void clearContext() {
        RequestContextHolder.clear();
        MDC.clear();
    }

    private String getOrGenerateRequestId(String headerValue) {
        if (StringUtils.hasText(headerValue)) {
            return headerValue.trim();
        }
        return traceIdGenerator.generateRequestId();
    }

    private String getOrGenerateTraceId(String headerValue) {
        if (StringUtils.hasText(headerValue)) {
            return headerValue.trim();
        }
        return traceIdGenerator.generateTraceId();
    }
}
