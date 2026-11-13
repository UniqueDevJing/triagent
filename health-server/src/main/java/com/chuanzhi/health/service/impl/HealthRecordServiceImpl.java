package com.chuanzhi.health.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.entity.HealthRecord;
import com.chuanzhi.health.mapper.HealthRecordMapper;
import com.chuanzhi.health.service.HealthRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordMapper recordMapper;

    @Override
    public IPage<HealthRecord> pageRecords(int page, int size, Long userId) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(HealthRecord::getUserId, userId);
        wrapper.orderByDesc(HealthRecord::getRecordDate);
        return recordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<HealthRecord> getByUserId(Long userId) {
        return recordMapper.selectList(
            new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .orderByDesc(HealthRecord::getRecordDate)
        );
    }

    @Override
    public HealthRecord getById(Long id) {
        HealthRecord record = recordMapper.selectById(id);
        if (record == null) throw new BusinessException("健康档案不存在");
        return record;
    }

    @Override
    @Transactional
    public HealthRecord create(HealthRecord record) {
        if (record.getUserId() == null) throw new BusinessException("用户ID不能为空");
        recordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public HealthRecord update(HealthRecord record) {
        if (recordMapper.selectById(record.getId()) == null)
            throw new BusinessException("健康档案不存在");
        recordMapper.updateById(record);
        return recordMapper.selectById(record.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (recordMapper.selectById(id) == null)
            throw new BusinessException("健康档案不存在");
        recordMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> getLatestMetrics(Long userId) {
        HealthRecord latest = recordMapper.selectOne(
            new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .orderByDesc(HealthRecord::getRecordDate)
                .last("LIMIT 1")
        );
        if (latest == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("message", "暂无体检数据");
            return empty;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", latest.getId());
        result.put("recordDate", latest.getRecordDate() != null ? latest.getRecordDate().toString() : "");
        result.put("type", latest.getType());
        try {
            result.put("metrics", JSONUtil.parseObj(latest.getMetrics()));
        } catch (Exception e) {
            result.put("metrics", latest.getMetrics());
        }
        result.put("doctorNotes", latest.getDoctorNotes());
        return result;
    }
}
