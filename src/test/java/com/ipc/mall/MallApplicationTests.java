package com.ipc.mall;

import com.ipc.mall.task.OrderTimeoutJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上下文加载测试：确认 Spring 容器、数据源、MyBatis-Plus 都能正常装配。
 * 注意：数据源是懒连接，即使数据库还没建，这个测试通常也能通过；
 * 真正查库需要先执行 db/init.sql。
 */
@SpringBootTest
class MallApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
    }

    /** 测试环境通过 src/test/resources/application.properties 关掉定时任务，避免后台线程查库干扰断言 */
    @Test
    void 测试环境不注册超时关单定时任务() {
        assertTrue(applicationContext.getBeansOfType(OrderTimeoutJob.class).isEmpty(),
                "mall.order.timeout-enabled=false 应让定时任务不注册");
    }
}
