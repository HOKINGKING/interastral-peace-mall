package com.ipc.mall.vo;

import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.entity.Account;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 后台用户列表行（PRD §6.4 / docs/api.md A18）。
 * <p>
 * 列表只展示打码手机号；完整手机号只在详情页（A19）出现，供编辑表单回填。
 */
@Data
public class AdminAccountRowVO {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;

    private String username;

    private String nickname;

    private String phoneMasked;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;

    public static AdminAccountRowVO of(Account account) {
        AdminAccountRowVO vo = new AdminAccountRowVO();
        vo.setId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setNickname(account.getNickname());
        vo.setPhoneMasked(AccountProfileVO.maskPhone(account.getPhone()));
        vo.setStatus(account.getStatus());
        vo.setCreateTime(account.getCreateTime());
        vo.setLastLoginTime(account.getLastLoginTime());
        return vo;
    }

    /** 昵称为空时回落用户名，列表不出现空白单元格 */
    public String getDisplayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }

    public String getStatusDesc() {
        return AccountStatus.isEnabled(status) ? "启用" : "禁用";
    }

    /** 复用 style.css 里已有的 badge 配色：启用=绿色，禁用=红色 */
    public String getStatusClass() {
        return AccountStatus.isEnabled(status) ? "badge-completed" : "badge-danger";
    }

    public String getCreateTimeText() {
        return createTime == null ? "" : MINUTE.format(createTime);
    }

    public String getLastLoginTimeText() {
        return lastLoginTime == null ? "从未登录" : MINUTE.format(lastLoginTime);
    }
}
