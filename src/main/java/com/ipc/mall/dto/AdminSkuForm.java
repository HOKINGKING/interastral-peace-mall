package com.ipc.mall.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SKU 表单（PRD §6.3 / docs/api.md A11 的 skus[]、A15）。
 * <p>
 * {@code id} 为空=新增；{@code version} 用于改库存时的乐观锁校验，
 * 页面回填后原样带回，冲突时提示刷新重试。
 */
@Data
public class AdminSkuForm {

    private Long id;

    /** 新增时由 SPU 表单填；SKU 页面也会带，用于归属校验 */
    private Long productId;

    @NotBlank(message = "请输入 SKU 规格")
    @Size(max = 50, message = "规格最多 50 个字符")
    private String spec;

    @NotNull(message = "请输入 SKU 价格")
    @DecimalMin(value = "0.01", message = "价格需在 0.01–99999.99 之间")
    @DecimalMax(value = "99999.99", message = "价格需在 0.01–99999.99 之间")
    private BigDecimal price;

    @NotNull(message = "请输入 SKU 库存")
    @Min(value = 0, message = "库存需在 0–999999 之间")
    @Max(value = 999999, message = "库存需在 0–999999 之间")
    private Integer stock;

    @Size(max = 255, message = "图片地址过长")
    private String image;

    /** 乐观锁版本，新增时为空 */
    private Integer version;
}
