package com.health.system.domain.vo;

import com.health.system.domain.Appointment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppointmentVO extends Appointment {
    private String memberName;
    private String memberPhone;
    private String packageName;
    private BigDecimal packagePrice;
    private String timeSlot;
    private String notes;
}
