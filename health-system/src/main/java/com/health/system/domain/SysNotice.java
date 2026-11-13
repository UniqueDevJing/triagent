package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {
    private Long userId;
    private String title;
    private String content;
    private String noticeType;
    private Integer isRead;
    private String extra;
}
