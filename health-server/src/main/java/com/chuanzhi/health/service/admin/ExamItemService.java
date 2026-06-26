package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.ExamItem;
import com.chuanzhi.health.entity.ExamItemCategory;
import com.chuanzhi.health.mapper.ExamItemCategoryMapper;
import com.chuanzhi.health.mapper.ExamItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamItemService {

    private final ExamItemCategoryMapper examItemCategoryMapper;
    private final ExamItemMapper examItemMapper;

    // ---- Categories ----

    public List<ExamItemCategory> listCategories() {
        return examItemCategoryMapper.selectList(
                new LambdaQueryWrapper<ExamItemCategory>().orderByAsc(ExamItemCategory::getSortOrder));
    }

    public ExamItemCategory createCategory(ExamItemCategory category) {
        examItemCategoryMapper.insert(category);
        return category;
    }

    public ExamItemCategory updateCategory(ExamItemCategory category) {
        examItemCategoryMapper.updateById(category);
        return examItemCategoryMapper.selectById(category.getId());
    }

    public void deleteCategory(Long id) {
        long count = examItemMapper.selectCount(
                new LambdaQueryWrapper<ExamItem>().eq(ExamItem::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException("该分类下存在检查项，无法删除");
        }
        examItemCategoryMapper.deleteById(id);
    }

    // ---- Items ----

    public PageResult<ExamItem> listItems(int page, int size, Long categoryId) {
        Page<ExamItem> pg = new Page<>(page, size);
        LambdaQueryWrapper<ExamItem> qw = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            qw.eq(ExamItem::getCategoryId, categoryId);
        }
        qw.orderByDesc(ExamItem::getCreatedAt);
        Page<ExamItem> result = examItemMapper.selectPage(pg, qw);
        // populate categoryName
        for (ExamItem item : result.getRecords()) {
            ExamItemCategory cat = examItemCategoryMapper.selectById(item.getCategoryId());
            if (cat != null) {
                item.setCategoryName(cat.getName());
            }
        }
        return PageResult.of(result);
    }

    public ExamItem getItem(Long id) {
        ExamItem item = examItemMapper.selectById(id);
        if (item != null) {
            ExamItemCategory cat = examItemCategoryMapper.selectById(item.getCategoryId());
            if (cat != null) {
                item.setCategoryName(cat.getName());
            }
        }
        return item;
    }

    public ExamItem createItem(ExamItem item) {
        examItemMapper.insert(item);
        return item;
    }

    public ExamItem updateItem(ExamItem item) {
        examItemMapper.updateById(item);
        return examItemMapper.selectById(item.getId());
    }

    public void deleteItem(Long id) {
        examItemMapper.deleteById(id);
    }
}
