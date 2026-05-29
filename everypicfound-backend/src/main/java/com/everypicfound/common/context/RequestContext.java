package com.everypicfound.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {
    /**
     * 当前请求 ID，通常每次 HTTP 请求唯一。
     */
    private String requestId;

    /**
     * 链路追踪 ID，可由上游传入，也可由当前服务生成。
     */
    private String traceId;

    /**
     * 当前业务 ID，例如 imageId。请求入口阶段通常为空，业务处理中可补充。
     */
    private String bizId;


    /**
     * 当前业务模块，例如 imageasset、search、vectorization。
     */
    private String module;

    /**
     * 当前操作名称，例如 IMAGE_UPLOAD、IMAGE_SEARCH。
     */
    private String operation;
}
