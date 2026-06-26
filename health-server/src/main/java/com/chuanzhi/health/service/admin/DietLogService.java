package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.DietLog;
import com.chuanzhi.health.mapper.DietLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DietLogService {

    private final DietLogMapper dietLogMapper;

    public PageResult<DietLog> list(int page, int size, Long memberId, LocalDate recordedDate) {
        Page<DietLog> pg = new Page<>(page, size);
        LambdaQueryWrapper<DietLog> qw = new LambdaQueryWrapper<>();
        if (memberId != null) {
            qw.eq(DietLog::getMemberId, memberId);
        }
        if (recordedDate != null) {
            qw.eq(DietLog::getRecordedDate, recordedDate);
        }
        qw.orderByDesc(DietLog::getRecordedDate);
        return PageResult.of(dietLogMapper.selectPage(pg, qw));
    }

    public DietLog getById(Long id) {
        DietLog entity = dietLogMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("饮食记录不存在");
        }
        return entity;
    }

    public DietLog create(DietLog entity) {
        dietLogMapper.insert(entity);
        return entity;
    }

    public void delete(Long id) {
        dietLogMapper.deleteById(id);
    }
}
