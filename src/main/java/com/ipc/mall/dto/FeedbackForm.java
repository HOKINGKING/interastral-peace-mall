package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 投诉 / 反馈提交表单（PRD §5.15 / docs/api.md F33）。
 * <p>
 * 同一张表单：{@code orderId} 有值即为投诉（Service 会强制 type=COMPLAINT），
 * 为空则是普通反馈。内容长度下限按类型在 Service 里二次校验（投诉 10 字、反馈 5 字）。
 */
@Data
public class FeedbackForm {

    /** 投诉时的订单 id；反馈留空 */
    private Long orderId;

    /** QUALITY / LOGISTICS / SERVICE / OTHER，投诉必填 */
    @Size(max = 20, message = "分类参数非法")
    private String category;

    @NotBlank(message = "请填写内容")
    @Size(min = 5, max = 500, message = "内容需为 5–500 字")
    private String content;

    /** 选填，≤50 字 */
    @Size(max = 50, message = "联系方式最多 50 个字符")
    private String contact;

    /** 【P1】逗号分隔的图片 URL，≤3 张；本期表单不提供上传，仅透传 */
    @Size(max = 500, message = "图片地址过长")
    private String images;
}
