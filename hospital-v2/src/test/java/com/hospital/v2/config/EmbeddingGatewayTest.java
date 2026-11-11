package com.hospital.v2.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 纯单元测试：余弦相似度与向量 JSON 序列化（不依赖 Spring）。 */
class EmbeddingGatewayTest {

    @Test
    void cosine_identical_is_one() {
        assertEquals(1.0, EmbeddingGateway.cosine(new float[]{1, 0, 0}, new float[]{1, 0, 0}), 1e-9);
    }

    @Test
    void cosine_orthogonal_is_zero() {
        assertEquals(0.0, EmbeddingGateway.cosine(new float[]{1, 0, 0}, new float[]{0, 1, 0}), 1e-9);
    }

    @Test
    void cosine_opposite_is_minus_one() {
        assertEquals(-1.0, EmbeddingGateway.cosine(new float[]{1, 0, 0}, new float[]{-1, 0, 0}), 1e-9);
    }

    @Test
    void cosine_zero_vector_is_zero() {
        assertEquals(0.0, EmbeddingGateway.cosine(new float[]{0, 0, 0}, new float[]{1, 0, 0}), 1e-9);
    }

    @Test
    void cosine_length_mismatch_guarded() {
        // 只比较公共前缀，不会抛异常
        assertEquals(0.0, EmbeddingGateway.cosine(new float[]{1, 0}, new float[]{0, 1, 0}), 1e-9);
    }

    @Test
    void json_round_trip() {
        float[] v = {0.1f, 0.2f, 0.3f, -0.4f};
        String s = EmbeddingGateway.toJson(v);
        float[] back = EmbeddingGateway.fromJson(s);
        assertEquals(v.length, back.length);
        for (int i = 0; i < v.length; i++) assertEquals(v[i], back[i], 1e-6f);
    }

    @Test
    void fromJson_blank_returns_empty() {
        assertArrayEquals(new float[0], EmbeddingGateway.fromJson(""));
        assertArrayEquals(new float[0], EmbeddingGateway.fromJson(null));
    }
}
