package com.chuanzhi.health.service;

import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.entity.HealthRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class HealthRecordServiceTest {

    @Autowired
    private HealthRecordService service;

    @Test
    void shouldCreateAndRetrieveHealthRecord() {
        HealthRecord record = new HealthRecord();
        record.setUserId(1L);
        record.setType("体检");
        record.setMetrics("{\"bloodPressure\":\"120/80\",\"heartRate\":72}");
        record.setDoctorNotes("年度体检，各项指标正常");

        HealthRecord created = service.create(record);
        assertNotNull(created.getId());

        HealthRecord found = service.getById(created.getId());
        assertEquals("体检", found.getType());
        assertEquals(1L, found.getUserId());
    }

    @Test
    void shouldThrowWhenUserIdIsNull() {
        HealthRecord record = new HealthRecord();
        record.setType("体检");

        assertThrows(BusinessException.class, () -> service.create(record));
    }

    @Test
    void shouldUpdateHealthRecord() {
        HealthRecord record = new HealthRecord();
        record.setUserId(2L);
        record.setType("复检");
        record.setMetrics("{\"weight\":70}");
        service.create(record);

        record.setDoctorNotes("体重略有下降，建议保持");
        HealthRecord updated = service.update(record);

        assertEquals("体重略有下降，建议保持", updated.getDoctorNotes());
    }

    @Test
    void shouldDeleteHealthRecord() {
        HealthRecord record = new HealthRecord();
        record.setUserId(3L);
        record.setType("自测");
        record.setMetrics("{\"steps\":8000}");
        service.create(record);

        service.delete(record.getId());

        assertThrows(BusinessException.class, () -> service.getById(record.getId()));
    }

    @Test
    void shouldParseLatestMetrics() {
        HealthRecord record = new HealthRecord();
        record.setUserId(4L);
        record.setType("体检");
        record.setMetrics("{\"bloodPressure\":\"130/85\",\"heartRate\":80,\"weight\":65.5}");
        service.create(record);

        Map<String, Object> metrics = service.getLatestMetrics(4L);
        assertNotNull(metrics.get("metrics"));
        assertEquals("体检", metrics.get("type"));
    }

    @Test
    void shouldReturnEmptyForNoRecords() {
        Map<String, Object> result = service.getLatestMetrics(99999L);
        assertEquals("暂无体检数据", result.get("message"));
    }
}
