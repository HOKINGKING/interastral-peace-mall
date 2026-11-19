package com.ipc.mall.config;

import com.ipc.mall.interceptor.AdminLoginInterceptor;
import com.ipc.mall.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Spring MVC 配置：登录拦截器 + 上传目录静态映射（PRD §3.1 / §9）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminLoginInterceptor adminLoginInterceptor;

    @Value("${mall.upload.path:./uploads/}")
    private String uploadPath;

    public WebMvcConfig(LoginInterceptor loginInterceptor, AdminLoginInterceptor adminLoginInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.adminLoginInterceptor = adminLoginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 前台：根路径与 /** 都注册，避免不同 PathPattern 策略下 /cart 与 /cart/** 的边界差异
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(
                        "/cart", "/cart/**",
                        "/order", "/order/**",
                        "/address", "/address/**",
                        "/feedback", "/feedback/**",
                        "/user/center", "/user/password")
                .excludePathPatterns(
                        "/login", "/register", "/user/login", "/user/register", "/captcha",
                        "/css/**", "/js/**", "/img/**", "/favicon.ico", "/uploads/**", "/error");

        // 后台：/admin 本身与子路径都拦截；登录页、验证码放行
        registry.addInterceptor(adminLoginInterceptor)
                .addPathPatterns("/admin", "/admin/**")
                .excludePathPatterns(
                        "/admin/login", "/admin/captcha",
                        "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadPath).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
