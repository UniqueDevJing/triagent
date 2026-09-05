package com.health.framework.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Sa-Token 登录态拦截 — 除了白名单路径外都需要登录
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error", "/api/v1/login", "/api/v1/captchaImage", "/api/v1/logout",
                        "/swagger-ui/**", "/v3/api-docs/**", "/doc.html", "/webjars/**");
    }
}
