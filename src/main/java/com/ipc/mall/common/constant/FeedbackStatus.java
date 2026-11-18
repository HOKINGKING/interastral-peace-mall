package com.ipc.mall.common.constant;

import lombok.Getter;

/**
 * 投诉/反馈状态枚举（PRD §8.4）。
 * <pre>
 * PENDING ──受理──▶ PROCESSING ──回复──▶ REPLIED ──关闭──▶ CLOSED
 *    │                  │                  │
 *    └──用户撤销─────────┴────卖家关闭──────┴──▶ CLOSED（终态）
 * </pre>
 * 注意：REPLIED 不等于办结，仍需卖家关闭才进入终态。
 */
@Getter
public enum FeedbackStatus {

    PENDING("待处理"),
    PROCESSING("处理中"),
    REPLIED("已回复"),
    CLOSED("已关闭");

    private final String desc;

    FeedbackStatus(String desc) {
        this.desc = desc;
    }

    public boolean canTransitTo(FeedbackStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == PROCESSING || target == REPLIED || target == CLOSED;
            case PROCESSING -> target == REPLIED || target == CLOSED;
            case REPLIED -> target == CLOSED;
            case CLOSED -> false;
        };
    }

    public boolean isClosed() {
        return this == CLOSED;
    }

    public static FeedbackStatus of(String code) {
        for (FeedbackStatus s : values()) {
            if (s.name().equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("非法的投诉状态: " + code);
    }

    public static String descOf(String code) {
        for (FeedbackStatus s : values()) {
            if (s.name().equals(code)) {
                return s.desc;
            }
        }
        return code;
    }
}
