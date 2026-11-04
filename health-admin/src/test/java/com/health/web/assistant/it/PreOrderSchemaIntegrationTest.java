package com.health.web.assistant.it;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 集成测试（Round 4）：真实 MySQL 上的预订单幂等约束。
 *
 * <p>跑法：设置 {@code IT_MYSQL_URL/IT_MYSQL_USER/IT_MYSQL_PASSWORD}（CI 由 services 提供）。
 *
 * <p>为什么不 mock：预订单"同一意图只出一单"这件事，**最终是靠数据库唯一索引兜底的**
 * （{@code UNIQUE KEY uk_idempotency_key}）。单测里 mock 掉 mapper 等于把这条保证
 * 从测试里删掉。这里直接在真实 MySQL 上应用仓库里的迁移 SQL，再验证约束真的在。
 *
 * <p>覆盖点（Risk Map R4 真实依赖 / R7 迁移脚本无测试）：
 * <ul>
 *   <li>迁移 SQL 能在干净库上成功执行（语法与依赖顺序）；</li>
 *   <li>{@code agent_preorder} 上存在覆盖 {@code idempotency_key} 的唯一索引；</li>
 *   <li>重复插入同一幂等键被数据库拒绝（真实约束，而不是应用层 if 判断）。</li>
 * </ul>
 */
@Tag("integration")
class PreOrderSchemaIntegrationTest {

    private static final String URL = System.getenv("IT_MYSQL_URL");
    private static final String USER = System.getenv("IT_MYSQL_USER");
    private static final String PASSWORD = System.getenv("IT_MYSQL_PASSWORD");

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** 从迁移脚本里取出 agent_preorder 的 CREATE TABLE 语句。 */
    private String createTableStatement() throws IOException {
        Path sql = Path.of(System.getProperty("user.dir"), "..", "sql", "migration_agent_preorder.sql");
        String text = Files.readString(sql, StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile(
                "CREATE TABLE[^;]*agent_preorder[^;]*;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(text);
        assertThat(matcher.find()).as("迁移脚本里应能找到 agent_preorder 的建表语句").isTrue();
        return matcher.group();
    }

    @Test
    @DisplayName("集成：迁移 SQL 可在干净库执行，且幂等唯一约束真实存在")
    void migrationAppliesAndUniqueConstraintExists() throws Exception {
        Assumptions.assumeTrue(URL != null, "IT_MYSQL_URL 未设置，跳过集成测试");

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS agent_preorder");
            st.execute(createTableStatement());

            Set<String> uniqueColumns = new LinkedHashSet<>();
            try (ResultSet rs = st.executeQuery("""
                    SELECT column_name FROM information_schema.statistics
                     WHERE table_schema = DATABASE() AND table_name = 'agent_preorder'
                       AND non_unique = 0
                     ORDER BY index_name, seq_in_index
                    """)) {
                while (rs.next()) {
                    uniqueColumns.add(rs.getString(1).toLowerCase());
                }
            }

            assertThat(uniqueColumns)
                    .as("幂等键必须有数据库级唯一约束兜底（改索引=改业务保证，必须显式确认）")
                    .contains("idempotency_key");

            st.execute("""
                    INSERT INTO agent_preorder (user_id, member_id, department, appointment_date, idempotency_key)
                    VALUES (1, 1, '心内科', '2026-12-01', 'it-key-dup')
                    """);

            assertThatThrownBy(() -> st.execute("""
                    INSERT INTO agent_preorder (user_id, member_id, department, appointment_date, idempotency_key)
                    VALUES (1, 1, '心内科', '2026-12-01', 'it-key-dup')
                    """)).as("重复幂等键必须被数据库拒绝")
                    .hasMessageContaining("Duplicate");
        }
    }
}