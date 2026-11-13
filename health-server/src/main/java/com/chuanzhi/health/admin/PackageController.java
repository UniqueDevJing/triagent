package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.Package;
import com.chuanzhi.health.service.admin.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(packageService.list(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<Package> get(@PathVariable Long id) {
        return Result.ok(packageService.getById(id));
    }

    @PostMapping
    public Result<Package> create(@RequestBody Package pkg) {
        return Result.ok(packageService.create(pkg));
    }

    @PutMapping("/{id}")
    public Result<Package> update(@PathVariable Long id, @RequestBody Package pkg) {
        pkg.setId(id);
        return Result.ok(packageService.update(pkg));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        packageService.delete(id);
        return Result.ok(null);
    }

    @GetMapping("/{id}/items")
    public Result<Package> getPackageItems(@PathVariable Long id) {
        return Result.ok(packageService.getPackageItems(id));
    }
}
