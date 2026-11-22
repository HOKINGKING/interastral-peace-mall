package com.ipc.mall.dto;

import lombok.Data;

/**
 * 商品列表 / 分类页 / 搜索的查询条件（PRD §5.3）。
 * <p>
 * 参数名与前端 URL 保持一致，Spring MVC 直接按字段绑定；
 * 绑定后调用 {@link #normalize()} 统一兜底（页码、页大小、关键词长度、排序白名单）。
 */
@Data
public class ProductQuery {

    public static final String SORT_DEFAULT = "default";
    public static final String SORT_PRICE_ASC = "priceAsc";
    public static final String SORT_PRICE_DESC = "priceDesc";
    public static final String SORT_NEWEST = "newest";

    /** 关键词最大长度（PRD §5.3） */
    private static final int KEYWORD_MAX_LENGTH = 50;
    private static final int SIZE_DEFAULT = 12;
    private static final int SIZE_MAX = 60;

    /** 分类筛选：一级分类会展开为其下所有二级分类 */
    private Long categoryId;

    /** 关键词：匹配商品名与短描述，trim 后 ≤50 字 */
    private String keyword;

    /** 仅看有货：该 SPU 至少一个 SKU 库存 > 0 */
    private boolean onlyInStock;

    /** 排序：default / priceAsc / priceDesc / newest */
    private String sort = SORT_DEFAULT;

    private long page = 1;

    private long size = SIZE_DEFAULT;

    /**
     * 规范化参数：页码至少 1，页大小限制在 1–60，关键词 trim 并截断，
     * 非法排序回落到默认排序（排序字段走白名单，禁止拼接 SQL）。
     */
    public void normalize() {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = SIZE_DEFAULT;
        }
        if (size > SIZE_MAX) {
            size = SIZE_MAX;
        }
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.length() > KEYWORD_MAX_LENGTH) {
                keyword = keyword.substring(0, KEYWORD_MAX_LENGTH);
            }
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }
        if (!isSupportedSort(sort)) {
            sort = SORT_DEFAULT;
        }
    }

    public static boolean isSupportedSort(String value) {
        return SORT_DEFAULT.equals(value)
                || SORT_PRICE_ASC.equals(value)
                || SORT_PRICE_DESC.equals(value)
                || SORT_NEWEST.equals(value);
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isEmpty();
    }
}
