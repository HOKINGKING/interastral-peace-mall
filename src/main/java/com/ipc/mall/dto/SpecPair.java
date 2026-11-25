package com.ipc.mall.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品参数的一对 key-value（PRD §5.4：≤8 组，落库为 {@code product.specs} JSON）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecPair {

    @Size(max = 30, message = "参数名最多 30 个字符")
    private String key;

    @Size(max = 100, message = "参数值最多 100 个字符")
    private String value;
}
