package com.ipc.mall.vo;

import lombok.Data;

import java.util.List;

/**
 * 分页条视图模型：URL 全部在服务端拼好，模板只负责渲染。
 */
@Data
public class PaginationVO {

    private long current;

    private long totalPages;

    private long total;

    private String prevUrl;

    private String nextUrl;

    private boolean hasPrev;

    private boolean hasNext;

    private List<PageLinkVO> links;
}
