package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.ExamItem;
import com.chuanzhi.health.entity.ExamItemCategory;
import com.chuanzhi.health.service.admin.ExamItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExamItemController {

    private final ExamItemService examItemService;

    // ---- Categories ----

    @GetMapping("/api/admin/exam-categories")
    public Result<List<ExamItemCategory>> listCategories() {
        return Result.ok(examItemService.listCategories());
    }

    @PostMapping("/api/admin/exam-categories")
    public Result<ExamItemCategory> createCategory(@RequestBody ExamItemCategory category) {
        return Result.ok(examItemService.createCategory(category));
    }

    @PutMapping("/api/admin/exam-categories/{id}")
    public Result<ExamItemCategory> updateCategory(@PathVariable Long id, @RequestBody ExamItemCategory category) {
        category.setId(id);
        return Result.ok(examItemService.updateCategory(category));
    }

    @DeleteMapping("/api/admin/exam-categories/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        examItemService.deleteCategory(id);
        return Result.ok(null);
    }

    // ---- Items ----

    @GetMapping("/api/admin/exam-items")
    public Result<?> listItems(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) Long categoryId) {
        return Result.ok(examItemService.listItems(page, size, categoryId));
    }

    @GetMapping("/api/admin/exam-items/{id}")
    public Result<ExamItem> getItem(@PathVariable Long id) {
        return Result.ok(examItemService.getItem(id));
    }

    @PostMapping("/api/admin/exam-items")
    public Result<ExamItem> createItem(@RequestBody ExamItem item) {
        return Result.ok(examItemService.createItem(item));
    }

    @PutMapping("/api/admin/exam-items/{id}")
    public Result<ExamItem> updateItem(@PathVariable Long id, @RequestBody ExamItem item) {
        item.setId(id);
        return Result.ok(examItemService.updateItem(item));
    }

    @DeleteMapping("/api/admin/exam-items/{id}")
    public Result<?> deleteItem(@PathVariable Long id) {
        examItemService.deleteItem(id);
        return Result.ok(null);
    }
}
