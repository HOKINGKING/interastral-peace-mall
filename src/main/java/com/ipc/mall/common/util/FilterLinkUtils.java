package com.ipc.mall.common.util;

import com.ipc.mall.dto.ProductQuery;
import com.ipc.mall.vo.FilterLinkVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 排序项链接（综合 / 价格升 / 价格降 / 最新），列表页与分类页共用。
 */
public final class FilterLinkUtils {

    private static final String[][] SORTS = {
            {ProductQuery.SORT_DEFAULT, "综合"},
            {ProductQuery.SORT_PRICE_ASC, "价格 ↑"},
            {ProductQuery.SORT_PRICE_DESC, "价格 ↓"},
            {ProductQuery.SORT_NEWEST, "最新"},
    };

    private FilterLinkUtils() {
    }

    public static List<FilterLinkVO> sortLinks(String basePath, Map<String, Object> params, String currentSort) {
        List<FilterLinkVO> links = new ArrayList<>(SORTS.length);
        for (String[] option : SORTS) {
            String value = option[0];
            Map<String, Object> sortParams = new LinkedHashMap<>(params);
            sortParams.put("sort", ProductQuery.SORT_DEFAULT.equals(value) ? null : value);
            links.add(FilterLinkVO.of(option[1], PageUtils.filterUrl(basePath, sortParams), value.equals(currentSort)));
        }
        return links;
    }
}
