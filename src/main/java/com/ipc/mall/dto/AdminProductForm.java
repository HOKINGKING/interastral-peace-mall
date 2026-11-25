package com.ipc.mall.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 后台 SPU 表单（PRD §6.3 / docs/api.md A10–A11）。
 * <p>
 * 由页面以 JSON 提交：SPU 基本信息 + 相册 + 参数表 + SKU 子表一次性保存。
 * 图片本身先走 {@code POST /admin/upload/image} 拿到 URL，这里只带地址。
 */
@Data
public class AdminProductForm {

    private Long id;

    @NotNull(message = "请选择所属分类")
    private Long categoryId;

    @NotBlank(message = "请输入商品名称")
    @Size(max = 60, message = "商品名称最多 60 个字符")
    private String name;

    @Size(max = 30, message = "品牌最多 30 个字符")
    private String brand;

    @Size(max = 200, message = "短描述最多 200 个字符")
    private String descn;

    private String detail;

    @Size(max = 255, message = "主图地址过长")
    private String mainImage;

    /** 相册，逗号分隔的 URL，≤5 张 */
    @Size(max = 1000, message = "相册地址过长")
    private String images;

    /** 商品参数，≤8 组 */
    private List<SpecPair> specs = new ArrayList<>();

    /** 1 上架 / 0 下架 */
    private Integer status;

    /** SKU 子表：至少保留 1 条 */
    @Valid
    @NotEmpty(message = "至少保留一个 SKU")
    private List<AdminSkuForm> skus = new ArrayList<>();
}
