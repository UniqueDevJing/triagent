package com.hospital.v2.service;

import com.hospital.v2.common.BizException;
import com.hospital.v2.dao.Db;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 病历七段结构化：① 主诉 ② 现病史 ③ 既往史 ④ 过敏史 ⑤ 辅助检查 ⑥ 诊断 ⑦ 处理意见。
 * DRAFT 草稿可继续编辑；SUBMITTED 提交后锁定，患者端与医生端均可查看。
 */
@Service
public class MedicalRecordService {
    private final Db db;

    public MedicalRecordService(Db db) {
        this.db = db;
    }

    private static final String[] SEGMENTS = {
            "chief_complaint", "present_illness", "past_history",
            "allergy_history", "assistant_exam", "diagnosis", "treatment"
    };

    /** 新建或更新病历（草稿态）。已 SUBMITTED 的病历不可再改。 */
    public long saveDraft(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        Map<String, Object> m = new LinkedHashMap<>(body);
        if (id == null) {
            return db.insert("INSERT INTO medical_record (registration_id, patient_id, doctor_id, chief_complaint, "
                    + "present_illness, past_history, allergy_history, assistant_exam, diagnosis, treatment, status) "
                    + "VALUES (:registration_id,:patient_id,:doctor_id,:chief_complaint,:present_illness,:past_history,"
                    + ":allergy_history,:assistant_exam,:diagnosis,:treatment,'DRAFT')", m);
        }
        Map<String, Object> row = db.queryOne("SELECT status FROM medical_record WHERE id=:id", Map.of("id", id));
        if (row == null) throw new BizException("病历不存在");
        if ("SUBMITTED".equals(row.get("status"))) throw new BizException("病历已提交锁定，不可修改");
        db.update("UPDATE medical_record SET chief_complaint=:chief_complaint, present_illness=:present_illness, "
                + "past_history=:past_history, allergy_history=:allergy_history, assistant_exam=:assistant_exam, "
                + "diagnosis=:diagnosis, treatment=:treatment WHERE id=:id", m);
        return id;
    }

    /** 提交病历：草稿 → SUBMITTED 锁定；同步把对应挂号置为 DONE（完成就诊）。 */
    public void submit(long recordId) {
        int aff = db.update("UPDATE medical_record SET status='SUBMITTED' WHERE id=:id AND status='DRAFT'",
                Map.of("id", recordId));
        if (aff == 0) throw new BizException("病历不存在或已提交");
        db.update("UPDATE registration SET status='DONE' WHERE id = "
                + "(SELECT registration_id FROM medical_record WHERE id=:id)", Map.of("id", recordId));
    }

    /** 完成就诊（不写病历直接结诊）。 */
    public void finishVisit(long registrationId) {
        int aff = db.update("UPDATE registration SET status='DONE' WHERE id=:id AND status='PAID'",
                Map.of("id", registrationId));
        if (aff == 0) throw new BizException("挂号不存在或已结诊");
    }

    public Map<String, Object> detail(long recordId) {
        return db.queryOne("SELECT r.*, d.name doctor_name, p.name patient_name FROM medical_record r "
                + "LEFT JOIN doctor d ON d.id=r.doctor_id LEFT JOIN patient_profile p ON p.id=r.patient_id "
                + "WHERE r.id=:id", Map.of("id", recordId));
    }

    public List<Map<String, Object>> myRecords(Long patientId) {
        return db.query("SELECT r.*, d.name doctor_name FROM medical_record r "
                + "LEFT JOIN doctor d ON d.id=r.doctor_id WHERE r.patient_id=:p ORDER BY r.id DESC",
                Map.of("p", patientId));
    }

    /** 医生名下待接诊/已接诊挂号列表（含是否已写病历）。 */
    public List<Map<String, Object>> doctorRegistrations(long doctorId) {
        return db.query("SELECT r.id registration_id, r.patient_id, r.work_date, r.period, r.status, "
                + "p.name patient_name, p.gender, p.age, p.past_history, p.allergic_history, "
                + "dep.name department, mr.id record_id, mr.status record_status "
                + "FROM registration r LEFT JOIN patient_profile p ON p.id=r.patient_id "
                + "LEFT JOIN department dep ON dep.id=r.department_id "
                + "LEFT JOIN medical_record mr ON mr.registration_id=r.id "
                + "WHERE r.doctor_id=:doc ORDER BY r.id DESC", Map.of("doc", doctorId));
    }
}
