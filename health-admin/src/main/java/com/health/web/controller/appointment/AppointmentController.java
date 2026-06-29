package com.health.web.controller.appointment;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.Appointment;
import com.health.system.mapper.AppointmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointment")
public class AppointmentController extends BaseController {
    private final AppointmentMapper appointmentMapper;
    public AppointmentController(AppointmentMapper appointmentMapper) { this.appointmentMapper = appointmentMapper; }

    @GetMapping
    @Log(title = "预约查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(status != null && !status.isEmpty(), Appointment::getStatus, status)
                .orderByDesc(Appointment::getCreateTime);
        Page<Appointment> p = appointmentMapper.selectPage(new Page<>(page, pageSize), qw);
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(appointmentMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增预约")
    public AjaxResult create(@RequestBody Appointment appointment) { appointmentMapper.insert(appointment); return success(appointment); }

    @PutMapping
    @Log(title = "修改预约")
    public AjaxResult update(@RequestBody Appointment appointment) { appointmentMapper.updateById(appointment); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除预约")
    public AjaxResult delete(@PathVariable List<Long> ids) { appointmentMapper.deleteBatchIds(ids); return success(); }
}
