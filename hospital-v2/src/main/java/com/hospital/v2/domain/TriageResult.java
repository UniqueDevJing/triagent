package com.hospital.v2.domain;

import java.util.Map;

/** 一次分诊的产物：run 编号 + 经核验的分诊建议 + 实际步数。 */
public record TriageResult(long runId, Map<String, Object> suggestion, int steps) {
}
