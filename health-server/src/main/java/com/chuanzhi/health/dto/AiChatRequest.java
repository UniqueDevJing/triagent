package com.chuanzhi.health.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String message;
    private String sessionId;
    @NotBlank(message = "功能类型不能为空")
    private String featureType;
}
