package com.hospital.v2.service;

import com.hospital.v2.common.BizException;
import com.hospital.v2.dao.Db;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** 预约挂号：乐观锁扣减号源 + 不可重复预约 + 标记分诊命中。 */
@Service
public class RegistrationService {
    private final Db db;

    public RegistrationService(Db db) {
        this.db = db;
    }

    public Map<String, Object> register(Long suggestionId, Long patientId) {
        Map<String, Object> sug = db.queryOne(
                "SELECT id, department_id, doctor_id FROM triage_suggestion WHERE id=:id", Map.of("id", suggestionId));
        if (sug == null) throw new BizException("分诊建议不存在");
        long dept = ((Number) sug.get("department_id")).longValue();
        long doc = ((Number) sug.get("doctor_id")).longValue();

        // 选最近有余号的排班
        String today = LocalDate.now().toString();
        Map<String, Object> sched = db.queryOne(
                "SELECT id, work_date, period FROM doctor_schedule WHERE doctor_id=:doc AND status=1 "
                        + "AND remain_num>0 AND work_date>=:today ORDER BY work_date, period LIMIT 1",
                Map.of("doc", doc, "today", today));
        if (sched == null) throw new BizException("该医生暂无可约号源");

        long schedId = ((Number) sched.get("id")).longValue();
        // 乐观锁扣减
        int aff = db.update("UPDATE doctor_schedule SET remain_num=remain_num-1 "
                + "WHERE id=:id AND remain_num>0", Map.of("id", schedId));
        if (aff == 0) throw new BizException("号源已被抢光，请重试");

        // 写挂号记录（唯一约束：不可重复预约）
        Map<String, Object> ins = new LinkedHashMap<>();
        ins.put("suggestion_id", suggestionId);
        ins.put("patient_id", patientId);
        ins.put("department_id", dept);
        ins.put("doctor_id", doc);
        ins.put("schedule_id", schedId);
        ins.put("work_date", sched.get("work_date"));
        ins.put("period", sched.get("period"));
        long regId;
        try {
            regId = db.insert("INSERT INTO registration (suggestion_id, patient_id, department_id, doctor_id, "
                    + "schedule_id, work_date, period) VALUES "
                    + "(:suggestion_id,:patient_id,:department_id,:doctor_id,:schedule_id,:work_date,:period)", ins);
        } catch (DuplicateKeyException e) {
            // 回滚号源
            db.update("UPDATE doctor_schedule SET remain_num=remain_num+1 WHERE id=:id", Map.of("id", schedId));
            throw new BizException("您已预约该时段，不可重复预约");
        }

        // 标记分诊命中（命中率统计用）
        db.update("UPDATE triage_suggestion SET registered=1, status='REGISTERED' WHERE id=:id", Map.of("id", suggestionId));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("registration_id", regId);
        out.put("department_id", dept);
        out.put("doctor_id", doc);
        out.put("work_date", sched.get("work_date"));
        out.put("period", sched.get("period"));
        return out;
    }

    public List<Map<String, Object>> myRegistrations(Long patientId) {
        return db.query("SELECT * FROM registration WHERE patient_id=:p ORDER BY id DESC",
                Map.of("p", patientId));
    }
}
