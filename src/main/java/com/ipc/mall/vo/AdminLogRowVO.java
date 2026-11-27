package com.ipc.mall.vo;

import com.ipc.mall.entity.SysOperLog;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志列表行（PRD §6.7 / docs/api.md A27）。
 * <p>
 * 参数列在列表里只展示截断后的文本，完整值放在 {@code title} 里由鼠标悬停查看；
 * 失败行额外展示错误原因。
 */
@Data
public class AdminLogRowVO {

    private static final DateTimeFormatter SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PARAMS_PREVIEW = 80;

    private Long id;

    private Long adminId;

    private String adminName;

    private String module;

    private String action;

    private Long targetId;

    private String requestUri;

    private String requestMethod;

    private String requestParams;

    private String ip;

    private Integer success;

    private String errorMsg;

    private Integer costMs;

    private LocalDateTime createTime;

    public static AdminLogRowVO of(SysOperLog log) {
        AdminLogRowVO vo = new AdminLogRowVO();
        vo.setId(log.getId());
        vo.setAdminId(log.getAdminId());
        vo.setAdminName(log.getAdminName());
        vo.setModule(log.getModule());
        vo.setAction(log.getAction());
        vo.setTargetId(log.getTargetId());
        vo.setRequestUri(log.getRequestUri());
        vo.setRequestMethod(log.getRequestMethod());
        vo.setRequestParams(log.getRequestParams());
        vo.setIp(log.getIp());
        vo.setSuccess(log.getSuccess());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setCostMs(log.getCostMs());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    public String getCreateTimeText() {
        return createTime == null ? "" : SECOND.format(createTime);
    }

    public boolean isFailed() {
        return success == null || success == 0;
    }

    public String getResultDesc() {
        return isFailed() ? "失败" : "成功";
    }

    public String getResultClass() {
        return isFailed() ? "badge-danger" : "badge-completed";
    }

    public String getTargetText() {
        return targetId == null ? "—" : "#" + targetId;
    }

    public String getCostText() {
        return costMs == null ? "—" : costMs + " ms";
    }

    /** 列表展示用：超长参数截断，完整值仍可通过 getRequestParams() 给 title 用 */
    public String getParamsPreview() {
        if (requestParams == null || requestParams.isBlank()) {
            return "—";
        }
        return requestParams.length() <= PARAMS_PREVIEW
                ? requestParams
                : requestParams.substring(0, PARAMS_PREVIEW) + "…";
    }
}
