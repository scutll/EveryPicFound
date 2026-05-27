package com.everypicfound.common.metric;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricTags {

    // 指标标签。
    private Map<String, String> tags;
}
