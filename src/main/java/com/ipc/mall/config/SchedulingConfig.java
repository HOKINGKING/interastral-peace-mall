package com.ipc.mall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关（超时关单等，PRD T4 / P1）。
 * <p>
 * 单独建配置类而不是放在启动类上：后续加别的定时任务时职责更清楚；
 * 测试环境通过 {@code mall.order.timeout-enabled=false} 关掉任务本身。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
