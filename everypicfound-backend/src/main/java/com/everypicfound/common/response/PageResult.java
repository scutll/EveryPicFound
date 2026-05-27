package com.everypicfound.common.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    // 总记录数。
    private Long total;

    // 当前页码。
    private Integer pageNo;

    // 每页大小。
    private Integer pageSize;

    // 当前页数据列表。
    private List<T> records;
}
