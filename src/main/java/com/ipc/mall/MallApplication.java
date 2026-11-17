package com.ipc.mall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 星际和平商城 - 启动类
 *
 * @author IPC Team
 */
@SpringBootApplication
@MapperScan("com.ipc.mall.mapper")
public class MallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
        System.out.println("""

                ==================================================
                  星际和平商城 Interastral Peace Mall 启动成功
                  首页： http://localhost:8080/
                  自检： http://localhost:8080/ping
                ==================================================
                """);
    }
}
