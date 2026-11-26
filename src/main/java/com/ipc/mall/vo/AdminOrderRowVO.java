package com.ipc.mall.vo;

import com.ipc.mall.common.constant.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 后台订单列表行（PRD §6.5 / docs/api.md A22）：订单号、用户、件数、金额、状态、下单时间。
 */
@Data
public class AdminOrderRowVO {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;

    private String orderNo;

    private Long accountId;

    private String username;

    private String nickname;

    private BigDecimal totalAmount;

    private String totalAmountText;

    private String status;

    private String statusDesc;

    private String statusClass;

    /** 订单内商品总件数 */
    private Integer itemCount;

    private LocalDateTime createTime;

    public String getCreateTimeText() {
        return createTime == null ? "" : MINUTE.format(createTime);
    }

    public String getTotalAmountText() {
        return totalAmount == null ? "0.00" : totalAmount.toPlainString();
    }

    /** 联系展示：优先昵称，回落用户名 */
    public String getDisplayName() {
        return nickname != null && !nickname.isBlank() ? nickname : username;
    }

    public String getStatusDesc() {
        return OrderStatus.descOf(status);
    }

    public String getStatusClass() {
        return OrderSummaryVO.badgeClassOf(status);
    }
}
