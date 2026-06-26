package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("appointments")
public class Appointment {
    @TableId(type = IdType.AUTO) private Long id;
    private Long memberId;
    private Long packageId;
    private LocalDate appointmentDate;
    private String timeSlot;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    @TableField(exist = false) private String memberName;
    @TableField(exist = false) private String packageName;
}
