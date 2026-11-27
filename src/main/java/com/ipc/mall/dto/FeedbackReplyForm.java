package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 卖家回复（PRD §6.6 / docs/api.md A31）：1–500 字必填，回复后状态转 REPLIED。
 */
@Data
public class FeedbackReplyForm {

    @NotBlank(message = "请填写回复内容")
    @Size(max = 500, message = "回复内容最多 500 个字符")
    private String reply;
}
