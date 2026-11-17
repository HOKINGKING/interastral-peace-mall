package com.ipc.mall.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置（IoC 体现点：@Configuration + @Bean 把插件对象装配进 Spring 容器）
 * <p>
 * 1. 分页插件：商品列表分页必须，不注册 limit 不生效
 * 2. 乐观锁插件：后续订单扣减库存使用 @Version
 * <p>
 * 注意：多个内部拦截器有顺序要求，分页必须放在最后添加。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁（订单/库存并发控制）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页（放最后）
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);   // 单页最大条数，防止恶意传参
        pagination.setOverflow(false);  // 页码超出总页数时返回空列表而不是首页
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }
}
