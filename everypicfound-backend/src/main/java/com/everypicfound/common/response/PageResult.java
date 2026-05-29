package com.everypicfound.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private Long total;

    private Integer pageNo;

    private Integer pageSize;

    private List<T> records;

    public static <T> PageResult<T> empty(Integer pageNo, Integer pageSize) {
        return new PageResult<>(0L, pageNo, pageSize, Collections.emptyList());
    }
}