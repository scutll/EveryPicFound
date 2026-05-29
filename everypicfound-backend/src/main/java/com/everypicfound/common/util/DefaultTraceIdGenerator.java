package com.everypicfound.common.util;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.stereotype.Component;
/*
DefaultTraceIdGenerator 只做三件事：

1. 生成 requestId
2. 生成 traceId
3. 保证格式统一、足够唯一、便于日志排查

它不应该做这些事：

不读 HTTP Header
不写 response header
不操作 MDC
不操作 RequestContextHolder
不判断业务模块 module / operation
不感知图片上传、向量化、搜索等业务
*/

/*生成规则建议为：
requestId: req-时间戳-随机串
traceId:   trace-时间戳-随机串
*/
@Component
public class DefaultTraceIdGenerator implements TraceIdGenerator {

    private static final String REQUEST_ID_PREFIX = "req";

    private static final String TRACE_ID_PREFIX = "trace";

    private static final int RANDOM_HEX_LENGTH = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();// 用于生成随机数


    @Override
    // 生成单次请求 ID。
    public String generateRequestId() {
        return generateId(REQUEST_ID_PREFIX);
    }

    @Override
    // 生成链路追踪 ID。
    public String generateTraceId() {
        return generateId(TRACE_ID_PREFIX);
    }

    private String generateId(String prefix) {
        return prefix + "-" + Instant.now().toEpochMilli() + "-" + generateRandomHex();
    }

    private String generateRandomHex() {
        StringBuilder builder = new StringBuilder(RANDOM_HEX_LENGTH);

        for (int i = 0; i < RANDOM_HEX_LENGTH; i++) {
            builder.append(Integer.toHexString(SECURE_RANDOM.nextInt(16)));
        }

        return builder.toString();
    }
}
