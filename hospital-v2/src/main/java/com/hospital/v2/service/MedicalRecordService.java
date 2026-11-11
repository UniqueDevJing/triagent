package com.hospital.v2.service;

import com.hospital.v2.common.BizException;
import com.hospital.v2.dao.Db;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 病历：医生接诊后写病历、签名归档；患者可查看。 */
@Service
public class MedicalRecordService {
    private final Db db;

    public MedicalRecordService(Db db) {
        this.db = db;
    }

    public long create(Long registrationId, Long patientId, Long doctorId,
                        String chief, String present, String diagnosis, String treatment) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("registration_id", registrationId);
        m.put("patient_id", patientId);
        m.put("doctor_id", doctorId);
        m.put("chief_complaint", chief);
        m.put("present_illness", present);
        m.put("diagnosis", diagnosis);
        m.put("treatment", treatment);
        return db.insert("INSERT INTO medical_record (registration_id, patient_id, doctor_id, chief_complaint, "
                + "present_illness, diagnosis, treatment) VALUES "
                + "(:registration_id,:patient_id,:doctor_id,:chief_complaint,:present_illness,:diagnosis,:treatment)", m);
    }

    public void sign(long recordId) {
        int aff = db.update("UPDATE medical_record SET status='SIGNED' WHERE id=:id AND status='DRAFT'",
                Map.of("id", recordId));
        if (aff == 0) throw new BizException("病历不存在或已签名");
    }

    public List<Map<String, Object>> myRecords(Long patientId) {
        return db.query("SELECT r.*, d.name doctor_name FROM medical_record r "
                + "LEFT JOIN doctor d ON d.id=r.doctor_id WHERE r.patient_id=:p ORDER BY r.id DESC",
                Map.of("p", patientId));
    }
}
