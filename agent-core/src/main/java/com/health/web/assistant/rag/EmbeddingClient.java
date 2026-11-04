package com.health.web.assistant.rag;

/**
 * 嵌入客户端抽象（T-A 向量检索腿）。
 *
 * <p>实现方负责真实调用（如 DashScope text-embedding 系列）；
 * 检索器只依赖本接口，测试用脚本化假向量即可驱动。
 */
public interface EmbeddingClient {

    /**
     * 把文本映射为向量。
     *
     * @return 向量；失败时实现方可返回 null（调用方按"嵌入不可用"降级，不抛出）
     */
    float[] embed(String text);

    /** 模型标识（日志/审计用） */
    default String model() {
        return "";
    }
}
