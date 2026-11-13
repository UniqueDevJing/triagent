package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.TcmConstitution;
import com.chuanzhi.health.mapper.TcmConstitutionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TcmConstitutionService {

    private final TcmConstitutionMapper tcmConstitutionMapper;

    public PageResult<TcmConstitution> list(int page, int size) {
        Page<TcmConstitution> pg = new Page<>(page, size);
        LambdaQueryWrapper<TcmConstitution> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(TcmConstitution::getId);
        return PageResult.of(tcmConstitutionMapper.selectPage(pg, qw));
    }

    public TcmConstitution getById(Long id) {
        TcmConstitution constitution = tcmConstitutionMapper.selectById(id);
        if (constitution == null) {
            throw new BusinessException("中医体质类型不存在");
        }
        return constitution;
    }

    public TcmConstitution create(TcmConstitution constitution) {
        tcmConstitutionMapper.insert(constitution);
        return constitution;
    }

    public TcmConstitution update(TcmConstitution constitution) {
        TcmConstitution existing = tcmConstitutionMapper.selectById(constitution.getId());
        if (existing == null) {
            throw new BusinessException("中医体质类型不存在");
        }
        tcmConstitutionMapper.updateById(constitution);
        return tcmConstitutionMapper.selectById(constitution.getId());
    }

    public void delete(Long id) {
        TcmConstitution existing = tcmConstitutionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("中医体质类型不存在");
        }
        tcmConstitutionMapper.deleteById(id);
    }
}
