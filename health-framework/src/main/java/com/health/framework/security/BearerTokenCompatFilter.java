package com.health.framework.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 让 Authorization 头同时接受两种写法：
 *   - 裸 token：Authorization: 60_NdvwHtOot...    （本项目前端 request.js 的写法）
 *   - 标准 Bearer：Authorization: Bearer 60_Ndvw...（curl / 第三方对接的写法）
 *
 * 为什么需要：鉴权由 Sa-Token 负责，配置为 token-name: Authorization 且未设 token-prefix，
 * 于是它把整串头的值当作 token。线上实测：
 *   Authorization: &lt;token&gt;          -> 200
 *   Authorization: Bearer &lt;token&gt;   -> 401（不是没权限，而是 token 字符串里多了 "Bearer "）
 * 按 RFC 6750 习惯写 Bearer 的对接方会直接踩坑。
 *
 * 为什么放在 health-framework：应用启动类是 com.health.web.HealthApplication
 * （scanBasePackages = "com.health"），只有 com.health 下的 Bean 会被装配；
 * 放在 com.chuanzhi.* 或未参与构建的模块里都不会生效（此前踩过）。
 *
 * 为什么用 Filter 而不是改 Sa-Token 配置：设 token-prefix: Bearer 会让现有前端（发裸 token）
 * 全部 401。Filter 在最外层把前缀剥掉，两种写法都能过。删除本类即回到"只认裸 token"。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BearerTokenCompatFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header == null || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            filterChain.doFilter(request, response);
            return;
        }
        final String stripped = header.substring(PREFIX.length()).trim();
        if (stripped.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return HEADER.equalsIgnoreCase(name) ? stripped : super.getHeader(name);
            }
        };
        filterChain.doFilter(wrapper, response);
    }
}