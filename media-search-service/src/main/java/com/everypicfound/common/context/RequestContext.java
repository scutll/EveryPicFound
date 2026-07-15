package com.everypicfound.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {
    
    private String requestId;

    private String traceId;

    private String bizId;

    private String module;

    private String operation;
}
