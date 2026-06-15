package com.chuanzhi.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.entity.HealthRecord;

import java.util.List;
import java.util.Map;

public interface HealthRecordService {
    IPage<HealthRecord> pageRecords(int page, int size, Long userId);
    List<HealthRecord> getByUserId(Long userId);
    HealthRecord getById(Long id);
    HealthRecord create(HealthRecord record);
    HealthRecord update(HealthRecord record);
    void delete(Long id);
    Map<String, Object> getLatestMetrics(Long userId);
}
