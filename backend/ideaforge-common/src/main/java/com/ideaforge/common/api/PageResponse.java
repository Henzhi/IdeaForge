package com.ideaforge.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 游标分页响应。配合 cursor + limit 使用,避免 offset 深翻页。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> items;
    private String nextCursor;
    private boolean hasMore;

    public static <T> PageResponse<T> of(List<T> items, String nextCursor) {
        return new PageResponse<>(items, nextCursor, nextCursor != null);
    }
}
