package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户补充说明（PRD §5.15 / docs/api.md F38）：追加到 user_note，不改变状态。
 */
@Data
public class FeedbackNoteForm {

    @NotBlank(message = "请填写补充说明")
    @Size(max = 200, message = "补充说明最多 200 个字符")
    private String note;
}
