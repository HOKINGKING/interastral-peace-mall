package com.ipc.mall.vo;

import com.ipc.mall.entity.Account;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 个人中心展示对象（PRD §5.8）：手机号中间 4 位打码，时间格式化为页面字符串。
 */
@Data
public class AccountProfileVO {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String username;

    private String nickname;

    private String phoneMasked;

    private String createTimeText;

    private String lastLoginTimeText;

    public static AccountProfileVO of(Account account) {
        AccountProfileVO vo = new AccountProfileVO();
        vo.setUsername(account.getUsername());
        vo.setNickname(account.getNickname() == null || account.getNickname().isBlank()
                ? account.getUsername() : account.getNickname());
        vo.setPhoneMasked(maskPhone(account.getPhone()));
        vo.setCreateTimeText(format(account.getCreateTime(), "—"));
        vo.setLastLoginTimeText(format(account.getLastLoginTime(), "首次登录"));
        return vo;
    }

    /** 138****0001；未绑定或格式异常时给出可读文案 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "未绑定";
        }
        String value = phone.trim();
        if (value.length() == 11) {
            return value.substring(0, 3) + "****" + value.substring(7);
        }
        return value;
    }

    private static String format(LocalDateTime time, String fallback) {
        return time == null ? fallback : FORMATTER.format(time);
    }
}
