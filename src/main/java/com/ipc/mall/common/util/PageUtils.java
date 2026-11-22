package com.ipc.mall.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.vo.PageLinkVO;
import com.ipc.mall.vo.PaginationVO;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分页与筛选 URL 工具。
 * <p>
 * 模板不拼查询串：页码链接、排序链接都由服务端用 {@link UriComponentsBuilder} 生成，
 * 参数统一 URL 编码，避免中文关键词与 &amp; 拼接错误。
 */
public final class PageUtils {

    private PageUtils() {
    }

    /**
     * 生成分页条：最多显示 7 个槽位，超出时用省略号收中间段。
     */
    public static PaginationVO pagination(IPage<?> page, String basePath, Map<String, Object> params) {
        PaginationVO vo = new PaginationVO();
        long totalPages = page.getPages();
        long current = page.getCurrent();
        vo.setCurrent(current);
        vo.setTotalPages(totalPages);
        vo.setTotal(page.getTotal());
        vo.setHasPrev(current > 1);
        vo.setHasNext(current < totalPages);
        vo.setPrevUrl(url(basePath, params, Math.max(1, current - 1)));
        vo.setNextUrl(url(basePath, params, Math.max(1, Math.min(totalPages, current + 1))));

        List<PageLinkVO> links = new ArrayList<>();
        if (totalPages <= 7) {
            for (long i = 1; i <= totalPages; i++) {
                links.add(PageLinkVO.page(i, url(basePath, params, i), i == current));
            }
        } else {
            links.add(PageLinkVO.page(1, url(basePath, params, 1), current == 1));
            long start = Math.max(2, current - 1);
            long end = Math.min(totalPages - 1, current + 1);
            if (start > 2) {
                links.add(PageLinkVO.ellipsis());
            }
            for (long i = start; i <= end; i++) {
                links.add(PageLinkVO.page(i, url(basePath, params, i), i == current));
            }
            if (end < totalPages - 1) {
                links.add(PageLinkVO.ellipsis());
            }
            links.add(PageLinkVO.page(totalPages, url(basePath, params, totalPages), current == totalPages));
        }
        vo.setLinks(links);
        return vo;
    }

    /** 带 page 参数的 URL，page 固定放在最后 */
    public static String url(String basePath, Map<String, Object> params, long pageNo) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        append(builder, params);
        builder.queryParam("page", pageNo);
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    /** 不带 page 参数的 URL（筛选、排序、清空都用它） */
    public static String filterUrl(String basePath, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        append(builder, params);
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    /** 复制参数并覆盖若干键，null 值表示移除 */
    public static Map<String, Object> with(Map<String, Object> params, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(params);
        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }
        return copy;
    }

    public static Map<String, Object> without(Map<String, Object> params, String key) {
        return with(params, key, null);
    }

    private static void append(UriComponentsBuilder builder, Map<String, Object> params) {
        if (params == null) {
            return;
        }
        params.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            if (value instanceof Boolean flag && !flag) {
                return;
            }
            if (value instanceof String text && text.isEmpty()) {
                return;
            }
            builder.queryParam(key, value);
        });
    }
}
