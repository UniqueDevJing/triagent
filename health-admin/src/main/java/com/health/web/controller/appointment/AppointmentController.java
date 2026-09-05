package com.health.web.controller.appointment;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.Appointment;
import com.health.system.domain.Member;
import com.health.system.domain.PackageInfo;
import com.health.system.domain.vo.AppointmentVO;
import com.health.system.mapper.AppointmentMapper;
import com.health.system.mapper.MemberMapper;
import com.health.system.mapper.PackageInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/appointment")
public class AppointmentController extends BaseController {

    private final AppointmentMapper appointmentMapper;
    private final MemberMapper memberMapper;
    private final PackageInfoMapper packageInfoMapper;

    public AppointmentController(AppointmentMapper appointmentMapper,
                                 MemberMapper memberMapper,
                                 PackageInfoMapper packageInfoMapper) {
        this.appointmentMapper = appointmentMapper;
        this.memberMapper = memberMapper;
        this.packageInfoMapper = packageInfoMapper;
    }

    @GetMapping
    @Log(title = "预约查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(status != null && !status.isEmpty(), Appointment::getStatus, status)
                .orderByDesc(Appointment::getCreateTime);
        Page<Appointment> p = appointmentMapper.selectPage(new Page<>(page, pageSize), qw);

        List<AppointmentVO> vos = toVOList(p.getRecords());
        return toPage(p.getTotal(), vos);
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        Appointment a = appointmentMapper.selectById(id);
        if (a == null) return error("预约不存在");
        return success(toVO(a));
    }

    @PostMapping
    @Log(title = "新增预约")
    public AjaxResult create(@RequestBody Appointment appointment) {
        if (appointment.getStatus() == null) appointment.setStatus("PENDING");
        appointmentMapper.insert(appointment);
        return success(appointment);
    }

    @PutMapping("/{id}")
    @Log(title = "修改预约")
    public AjaxResult update(@PathVariable Long id, @RequestBody Appointment appointment) {
        appointment.setId(id);
        appointmentMapper.updateById(appointment);
        return success();
    }

    @DeleteMapping("/{ids}")
    @Log(title = "删除预约")
    public AjaxResult delete(@PathVariable List<Long> ids) {
        appointmentMapper.deleteBatchIds(ids);
        return success();
    }

    private List<AppointmentVO> toVOList(List<Appointment> appointments) {
        if (appointments.isEmpty()) return List.of();

        List<Long> memberIds = appointments.stream().map(Appointment::getMemberId).distinct().toList();
        List<Long> packageIds = appointments.stream().map(Appointment::getPackageId).distinct().toList();

        Map<Long, Member> memberMap = memberMapper.selectBatchIds(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        Map<Long, PackageInfo> packageMap = packageInfoMapper.selectBatchIds(packageIds).stream()
                .collect(Collectors.toMap(PackageInfo::getId, Function.identity()));

        return appointments.stream().map(a -> {
            AppointmentVO vo = new AppointmentVO();
            // copy base fields
            vo.setId(a.getId());
            vo.setMemberId(a.getMemberId());
            vo.setPackageId(a.getPackageId());
            vo.setAppointmentDate(a.getAppointmentDate());
            vo.setAppointmentTime(a.getAppointmentTime());
            vo.setStatus(a.getStatus());
            vo.setRemark(a.getRemark());
            vo.setTimeSlot(a.getAppointmentTime() != null && a.getAppointmentTime().getHour() < 12 ? "MORNING" : "AFTERNOON");
            vo.setNotes(a.getRemark());
            vo.setCreateTime(a.getCreateTime());
            vo.setUpdateTime(a.getUpdateTime());
            // fill joined fields
            Member m = memberMap.get(a.getMemberId());
            if (m != null) {
                vo.setMemberName(m.getName());
                vo.setMemberPhone(m.getPhone());
            }
            PackageInfo pkg = packageMap.get(a.getPackageId());
            if (pkg != null) {
                vo.setPackageName(pkg.getPackageName());
                vo.setPackagePrice(pkg.getPrice());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private AppointmentVO toVO(Appointment a) {
        return toVOList(List.of(a)).get(0);
    }
}
