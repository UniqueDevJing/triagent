package com.health.web.config;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DataSource dataSource;

    @Value("${health.data-init.enabled:true}")
    private boolean enabled;

    public DataInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("种子数据初始化已禁用 (health.data-init.enabled=false)");
            return;
        }
        try (Connection conn = dataSource.getConnection()) {
            if (isFullySeeded(conn)) {
                log.info("数据库已有完整业务数据，跳过种子数据初始化");
                return;
            }
            log.warn("检测到数据不完整（member/appointment/assessment_record 有空表），开始重新执行种子数据初始化...");

            // 关闭自动提交，整个脚本用一个事务包裹
            conn.setAutoCommit(false);
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setAutoCommit(false);
            runner.setStopOnError(true);
            runner.setSendFullScript(false);
            runner.setLogWriter(null);
            runner.setErrorLogWriter(null);

            var reader = new InputStreamReader(
                    new ClassPathResource("db/seed_v2.sql").getInputStream(),
                    StandardCharsets.UTF_8);
            runner.runScript(reader);
            reader.close();

            conn.commit();
            log.info("种子数据初始化完成！事务已提交");
        } catch (Exception e) {
            log.error("种子数据初始化失败: {}", e.getMessage(), e);
            log.error("请检查: 1) MySQL是否已启动 2) init.sql是否已执行 3) seed_v2.sql是否存在");
        }
    }

    private boolean isFullySeeded(Connection conn) throws Exception {
        long memberCount = 0, appointmentCount = 0, assessmentCount = 0;
        long exerciseCount = 0, recipeCount = 0, interventionCount = 0;
        try (var stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM member");
            if (rs.next()) memberCount = rs.getLong(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM appointment");
            if (rs.next()) appointmentCount = rs.getLong(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM assessment_record");
            if (rs.next()) assessmentCount = rs.getLong(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM exercise_library");
            if (rs.next()) exerciseCount = rs.getLong(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM recipe_library");
            if (rs.next()) recipeCount = rs.getLong(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM intervention_plan");
            if (rs.next()) interventionCount = rs.getLong(1);
        }
        log.info("数据完整性检查: member={}, appointment={}, assessment_record={}, exercise_library={}, recipe_library={}, intervention_plan={}",
                memberCount, appointmentCount, assessmentCount, exerciseCount, recipeCount, interventionCount);
        return memberCount > 0 && appointmentCount > 0 && assessmentCount > 0
            && exerciseCount > 0 && recipeCount > 0 && interventionCount > 0;
    }
}