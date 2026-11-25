package com.ipc.mall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台分类表单（PRD §6.2 / docs/api.md A7）。
 * <p>
 * {@code parentId=0} 表示一级分类；{@code visible} 用 String 接收 checkbox 的 "on"，
 * 由 Service 统一归一化成 0/1。
 */
@Data
public class CategoryForm {

    private Long id;

    /** 0 = 一级分类；否则为某个一级分类 id */
    private Long parentId;

    @NotBlank(message = "请输入分类名称")
    @Size(max = 30, message = "分类名称最多 30 个字符")
    private String name;

    @Size(max = 200, message = "分类描述最多 200 个字符")
    private String descn;

    @Min(value = 0, message = "排序值不能为负数")
    private Integer sort;

    /** "1" / "on" / null */
    private String visible;
}
