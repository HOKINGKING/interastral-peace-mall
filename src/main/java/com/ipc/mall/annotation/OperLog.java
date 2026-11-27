package com.ipc.mall.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（PRD §6.7 / docs/api.md A27）。
 * <p>
 * 标注在 Controller 的<b>写操作</b>方法上，由 {@code OperLogAspect} 统一记录：
 * 操作人、模块、动作、目标 id、URI、参数（密码脱敏）、IP、耗时、成功与否。
 * <pre>
 * &#64;OperLog(module = "订单管理", action = "发货")
 * public Result&lt;Void&gt; ship(...)
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /** 模块名，如「订单管理」「用户管理」 */
    String module();

    /** 动作名，如「发货」「重置密码」 */
    String action();

    /**
     * 是否为登录类接口：登录时还没有后台会话，操作人取表单里的 {@code username}，
     * {@code admin_id} 按 PRD 记 0。
     */
    boolean login() default false;
}
