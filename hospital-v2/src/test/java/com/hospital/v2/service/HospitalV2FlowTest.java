package com.hospital.v2.service;

import com.hospital.v2.common.BizException;
import com.hospital.v2.domain.TriageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 集成测试：在 H2 零依赖上下文中跑通 ReAct 分诊 + 危急硬拦截 + 挂号乐观锁。 */
@SpringBootTest
class HospitalV2FlowTest {

    @Autowired
    private PreTriageService preTriage;
    @Autowired
    private ReActTriageAgent agent;
    @Autowired
    private RegistrationService registration;

    @Test
    void normal_triage_returns_valid_department() {
        long sid = preTriage.startSession(1L);
        TriageResult r = agent.triage(sid, 1L, "咳嗽一周，夜间加重");
        Map<String, Object> sug = r.suggestion();
        assertNotNull(sug);
        assertTrue(((Number) sug.get("department_id")).longValue() > 0, "应给出有效科室");
        assertNotNull(sug.get("doctor_id"));
        assertNotNull(sug.get("suggested_date"));
        assertEquals(0, ((Number) sug.get("red_flag")).intValue(), "普通症状不应触发危急");
    }

    @Test
    void critical_complaint_hard_routes_to_emergency() {
        long sid = preTriage.startSession(1L);
        TriageResult r = agent.triage(sid, 1L, "突发胸痛大汗");
        Map<String, Object> sug = r.suggestion();
        assertEquals(5L, ((Number) sug.get("department_id")).longValue(), "危急应硬转急诊科(id=5)");
        assertEquals(1, ((Number) sug.get("red_flag")).intValue());
    }

    @Test
    void duplicate_registration_rejected() {
        long sid = preTriage.startSession(2L);
        TriageResult r = agent.triage(sid, 2L, "头痛眩晕");
        long suggestionId = ((Number) r.suggestion().get("suggestion_id")).longValue();
        // 第一次挂号成功
        assertDoesNotThrow(() -> registration.register(suggestionId, 2L));
        // 同一建议重复挂号应抛业务异常（乐观锁 + 唯一约束）
        assertThrows(BizException.class, () -> registration.register(suggestionId, 2L));
    }
}
