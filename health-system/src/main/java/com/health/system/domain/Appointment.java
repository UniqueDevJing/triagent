package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appointment")
public class Appointment extends BaseEntity {
    private Long memberId;
    private Long packageId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String status;
    private String remark;
}
