package com.health.web.assistant.mcp;

import com.health.web.assistant.a2a.AgentCardRegistry;
import com.health.web.assistant.agent.AgentRegistry;
import com.health.web.assistant.tool.ToolAccessPolicy;
import com.health.web.assistant.tool.TriageTools;
import io.modelcontextprotocol.server.McpSyncServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpWebMvcServerAutoConfiguration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * MCP Server 装配冒烟测试（不依赖 MySQL/Redis/LLM，也不需要真实端口）。
 *
 * <p>验证的是本轮最重要的部署风险：{@code spring-ai-starter-mcp-server-webmvc:1.0.0}
 * 官方声明基于 Boot 3.4.5，而 triagent 仍在 Boot 3.2.0。这里在当前类路径下
 * 真实加载官方自动配置，确认 {@link McpSyncServer} 能装配出来、
 * 我们提供的 {@link ToolCallbackProvider} 被聚合进去，而不是「编译过了但启动炸」。
 */
class McpServerWiringTest {

    private final ToolAccessPolicy policy = new ToolAccessPolicy();
    private final AgentCardRegistry cards = new AgentCardRegistry(new AgentRegistry(policy), policy);

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    McpServerAutoConfiguration.class,
                    McpWebMvcServerAutoConfiguration.class))
            .withUserConfiguration(McpToolExposure.class)
            .withBean(TriageTools.class, () -> mock(TriageTools.class))
            .withBean(AgentCardRegistry.class, () -> cards)
            .withPropertyValues(
                    "spring.ai.mcp.server.enabled=true",
                    "spring.ai.mcp.server.name=triagent-tools",
                    "spring.ai.mcp.server.version=1.0.0",
                    "spring.ai.mcp.server.type=SYNC",
                    "spring.ai.mcp.server.stdio=false",
                    "spring.ai.mcp.server.capabilities.tool=true",
                    "spring.ai.mcp.server.capabilities.resource=false",
                    "spring.ai.mcp.server.capabilities.prompt=false",
                    "spring.ai.mcp.server.capabilities.completion=false");

    @Test
    @DisplayName("Boot 3.2.0 类路径下官方 MCP 自动配置可装配，Server 与工具提供者都就位")
    void serverAndToolsAreWired() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(McpSyncServer.class);
            assertThat(ctx).hasSingleBean(ToolCallbackProvider.class);
            assertThat(ctx).hasSingleBean(TriageMcpTools.class);

            ToolCallbackProvider provider = ctx.getBean(ToolCallbackProvider.class);
            assertThat(provider.getToolCallbacks()).hasSize(3);
        });
    }

    @Test
    @DisplayName("关闭开关后不装配 MCP Server，退化为纯进程内工具（降级路径可用）")
    void canBeDisabled() {
        runner.withPropertyValues("spring.ai.mcp.server.enabled=false").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(McpSyncServer.class);
            assertThat(ctx).doesNotHaveBean(TriageMcpTools.class);
        });
    }
}
