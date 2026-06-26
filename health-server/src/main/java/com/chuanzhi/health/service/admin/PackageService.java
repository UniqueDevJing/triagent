package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.Package;
import com.chuanzhi.health.entity.PackageItem;
import com.chuanzhi.health.mapper.PackageItemMapper;
import com.chuanzhi.health.mapper.PackageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageMapper packageMapper;
    private final PackageItemMapper packageItemMapper;

    public PageResult<Package> list(int page, int size, String keyword) {
        Page<Package> pg = new Page<>(page, size);
        LambdaQueryWrapper<Package> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(Package::getName, keyword);
        }
        qw.orderByDesc(Package::getCreatedAt);
        Page<Package> result = packageMapper.selectPage(pg, qw);
        return PageResult.of(result);
    }

    public Package getById(Long id) {
        Package pkg = packageMapper.selectById(id);
        if (pkg != null) {
            List<PackageItem> items = packageItemMapper.selectList(
                    new LambdaQueryWrapper<PackageItem>()
                            .eq(PackageItem::getPackageId, id)
                            .orderByAsc(PackageItem::getSortOrder));
            pkg.setItems(items);
        }
        return pkg;
    }

    @Transactional
    public Package create(Package pkg) {
        packageMapper.insert(pkg);
        if (pkg.getItems() != null) {
            for (PackageItem item : pkg.getItems()) {
                item.setPackageId(pkg.getId());
                packageItemMapper.insert(item);
            }
        }
        return pkg;
    }

    @Transactional
    public Package update(Package pkg) {
        packageMapper.updateById(pkg);
        // remove old associations and insert new ones
        packageItemMapper.delete(new LambdaQueryWrapper<PackageItem>()
                .eq(PackageItem::getPackageId, pkg.getId()));
        if (pkg.getItems() != null) {
            for (PackageItem item : pkg.getItems()) {
                item.setId(null);
                item.setPackageId(pkg.getId());
                packageItemMapper.insert(item);
            }
        }
        return packageMapper.selectById(pkg.getId());
    }

    public void delete(Long id) {
        packageMapper.deleteById(id);
    }

    public Package getPackageItems(Long packageId) {
        return getById(packageId);
    }
}
