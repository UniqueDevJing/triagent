package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.Appointment;
import com.chuanzhi.health.entity.Member;
import com.chuanzhi.health.entity.Package;
import com.chuanzhi.health.mapper.AppointmentMapper;
import com.chuanzhi.health.mapper.MemberMapper;
import com.chuanzhi.health.mapper.PackageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "CONFIRMED", "DONE", "CANCELLED");

    private static final java.util.Map<String, java.util.Set<String>> STATE_TRANSITIONS = java.util.Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("DONE", "CANCELLED"),
            "DONE", Set.of(),
            "CANCELLED", Set.of()
    );

    private final AppointmentMapper appointmentMapper;
    private final MemberMapper memberMapper;
    private final PackageMapper packageMapper;

    public PageResult<Appointment> list(int page, int size, String keyword, String status) {
        Page<Appointment> pg = new Page<>(page, size);
        Page<Appointment> result = appointmentMapper.selectWithDetail(pg, keyword, status);
        return PageResult.of(result);
    }

    public Appointment getById(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment != null) {
            populateNames(appointment);
        }
        return appointment;
    }

    public Appointment create(Appointment appointment) {
        appointmentMapper.insert(appointment);
        return appointment;
    }

    public Appointment update(Appointment appointment) {
        // 清除 status 字段，防止通用 update 绕过状态流转规则
        appointment.setStatus(null);
        appointmentMapper.updateById(appointment);
        return appointmentMapper.selectById(appointment.getId());
    }

    public void updateStatus(Long id, String targetStatus) {
        if (!VALID_STATUSES.contains(targetStatus)) {
            throw new BusinessException("无效的状态值: " + targetStatus);
        }
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        String currentStatus = appointment.getStatus();
        Set<String> allowed = STATE_TRANSITIONS.get(currentStatus);
        if (allowed == null) {
            throw new BusinessException("当前状态 " + currentStatus + " 不允许变更为 " + targetStatus);
        }
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException("当前状态 " + currentStatus + " 不允许变更为 " + targetStatus + "，允许: " +
                    String.join(", ", allowed.isEmpty() ? java.util.Collections.singletonList("无") : allowed));
        }
        appointment.setStatus(targetStatus);
        appointmentMapper.updateById(appointment);
    }

    public void delete(Long id) {
        appointmentMapper.deleteById(id);
    }

    private void populateNames(Appointment appointment) {
        if (appointment.getMemberId() != null) {
            Member member = memberMapper.selectById(appointment.getMemberId());
            if (member != null) {
                appointment.setMemberName(member.getName());
            }
        }
        if (appointment.getPackageId() != null) {
            Package pkg = packageMapper.selectById(appointment.getPackageId());
            if (pkg != null) {
                appointment.setPackageName(pkg.getName());
            }
        }
    }
}
