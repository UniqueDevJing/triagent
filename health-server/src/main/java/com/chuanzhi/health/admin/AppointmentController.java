package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.Appointment;
import com.chuanzhi.health.service.admin.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status) {
        return Result.ok(appointmentService.list(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<Appointment> get(@PathVariable Long id) {
        return Result.ok(appointmentService.getById(id));
    }

    @PostMapping
    public Result<Appointment> create(@RequestBody Appointment appointment) {
        return Result.ok(appointmentService.create(appointment));
    }

    @PutMapping("/{id}")
    public Result<Appointment> update(@PathVariable Long id, @RequestBody Appointment appointment) {
        appointment.setId(id);
        return Result.ok(appointmentService.update(appointment));
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        appointmentService.updateStatus(id, body.get("status"));
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        appointmentService.delete(id);
        return Result.ok(null);
    }
}
