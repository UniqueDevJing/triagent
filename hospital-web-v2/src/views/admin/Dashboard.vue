<template>
  <div>
    <div class="card">
      <div class="section-title">运营总览 · OVERVIEW</div>
      <div class="stat-grid">
        <div class="stat"><div class="num">{{ ov.totalSuggestion }}</div><div class="lbl">分诊建议总数</div></div>
        <div class="stat"><div class="num">{{ ov.hitRate }}%</div><div class="lbl">挂号命中率</div></div>
        <div class="stat danger" v-if="ov.redFlag"><div class="num">{{ ov.redFlag }}</div><div class="lbl">危急硬拦截</div></div>
        <div class="stat warn"><div class="num">{{ ov.pendingTriage }}</div><div class="lbl">待处理预问诊</div></div>
        <div class="stat"><div class="num">{{ ov.doctorOnline }}</div><div class="lbl">在线医生</div></div>
      </div>
    </div>

    <div class="card">
      <div class="section-title">分科室命中率 · BY DEPARTMENT</div>
      <table>
        <thead><tr><th>科室</th><th>建议数</th><th>已挂号</th><th>命中率</th></tr></thead>
        <tbody>
          <tr v-for="d in byDept" :key="d.department">
            <td>{{ d.department }}</td><td>{{ d.suggestion }}</td><td>{{ d.registered }}</td>
            <td><span class="readout">{{ d.hit_rate }}%</span></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div class="section-title">Agent 运行轨迹 · RUNS</div>
      <table>
        <thead><tr><th>run_id</th><th>科室</th><th>医生</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in runs" :key="r.run_id">
            <td><code>{{ r.run_id }}</code></td>
            <td>{{ r.final_department_name || '—' }}</td>
            <td>{{ r.final_doctor_name || '—' }}</td>
            <td><span class="chip" :class="r.status==='DONE'?'ok':'fail'">{{ r.status }}</span></td>
            <td><el-button size="small" @click="selectRun(r.run_id)">看时间线</el-button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card" v-if="selected">
      <div class="section-title">时间线 · run #{{ selected }}</div>
      <table>
        <thead><tr><th>step</th><th>工具</th><th>输入</th><th>输出</th><th>耗时</th></tr></thead>
        <tbody>
          <tr v-for="s in timeline" :key="s.step_index">
            <td>{{ s.step_index }}</td><td><code>{{ s.tool_code }}</code></td>
            <td class="dns">{{ short(s.input_json) }}</td>
            <td class="dns">{{ short(s.output_json) }}</td>
            <td>{{ s.cost_ms }}ms</td>
          </tr>
        </tbody>
      </table>
      <div class="section-title" style="margin-top:16px;">工具调用日志 · TOOL LOGS</div>
      <table>
        <thead><tr><th>工具</th><th>来源</th><th>状态</th><th>耗时</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="l in toolLogs" :key="l.id">
            <td><code>{{ l.tool_code }}</code></td><td>{{ l.source }}</td>
            <td><span class="chip" :class="l.status==='SUCCESS'?'ok':'fail'">{{ l.status }}</span></td>
            <td>{{ l.cost_ms }}ms</td><td class="dns">{{ l.create_time }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'

const ov = ref({})
const byDept = ref([])
const runs = ref([])
const selected = ref(null)
const timeline = ref([])
const toolLogs = ref([])

function short(s) { if (!s) return '—'; return s.length > 60 ? s.slice(0, 60) + '…' : s }
async function load() {
  ov.value = await api.get('/api/admin/dashboard/overview')
  byDept.value = await api.get('/api/admin/dashboard/by-department')
  runs.value = await api.get('/api/admin/dashboard/runs')
}
async function selectRun(runId) {
  selected.value = runId
  timeline.value = await api.get('/api/admin/dashboard/timeline', { runId })
  toolLogs.value = await api.get('/api/admin/dashboard/tool-logs', { runId })
}
onMounted(load)
</script>
