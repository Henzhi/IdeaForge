package com.ideaforge.common.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 游标分页响应。配合 cursor + limit 使用,避免 offset 深翻页。
 */
@Data
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> items;
    private String nextCursor;
    private boolean hasMore;
    private Long total;
    private Long page;
    private Long size;

    public PageResponse(List<T> items, String nextCursor, boolean hasMore) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    private PageResponse(List<T> items, String nextCursor, boolean hasMore, Long total, Long page, Long size) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /** 游标分页 */
    public static <T> PageResponse<T> of(List<T> items, String nextCursor) {
        return new PageResponse<>(items, nextCursor, nextCursor != null);
    }

    /** MyBatis-Plus 数字分页 */
    public static <T> PageResponse<T> of(List<T> items, long total, long page, long size) {
        boolean hasMore = page * size < total;
        return new PageResponse<>(items, null, hasMore, total, page, size);
    }
}
