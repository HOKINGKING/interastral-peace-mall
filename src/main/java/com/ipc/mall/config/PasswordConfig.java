package com.ipc.mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置（PRD §5.5 / §11.1）。
 * <p>
 * 只依赖 spring-security-crypto，不引入 spring-boot-starter-security，
 * 避免全站被 Spring Security 的过滤器链接管。
 */
@Configuration
public class PasswordConfig {

    /** BCrypt 默认强度 10，与 db/data.sql 中的 $2a$10$ 密文一致 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
