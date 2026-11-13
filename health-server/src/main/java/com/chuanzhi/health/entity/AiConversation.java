package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_conversations")
public class AiConversation {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @NotBlank(message = "角色不能为空")
    private String role;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String featureType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
