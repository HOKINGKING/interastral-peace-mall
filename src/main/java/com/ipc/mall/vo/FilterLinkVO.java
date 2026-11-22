package com.ipc.mall.vo;

import lombok.Data;

/**
 * 通用链接（排序项、已选筛选项）渲染载体。
 */
@Data
public class FilterLinkVO {

    private final String label;

    private final String url;

    private final boolean active;

    public static FilterLinkVO of(String label, String url, boolean active) {
        return new FilterLinkVO(label, url, active);
    }
}
