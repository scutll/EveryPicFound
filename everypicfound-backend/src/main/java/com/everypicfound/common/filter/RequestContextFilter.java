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
 *
 * <p>该过滤器在每个请求进入 Controller 前初始化 RequestContext，
 * 并把 requestId、traceId 写入 MDC，便于后续日志统一携带链路字段。</p>
 */
@Component("everypicfoundRequestContextFilter")//修改1
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
        String requestId = getOrGenerateRequestId(request.getHeader(HEADER_REQUEST_ID));
        String traceId = getOrGenerateTraceId(request.getHeader(HEADER_TRACE_ID));
        // 请求进入系统时：初始化上下文
        RequestContext requestContext = RequestContext.builder()
                .requestId(requestId)
                .traceId(traceId)
                .module(DEFAULT_MODULE)
                .operation(DEFAULT_OPERATION)
                .build();

                try{
                     // 放行请求，让 Controller 继续执行
                    RequestContextHolder.set(requestContext);//设置请求上下文
                    MDC.put(MDC_REQUEST_ID, requestId);//设置MDC的requestId
                    MDC.put(MDC_TRACE_ID, traceId);//设置MDC的traceId

                    response.setHeader(HEADER_REQUEST_ID, requestId);//设置响应头requestId
                    response.setHeader(HEADER_TRACE_ID, traceId);//设置响应头traceId

                    filterChain.doFilter(request, response);//执行下一个过滤器
                }finally{
                    // 请求结束时：清理上下文
                    RequestContextHolder.clear();//清除请求上下文
                    MDC.remove(MDC_REQUEST_ID);//清除MDC的requestId
                    MDC.remove(MDC_TRACE_ID);//清除MDC的traceId
                }

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
