<template>
  <main class="workspace command-center">
    <section class="workspace-hero workspace-hero--dark">
      <div>
        <span class="eyebrow">TRIAGENT / LIVE OPERATIONS</span>
        <h2>让每一次分诊决策<br>都清晰、可追踪。</h2>
        <p>从患者意图识别到号源落位，统一观察 Agent 推理、工具调用与临床安全拦截。</p>
      </div>
      <div class="system-pulse">
        <span class="pulse-orbit"><i></i></span>
        <div><strong>系统运行中</strong><small>{{ ov.doctorOnline || 0 }} 位医生在线 · {{ runs.length }} 条执行轨迹</small></div>
      </div>
    </section>

    <section class="metric-grid metric-grid--four" aria-label="运营核心指标">
      <article class="metric-tile"><span>分诊建议</span><strong>{{ ov.totalSuggestion || 0 }}</strong><small>累计生成的临床建议</small></article>
      <article class="metric-tile"><span>转化命中率</span><strong>{{ ov.hitRate || 0 }}<em>%</em></strong><small>建议后完成挂号</small></article>
      <article class="metric-tile"><span>号源使用率</span><strong>{{ enh.slotUsage || '0%' }}</strong><small>{{ enh.registrations || 0 }} 次有效挂号</small></article>
      <article class="metric-tile" :class="{ 'metric-tile--alert': ov.redFlag }"><span>安全硬拦截</span><strong>{{ ov.redFlag || 0 }}</strong><small>{{ ov.pendingTriage || 0 }} 个会话待分诊</small></article>
    </section>

    <section class="dashboard-grid">
      <article class="surface pipeline-panel">
        <header class="panel-head"><div><span class="eyebrow">AGENT PIPELINE</span><h3>实时决策链</h3></div><span class="live-badge"><i></i> LIVE</span></header>
        <div class="pipeline-map">
          <div class="pipeline-node"><span>01</span><b>症状理解</b><small>提取主诉与风险</small></div><i></i>
          <div class="pipeline-node"><span>02</span><b>安全校验</b><small>红旗征象硬拦截</small></div><i></i>
          <div class="pipeline-node"><span>03</span><b>资源匹配</b><small>科室、医生与号源</small></div><i></i>
          <div class="pipeline-node pipeline-node--active"><span>04</span><b>建议生成</b><small>可解释推荐结果</small></div>
        </div>
        <div class="run-feed">
          <button v-for="r in runs.slice(0, 5)" :key="r.run_id" :class="['run-row', { active: selected === r.run_id }]" @click="selectRun(r.run_id)">
            <span class="run-state" :class="r.status === 'DONE' ? 'ok' : 'fail'"></span>
            <code>#{{ r.run_id }}</code>
            <span>{{ r.final_department_name || '正在决策' }}</span>
            <span>{{ r.final_doctor_name || '待匹配医生' }}</span>
            <b>{{ r.status }}</b>
          </button>
          <div v-if="!runs.length" class="empty-inline">暂无执行轨迹</div>
        </div>
      </article>

      <article class="surface department-panel">
        <header class="panel-head"><div><span class="eyebrow">DEPARTMENT FIT</span><h3>科室转化表现</h3></div></header>
        <div class="rank-list">
          <div v-for="(d, i) in byDept" :key="d.department" class="rank-row">
            <span class="rank-index">{{ String(i + 1).padStart(2, '0') }}</span>
            <div><b>{{ d.department }}</b><small>{{ d.registered }} / {{ d.suggestion }} 已挂号</small><span class="progress-track"><i :style="{ width: `${Math.min(d.hit_rate || 0, 100)}%` }"></i></span></div>
            <strong>{{ d.hit_rate }}%</strong>
          </div>
          <div v-if="!byDept.length" class="empty-inline">暂无科室数据</div>
        </div>
      </article>
    </section>

    <section class="surface capacity-panel">
      <header class="panel-head"><div><span class="eyebrow">CLINICAL CAPACITY</span><h3>医生与号源负载</h3></div><span class="panel-note">按当前排班实时计算</span></header>
      <div class="doctor-grid">
        <article v-for="d in (enh.byDoctor || [])" :key="d.id" class="doctor-capacity">
          <div class="doctor-avatar">{{ (d.name || '医').slice(0, 1) }}</div>
          <div class="doctor-meta"><b>{{ d.name }}</b><small>{{ d.department }} · {{ d.title }}</small></div>
          <div class="capacity-readout"><strong>{{ d.used }}<em>/{{ d.slots }}</em></strong><small>已用号源</small></div>
          <span class="progress-track"><i :style="{ width: `${capacity(d)}%` }"></i></span>
        </article>
        <div v-if="!(enh.byDoctor || []).length" class="empty-inline">暂无医生负载数据</div>
      </div>
    </section>

    <section v-if="selected" class="surface trace-panel">
      <header class="panel-head"><div><span class="eyebrow">TRACE / #{{ selected }}</span><h3>决策执行详情</h3></div><button class="text-button" @click="selected = null">关闭详情</button></header>
      <div class="trace-grid">
        <div class="timeline-list">
          <article v-for="s in timeline" :key="s.step_index" class="timeline-step">
            <span>{{ s.step_index }}</span><div><b>{{ s.tool_code }}</b><small>{{ short(s.input_json) }}</small><p>{{ short(s.output_json) }}</p></div><em>{{ s.cost_ms }}ms</em>
          </article>
        </div>
        <div class="tool-log-list">
          <article v-for="l in toolLogs" :key="l.id"><span class="run-state" :class="l.status === 'SUCCESS' ? 'ok' : 'fail'"></span><div><b>{{ l.tool_code }}</b><small>{{ l.source }} · {{ l.create_time }}</small></div><em>{{ l.cost_ms }}ms</em></article>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'

const ov = ref({})
const byDept = ref([])
const runs = ref([])
const enh = ref({})
const selected = ref(null)
const timeline = ref([])
const toolLogs = ref([])

function short(s) { if (!s) return '—'; return s.length > 60 ? s.slice(0, 60) + '…' : s }
function capacity(d) { return d.slots ? Math.min(Math.round((d.used / d.slots) * 100), 100) : 0 }
async function load() {
  ov.value = await api.get('/admin/dashboard/overview')
  byDept.value = await api.get('/admin/dashboard/by-department')
  runs.value = await api.get('/admin/dashboard/runs')
  enh.value = await api.get('/admin/dashboard/enhanced')
}
async function selectRun(runId) {
  selected.value = runId
  timeline.value = await api.get('/admin/dashboard/timeline', { runId })
  toolLogs.value = await api.get('/admin/dashboard/tool-logs', { runId })
}
onMounted(load)
</script>
