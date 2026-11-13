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
                        "/swagger-ui/**", "/v3/api-docs/**", "/doc.html", "/webjars/**",
                        // A2A AgentCard 发现：只暴露静态能力自述，不含会话/患者数据
                        "/.well-known/agent.json",
                        // MCP 工具层协议端点：按 MCP 规范由客户端自行握手，
                        // 仅发布只读确定性工具；带状态的写操作工具不对外暴露
                        "/sse", "/mcp/message", "/mcp");
    }
}
