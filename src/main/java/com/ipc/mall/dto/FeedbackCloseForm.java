package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 卖家关闭投诉/反馈（PRD §6.6 / docs/api.md A32）：未关闭状态均可关闭，原因必填 ≤100 字。
 */
@Data
public class FeedbackCloseForm {

    @NotBlank(message = "请填写关闭原因")
    @Size(max = 100, message = "关闭原因最多 100 个字符")
    private String closeReason;
}
