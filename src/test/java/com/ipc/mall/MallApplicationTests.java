package com.ipc.mall;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文加载测试：确认 Spring 容器、数据源、MyBatis-Plus 都能正常装配。
 * 注意：数据源是懒连接，即使数据库还没建，这个测试通常也能通过；
 * 真正查库需要先执行 db/init.sql。
 */
@SpringBootTest
class MallApplicationTests {

    @Test
    void contextLoads() {
    }
}
