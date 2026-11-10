package com.health.web.assistant.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.health.web.assistant.tool.BudgetGuard;
import com.health.web.assistant.tool.ToolAccessPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TriAgent 离线评测基线（T2/T3）：
 * 150 个确定性案例，覆盖路由 / 会话连续性 / 分诊状态机轨迹 / 红旗与安全 / 权限矩阵 / 预算熔断。
 * 一条命令出报告：mvn test -Dtest=TriageEvalHarnessTest
 * 报告落盘 evals/reports/（latest-report.json + latest.md）。
 * 定位：重构门禁基线 —— 任何编排层重构后重跑，成功率降幅不得超过 2pp。
 */
class TriageEvalHarnessTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static Path repoRoot;
    private static AgentRegistry registry;
    private static SymptomStateMachine stateMachine;
    private static AssistantOrchestrator orchestrator;
    private static ToolAccessPolicy policy;

    private record CaseResult(String id, String category, boolean passed, String expected, String actual, long nanos) {}

    @BeforeAll
    static void setup() {
        Path cwd = Path.of(System.getProperty("user.dir"));
        repoRoot = "health-admin".equals(cwd.getFileName() == null ? "" : cwd.getFileName().toString())
                ? cwd.getParent() : cwd;
        policy = new ToolAccessPolicy();
        registry = new AgentRegistry(policy);
        stateMachine = new SymptomStateMachine();
        // decideAgent 不依赖任何实例字段，null 依赖即可构造（advices 传空列表避免 NPE）
        orchestrator = new AssistantOrchestrator(null, registry, null, null, null, null,
                null, null, null, List.of(), null, null, null, null);
    }

    private static List<Map<String, Object>> load(String file) throws IOException {
        Path p = repoRoot.resolve("evals/cases").resolve(file);
        assertTrue(Files.exists(p), "案例文件不存在: " + p);
        return MAPPER.readValue(Files.readString(p, StandardCharsets.UTF_8),
                new TypeReference<List<Map<String, Object>>>() {});
    }

    @Test
    void evalBaseline() throws IOException {
        List<CaseResult> results = new ArrayList<>();
        results.addAll(evalRouting());
        results.addAll(evalContinuity());
        results.addAll(evalStateMachine());
        results.addAll(evalEmergency());
        results.addAll(evalPermission());
        results.addAll(evalBudget());

        assertEquals(150, results.size(), "案例总数必须为 150（数据集口径）");
        writeReport(results);

        List<String> failures = results.stream().filter(r -> !r.passed())
                .map(r -> r.id() + " 期望=" + r.expected() + " 实际=" + r.actual())
                .toList();
        assertTrue(failures.isEmpty(),
                "评测未全通过 " + (results.size() - failures.size()) + "/" + results.size() + ":\n"
                        + String.join("\n", failures));
    }

    // ---------- 各维度评测 ----------

    private List<CaseResult> evalRouting() throws IOException {
        List<CaseResult> out = new ArrayList<>();
        for (Map<String, Object> c : load("routing.json")) {
            long t0 = System.nanoTime();
            String msg = str(c, "message");
            String actual = registry.resolve(msg).getType().name();
            out.add(new CaseResult(str(c, "id"), "routing", actual.equals(str(c, "expect")),
                    str(c, "expect"), actual, System.nanoTime() - t0));
        }
        return out;
    }

    private List<CaseResult> evalContinuity() throws IOException {
        List<CaseResult> out = new ArrayList<>();
        for (Map<String, Object> c : load("continuity.json")) {
            long t0 = System.nanoTime();
            String fresh = str(c, "fresh");
            String current = c.get("current") == null ? null : str(c, "current");
            String actual = orchestrator.decideAgent(AgentType.valueOf(fresh), current, str(c, "message")).name();
            out.add(new CaseResult(str(c, "id"), "continuity", actual.equals(str(c, "expect")),
                    str(c, "expect"), actual, System.nanoTime() - t0));
        }
        return out;
    }

    private List<CaseResult> evalStateMachine() throws IOException {
        List<CaseResult> out = new ArrayList<>();
        for (Map<String, Object> c : load("state-machine.json")) {
            long t0 = System.nanoTime();
            String sessionId = "eval-" + str(c, "id");
            List<String> turns = strList(c, "turns");
            List<String> expTypes = strList(c, "expectDecisions");
            List<Object> expMissing = castList(c.get("expectMissing"));
            List<String> actTypes = new ArrayList<>();
            List<List<String>> actMissing = new ArrayList<>();
            for (String turn : turns) {
                SymptomStateMachine.Decision d = stateMachine.decide(sessionId, turn);
                actTypes.add(d.type().name());
                actMissing.add(d.missingFields());
            }
            boolean ok = actTypes.equals(expTypes);
            if (ok && expMissing != null) {
                for (int i = 0; i < expMissing.size() && ok; i++) {
                    Object exp = expMissing.get(i);
                    List<String> expList = exp == null ? List.of() : new ArrayList<>();
                    if (exp != null) {
                        for (Object o : (List<?>) exp) {
                            expList.add(String.valueOf(o));
                        }
                    }
                    ok = expList.equals(actMissing.get(i));
                }
            }
            out.add(new CaseResult(str(c, "id"), "state-machine", ok,
                    expTypes.toString(), actTypes.toString(), System.nanoTime() - t0));
        }
        return out;
    }

    private List<CaseResult> evalEmergency() throws IOException {
        List<CaseResult> out = new ArrayList<>();
        for (Map<String, Object> c : load("emergency.json")) {
            long t0 = System.nanoTime();
            String id = str(c, "id");
            String msg = str(c, "message");
            boolean ok;
            String expected;
            String actual;
            if ("override".equals(c.getOrDefault("kind", ""))) {
                boolean expOverride = Boolean.TRUE.equals(c.get("expectOverride"));
                boolean actOverride = stateMachine.emergencyOverrideClearsFact(msg);
                ok = expOverride == actOverride;
                expected = "override=" + expOverride;
                actual = "override=" + actOverride;
            } else {
                List<String> expFlags = new ArrayList<>(strList(c, "expectRedFlags"));
                SymptomStateMachine.Decision d = stateMachine.decide("eval-" + id, msg);
                Set<String> actFlags = new HashSet<>(d.redFlagWords());
                ok = d.type() == SymptomStateMachine.DecisionType.EMERGENCY
                        && actFlags.equals(new HashSet<>(expFlags));
                expected = "EMERGENCY " + new TreeSet<>(expFlags);
                actual = d.type() + " " + new TreeSet<>(actFlags);
            }
            out.add(new CaseResult(id, "emergency", ok, expected, actual, System.nanoTime() - t0));
        }
        return out;
    }

    private List<CaseResult> evalPermission() throws IOException {
        List<CaseResult> out = new ArrayList<>();
        for (Map<String, Object> c : load("permission.json")) {
            long t0 = System.nanoTime();
            boolean actual = policy.isAllowed(AgentType.valueOf(str(c, "role")), str(c, "tool"));
            boolean expected = Boolean.TRUE.equals(c.get("expect"));
            out.add(new CaseResult(str(c, "id"), "permission", actual == expected,
                    String.valueOf(expected), String.valueOf(actual), System.nanoTime() - t0));
        }
        return out;
    }

    private List<CaseResult> evalBudget() throws IOException {
        List<CaseResult> out = new ArrayList<>();
        for (Map<String, Object> c : load("budget.json")) {
            long t0 = System.nanoTime();
            String sessionId = "eval-" + str(c, "id");
            BudgetGuard guard = new BudgetGuard();
            Map<?, ?> setup = (Map<?, ?>) c.get("setup");
            ReflectionTestUtils.setField(guard, "enabled", bool(setup, "enabled", true));
            ReflectionTestUtils.setField(guard, "maxToolCalls", intOf(setup.get("maxToolCalls"), 20));
            ReflectionTestUtils.setField(guard, "maxTokens", intOf(setup.get("maxTokens"), 60000));
            for (Map<String, Object> step : castStepList(c.get("steps"))) {
                if ("tool".equals(step.get("type"))) {
                    int times = step.get("times") == null ? 1 : intOf(step.get("times"), 1);
                    for (int i = 0; i < times; i++) {
                        guard.tryConsumeToolCall(sessionId);
                    }
                }
                if ("tokens".equals(step.get("type"))) {
                    int chars = intOf(step.get("chars"), 0);
                    guard.recordTokens(sessionId, "x".repeat(chars));
                }
            }
            BudgetGuard.BudgetStatus st = guard.status(sessionId);
            Map<?, ?> exp = (Map<?, ?>) c.get("expect");
            List<String> mismatches = new ArrayList<>();
            if (exp.containsKey("toolCalls") && st.toolCalls() != intOf(exp.get("toolCalls"), 0)) {
                mismatches.add("toolCalls 期望" + exp.get("toolCalls") + " 实际" + st.toolCalls());
            }
            if (exp.containsKey("tokens") && st.tokens() != intOf(exp.get("tokens"), 0)) {
                mismatches.add("tokens 期望" + exp.get("tokens") + " 实际" + st.tokens());
            }
            if (exp.containsKey("toolExceeded") && st.toolExceeded() != Boolean.parseBoolean(String.valueOf(exp.get("toolExceeded")))) {
                mismatches.add("toolExceeded 期望" + exp.get("toolExceeded") + " 实际" + st.toolExceeded());
            }
            if (exp.containsKey("tokenExceeded") && st.tokenExceeded() != Boolean.parseBoolean(String.valueOf(exp.get("tokenExceeded")))) {
                mismatches.add("tokenExceeded 期望" + exp.get("tokenExceeded") + " 实际" + st.tokenExceeded());
            }
            Map<?, ?> probe = (Map<?, ?>) c.get("probe");
            if (probe != null && probe.containsKey("consume")) {
                boolean actConsume = guard.tryConsumeToolCall(sessionId);
                boolean expConsume = Boolean.parseBoolean(String.valueOf(probe.get("consume")));
                if (actConsume != expConsume) {
                    mismatches.add("consume 期望" + expConsume + " 实际" + actConsume);
                }
            }
            out.add(new CaseResult(str(c, "id"), "budget", mismatches.isEmpty(),
                    String.valueOf(exp), mismatches.isEmpty() ? "OK" : String.join("; ", mismatches),
                    System.nanoTime() - t0));
        }
        return out;
    }

    // ---------- 报告 ----------

    private void writeReport(List<CaseResult> results) throws IOException {
        Map<String, List<CaseResult>> byCat = new LinkedHashMap<>();
        for (CaseResult r : results) {
            byCat.computeIfAbsent(r.category(), k -> new ArrayList<>()).add(r);
        }
        List<Map<String, Object>> catStats = new ArrayList<>();
        for (Map.Entry<String, List<CaseResult>> e : byCat.entrySet()) {
            long passed = e.getValue().stream().filter(CaseResult::passed).count();
            catStats.add(Map.of("category", e.getKey(), "total", e.getValue().size(),
                    "passed", passed, "failed", e.getValue().size() - passed,
                    "passRate", percent(passed, e.getValue().size())));
        }
        long passed = results.stream().filter(CaseResult::passed).count();
        List<Long> nanos = results.stream().map(CaseResult::nanos).sorted().toList();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("total", results.size());
        report.put("passed", passed);
        report.put("failed", results.size() - passed);
        report.put("passRate", percent(passed, results.size()));
        report.put("harnessP50us", nanos.get(nanos.size() / 2) / 1000);
        report.put("harnessP95us", nanos.get((int) (nanos.size() * 0.95)) / 1000);
        report.put("byCategory", catStats);
        List<Map<String, Object>> failedCases = new ArrayList<>();
        for (CaseResult r : results) {
            if (!r.passed()) {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("id", r.id());
                f.put("category", r.category());
                f.put("expected", r.expected());
                f.put("actual", r.actual());
                failedCases.add(f);
            }
        }
        report.put("failedCases", failedCases);

        Path reports = repoRoot.resolve("evals/reports");
        Files.createDirectories(reports);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Files.writeString(reports.resolve("report-" + stamp + ".json"),
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(report), StandardCharsets.UTF_8);
        Files.writeString(reports.resolve("latest-report.json"),
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(report), StandardCharsets.UTF_8);

        StringBuilder md = new StringBuilder();
        md.append("# TriAgent 评测基线报告\n\n");
        md.append("- 时间: ").append(report.get("timestamp")).append('\n');
        md.append("- 总案例: ").append(results.size()).append(" 通过: ").append(passed)
                .append(" 失败: ").append(results.size() - passed)
                .append(" 通过率: ").append(report.get("passRate")).append('\n');
        md.append("- harness 单案例耗时 us: P50=").append(report.get("harnessP50us"))
                .append(" P95=").append(report.get("harnessP95us")).append("\n\n");
        md.append("| 维度 | 案例 | 通过 | 失败 | 通过率 |\n|---|---|---|---|---|\n");
        for (Map<String, Object> s : catStats) {
            md.append("| ").append(s.get("category")).append(" | ").append(s.get("total"))
                    .append(" | ").append(s.get("passed")).append(" | ").append(s.get("failed"))
                    .append(" | ").append(s.get("passRate")).append(" |\n");
        }
        if (!failedCases.isEmpty()) {
            md.append("\n## 失败案例\n\n");
            for (Map<String, Object> f : failedCases) {
                md.append("- ").append(f.get("id")).append(" (").append(f.get("category"))
                        .append(") 期望=").append(f.get("expected")).append(" 实际=").append(f.get("actual")).append('\n');
            }
        }
        Files.writeString(reports.resolve("latest.md"), md.toString(), StandardCharsets.UTF_8);
        System.out.println("[eval] 报告已写入 " + reports.resolve("latest.md"));
        System.out.println(md);
    }

    // ---------- 工具 ----------

    private static String str(Map<String, Object> m, String k) {
        return String.valueOf(m.get(k));
    }

    private static boolean bool(Map<?, ?> m, String k, boolean def) {
        return m == null || !m.containsKey(k) ? def : Boolean.parseBoolean(String.valueOf(m.get(k)));
    }

    private static int intOf(Object v, int def) {
        if (v == null) {
            return def;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static List<String> strList(Map<String, Object> m, String k) {
        List<String> out = new ArrayList<>();
        for (Object o : (List<?>) m.get(k)) {
            out.add(String.valueOf(o));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(Object o) {
        return o == null ? null : (List<Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castStepList(Object o) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object e : (List<?>) o) {
            out.add((Map<String, Object>) e);
        }
        return out;
    }

    private static String percent(long passed, long total) {
        return Math.round(passed * 1000.0 / total) / 10.0 + "%";
    }
}
