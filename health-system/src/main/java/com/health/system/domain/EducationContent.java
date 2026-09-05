package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("education_content")
public class EducationContent extends BaseEntity {
    private String title;
    private String summary;
    private String content;
    private String contentType;
    private String author;
    private Integer viewCount;
    private String targetAudience;
    private Long wordId;
    private String status;
}
