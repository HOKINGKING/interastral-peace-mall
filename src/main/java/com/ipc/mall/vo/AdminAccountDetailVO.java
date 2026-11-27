package com.ipc.mall.vo;

import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.entity.Account;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 后台用户详情（PRD §6.4 / docs/api.md A19）。
 * <p>
 * 比列表多两个后台专用字段：
 * <ul>
 *     <li>{@code phone}：完整手机号，仅用于「编辑」表单回填；</li>
 *     <li>{@code orderCount}：该用户的订单数（逻辑删除的订单不计）。</li>
 * </ul>
 */
@Data
public class AdminAccountDetailVO {

    private static final DateTimeFormatter SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;

    private String username;

    private String nickname;

    /** 完整手机号，仅后台详情页使用 */
    private String phone;

    private String phoneMasked;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;

    /** 未删除的订单数 */
    private long orderCount;

    public static AdminAccountDetailVO of(Account account, long orderCount) {
        AdminAccountDetailVO vo = new AdminAccountDetailVO();
        vo.setId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setNickname(account.getNickname());
        vo.setPhone(account.getPhone());
        vo.setPhoneMasked(AccountProfileVO.maskPhone(account.getPhone()));
        vo.setStatus(account.getStatus());
        vo.setCreateTime(account.getCreateTime());
        vo.setLastLoginTime(account.getLastLoginTime());
        vo.setOrderCount(orderCount);
        return vo;
    }

    public String getDisplayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }

    public String getStatusDesc() {
        return AccountStatus.isEnabled(status) ? "启用" : "禁用";
    }

    public String getStatusClass() {
        return AccountStatus.isEnabled(status) ? "badge-completed" : "badge-danger";
    }

    public String getCreateTimeText() {
        return createTime == null ? "—" : SECOND.format(createTime);
    }

    public String getLastLoginTimeText() {
        return lastLoginTime == null ? "从未登录" : SECOND.format(lastLoginTime);
    }
}
