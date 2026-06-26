package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.ChronicDiseaseMgmt;
import com.chuanzhi.health.mapper.ChronicDiseaseMgmtMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChronicDiseaseService {

    private final ChronicDiseaseMgmtMapper chronicDiseaseMgmtMapper;

    public PageResult<ChronicDiseaseMgmt> list(int page, int size, Long memberId) {
        Page<ChronicDiseaseMgmt> pg = new Page<>(page, size);
        LambdaQueryWrapper<ChronicDiseaseMgmt> qw = new LambdaQueryWrapper<>();
        if (memberId != null) {
            qw.eq(ChronicDiseaseMgmt::getMemberId, memberId);
        }
        qw.orderByDesc(ChronicDiseaseMgmt::getUpdatedAt);
        return PageResult.of(chronicDiseaseMgmtMapper.selectPage(pg, qw));
    }

    public ChronicDiseaseMgmt getById(Long id) {
        ChronicDiseaseMgmt entity = chronicDiseaseMgmtMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("慢性病管理记录不存在");
        }
        return entity;
    }

    public ChronicDiseaseMgmt create(ChronicDiseaseMgmt entity) {
        chronicDiseaseMgmtMapper.insert(entity);
        return entity;
    }

    public ChronicDiseaseMgmt update(ChronicDiseaseMgmt entity) {
        chronicDiseaseMgmtMapper.updateById(entity);
        return chronicDiseaseMgmtMapper.selectById(entity.getId());
    }

    public void delete(Long id) {
        chronicDiseaseMgmtMapper.deleteById(id);
    }
}
