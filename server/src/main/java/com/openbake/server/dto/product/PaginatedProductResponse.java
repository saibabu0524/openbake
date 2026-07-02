package com.openbake.server.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PaginatedProductResponse {

    private final List<ProductResponse> items;
    private final long total;
    private final int page;

    @JsonProperty("page_size")
    private final int pageSize;

    private final long pages;

    @JsonProperty("has_next")
    private final boolean hasNext;

    @JsonProperty("has_prev")
    private final boolean hasPrev;

    public PaginatedProductResponse(List<ProductResponse> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.pages = Math.max(1, (long) Math.ceil((double) total / pageSize));
        this.hasNext = (long) page * pageSize < total;
        this.hasPrev = page > 1;
    }

    public List<ProductResponse> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getPages() { return pages; }
    public boolean isHasNext() { return hasNext; }
    public boolean isHasPrev() { return hasPrev; }
}
