package com.health.web.assistant.it;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试的"防假绿"守卫（Round 4）。
 *
 * <p>集成测试在没有环境变量时会 {@code assumeTrue} 跳过——这在本地是合理降级，
 * 但在 CI 里若因为配置漏写而跳过，就会**看起来全绿实际什么都没验**。
 * 所以这里立一条硬规矩：只要 CI=true，集成端点就必须被提供，否则直接失败。
 */
@Tag("integration")
class IntegrationEnvGuardTest {

    @Test
    @DisplayName("CI 必须提供集成测试端点（缺失即失败，不允许静默跳过）")
    void ciMustProvideIntegrationEndpoints() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("CI")),
                "本地运行：集成端点可选，跳过守卫");

        assertThat(System.getenv("IT_REDIS_HOST")).as("CI 缺少 IT_REDIS_HOST").isNotBlank();
        assertThat(System.getenv("IT_REDIS_PORT")).as("CI 缺少 IT_REDIS_PORT").isNotBlank();
        assertThat(System.getenv("IT_MYSQL_URL")).as("CI 缺少 IT_MYSQL_URL").isNotBlank();
    }
}