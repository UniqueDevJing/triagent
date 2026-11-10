package com.health.web.assistant.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.domain.SysUser;
import com.health.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 演示环境安全化装配（T8）。仅在 triagent.demo.enabled=true 时生效。
 *
 * 拦截 /api/** 全部请求：演示只读账号禁止一切非白名单写操作（403），
 * 聊天接口超出每日额度直接拒绝（429）。非演示账号零开销直通。
 */
@Configuration
@ConditionalOnProperty(prefix = "triagent.demo", name = "enabled", havingValue = "true")
public class DemoWebSecurityConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(DemoWebSecurityConfig.class);

    private final DemoProperties properties;
    private final SysUserMapper userMapper;

    public DemoWebSecurityConfig(DemoProperties properties, SysUserMapper userMapper) {
        this.properties = properties;
        this.userMapper = userMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 启动时一次性解析演示账号 userId，请求期零查库
        Set<Long> demoUserIds = properties.getReadonlyUsers().stream()
                .map(name -> userMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, name)))
                .filter(Objects::nonNull)
                .peek(u -> log.info("演示只读账号已注册: {} -> userId={}", u.getUserName(), u.getId()))
                .map(SysUser::getId)
                .collect(Collectors.toSet());
        if (demoUserIds.isEmpty()) {
            log.error("演示模式已启用但未找到任何只读账号，请先执行 sql/demo_account.sql 创建 visitor 账号");
        }
        DemoAccessGuard guard = new DemoAccessGuard(demoUserIds,
                new DemoQuotaService(properties.getDailyChatLimit()));

        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                     Object handler) throws IOException {
                if (!cn.dev33.satoken.stp.StpUtil.isLogin()) {
                    return true; // 未登录请求交给 AuthInterceptor 统一处理
                }
                Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
                String method = request.getMethod();
                String path = request.getRequestURI();
                switch (guard.check(userId, method, path)) {
                    case ALLOW:
                        return true;
                    case DENY:
                        log.warn("演示账号越权拦截: userId={} {} {}", userId, method, path);
                        writeJson(response, 403, "演示账号为只读，禁止执行管理操作");
                        return false;
                    case QUOTA_EXCEEDED:
                    default:
                        log.warn("演示账号额度耗尽: userId={} {} {}", userId, method, path);
                        writeJson(response, 429, "演示聊天额度已用尽，每日上限 "
                                + properties.getDailyChatLimit() + " 次，次日自动恢复");
                        return false;
                }
            }
        }).addPathPatterns("/api/**").order(0);
    }

    private void writeJson(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":" + status + ",\"msg\":\"" + msg + "\"}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
