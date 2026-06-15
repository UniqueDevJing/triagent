package com.chuanzhi.health.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class AssessmentRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotNull(message = "量表ID不能为空")
    private Long templateId;
    @NotEmpty(message = "答案不能为空")
    private Map<Long, Integer> answers;
}
