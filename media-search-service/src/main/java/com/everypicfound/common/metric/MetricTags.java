package com.everypicfound.common.metric;

import java.util.Map;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Collections;


/**
 * 不可变指标标签集合。
 * 负责在业务中维护一组MetricTag，一个个key-value对
 */
public final class MetricTags {

    /**
     * 限制单个标签值长度，防止异常输入产生过大的指标元数据。
     */
    private static final int MAX_TAG_VALUE_LENGTH = 64;

    private static final MetricTags EMPTY = new MetricTags(Collections.emptyMap());

    private final Map<String, String> tags;

    public MetricTags(Map<String, String> tags) {
        this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
    }

    public static MetricTags empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取不可变标签 Map。
     */
    public Map<String, String> getTags() {
        return tags;
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }

    /**
     * 指标标签构造器。
     */
    public static final class Builder {

        private final Map<MetricTag, String> tags = new EnumMap<>(MetricTag.class);

        /**
         * 推荐使用的标准标签添加方式。
         * 通过.tag().tag...build()的方式添加标签
         */
        public Builder tag(MetricTag metricTag, String value) {
            if (metricTag == null) {
                return this;
            }

            String normalizedValue = normalizeValue(value);
            if (normalizedValue == null) {
                return this;
            }

            tags.put(metricTag, normalizedValue);
            return this;
        }
        
        public Builder tags(Map<String, String> sourceTags) {
            if (sourceTags == null || sourceTags.isEmpty()) {
                return this;
            }

            sourceTags.forEach((key, value) -> {
                MetricTag metricTag = MetricTag.fromKey(key);
                if (metricTag != null) {
                    tag(metricTag, value);
                }
            });

            return this;
        }

        public MetricTags build() {
            if (tags.isEmpty()) {
                return MetricTags.empty();
            }

            Map<String, String> result = new LinkedHashMap<>();

            tags.forEach((metricTag, value) -> {
                result.put(metricTag.getKey(), value);
            });

            return new MetricTags(result);
        }


        //消除首位空格、换行符、切分长度到LIMIT以内
        private String normalizeValue(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }

            String normalized = value.trim()
                    .replace('\r', ' ')
                    .replace('\n', ' ');

            if (normalized.length() <= MAX_TAG_VALUE_LENGTH) {
                return normalized;
            }

            return normalized.substring(0, MAX_TAG_VALUE_LENGTH);
        }
    }


}
