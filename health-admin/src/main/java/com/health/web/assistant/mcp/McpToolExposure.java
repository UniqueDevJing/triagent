package com.health.web.assistant.mcp;

import com.health.web.assistant.a2a.AgentCardRegistry;
import com.health.web.assistant.tool.TriageTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 工具暴露层。
 *
 * <p>Spring AI 1.0.0 的 MCP 自动配置会聚合 {@link ToolCallbackProvider} Bean
 * 转成 MCP 的 {@code tools/list}。这里刻意<b>不</b>返回 {@code ToolCallback[]}：
 * 数组 Bean 不在自动配置的聚合列表里，会出现「工具定义成功但 tools/list 为空」
 * 的静默故障（P0-2 在 CodeCanvas 上踩过）。
 *
 * <p>{@code spring.ai.mcp.server.enabled=false} 可整体关闭，关闭后本配置不生效，
 * 应用退化为纯进程内工具调用，既有行为与测试不受影响。
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class McpToolExposure {

    @Bean
    public TriageMcpTools triageMcpTools(TriageTools triageTools, AgentCardRegistry cards) {
        return new TriageMcpTools(triageTools, cards);
    }

    @Bean
    public ToolCallbackProvider triageMcpToolProvider(TriageMcpTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
