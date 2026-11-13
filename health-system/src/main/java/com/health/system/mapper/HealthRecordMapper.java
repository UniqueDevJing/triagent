package com.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.system.domain.HealthRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthRecordMapper extends BaseMapper<HealthRecord> {
}
