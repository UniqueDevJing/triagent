package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member")
public class Member extends BaseEntity {
    private String name;
    private String gender;
    private LocalDate birthday;
    private String phone;
    private String idCard;
    private String address;
    private String bloodType;
    private BigDecimal height;
    private BigDecimal weight;
    private String allergyHistory;
    private String familyHistory;
    private String smokingStatus;
    private String drinkingStatus;
    private String remark;
}
