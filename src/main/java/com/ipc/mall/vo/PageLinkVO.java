package com.ipc.mall.vo;

import lombok.Data;

/**
 * 分页条中的一个页码（ellipsis=true 时渲染为省略号）。
 */
@Data
public class PageLinkVO {

    private final long no;

    private final String url;

    private final boolean current;

    private final boolean ellipsis;

    public static PageLinkVO page(long no, String url, boolean current) {
        return new PageLinkVO(no, url, current, false);
    }

    public static PageLinkVO ellipsis() {
        return new PageLinkVO(0, null, false, true);
    }
}
