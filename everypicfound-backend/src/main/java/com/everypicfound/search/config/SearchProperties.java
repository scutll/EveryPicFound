package com.everypicfound.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "everypicfound.search")
public class SearchProperties {
    
    private Integer defaultTopK = 30;

    private Integer maxTopK = 50;

    private Integer overFetchRatio = 3;

    private Integer maxTopN = 200;

    private Boolean rerankEnabled = false;

    private Boolean cacheEnabled = false;

    private Integer maxQueryTextLength = 500;

    // 搜索结果缓存 TTL，单位：秒。
    private Long resultCacheTtlSeconds = 300L;

    // 是否缓存空搜索结果。
    private Boolean cacheEmptyResult = true;
}
