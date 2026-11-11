<template>
  <main class="workspace records-workspace">
    <section class="page-intro"><div><span class="eyebrow">MY CARE JOURNEY</span><h2>我的就诊时间线</h2><p>挂号、接诊与电子病历汇集在一条清晰的健康记录中。</p></div><button class="secondary-button" @click="load">刷新记录</button></section>
    <section class="record-summary"><div><strong>{{ regs.length }}</strong><span>挂号记录</span></div><div><strong>{{ recs.length }}</strong><span>电子病历</span></div><div><strong>{{ recs.filter(r => r.status === 'SUBMITTED').length }}</strong><span>已归档病历</span></div></section>
    <section class="care-timeline">
      <article v-for="r in regs" :key="`reg-${r.id}`" class="care-event"><div class="event-line"><i></i></div><div class="event-date"><strong>{{ r.work_date }}</strong><small>{{ r.period }}</small></div><div class="event-card"><header><span class="eyebrow">REGISTRATION / #{{ r.id }}</span><span :class="['queue-status', r.status === 'DONE' ? 'done' : 'waiting']">{{ r.status === 'DONE' ? '已完成' : '待就诊' }}</span></header><h3>预约就诊</h3><p>科室 {{ r.department_id }} · 医生 {{ r.doctor_id }}</p></div></article>
      <article v-for="r in recs" :key="`rec-${r.id}`" class="care-event"><div class="event-line"><i></i></div><div class="event-date"><strong>病历</strong><small>#{{ r.id }}</small></div><button class="event-card event-card--record" @click="open(r.id)"><header><span class="eyebrow">MEDICAL RECORD</span><span :class="['queue-status', r.status === 'SUBMITTED' ? 'done' : 'waiting']">{{ r.status === 'SUBMITTED' ? '已归档' : '书写中' }}</span></header><h3>{{ r.diagnosis || '诊断待完善' }}</h3><p>{{ r.chief_complaint || '暂无主诉' }}</p><footer>{{ r.doctor_name || r.doctor_id }} 医生 <span>查看完整病历 →</span></footer></button></article>
      <div v-if="!regs.length && !recs.length" class="surface empty-state"><span>＋</span><b>还没有就诊记录</b><p>完成首次预问诊与挂号后，记录会出现在这里。</p></div>
    </section>

    <el-dialog v-model="dlg" title="病历详情 · 七段结构化" width="640px">
      <template v-if="cur">
        <p class="dns" style="margin:0 0 10px;">{{ cur.patient_name || '患者' }} · {{ cur.doctor_name || '-' }} 医生
          · <span class="chip" :class="cur.status==='SUBMITTED'?'ok':'warn'">{{ cur.status==='SUBMITTED'?'已归档':'草稿' }}</span></p>
        <div v-for="s in SEGS" :key="s.key" class="seg">
          <div class="seg-title">{{ s.no }} {{ s.label }}</div>
          <div class="seg-body">{{ cur[s.key] || '（暂无记录）' }}</div>
        </div>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'

const SEGS = [
  { key: 'chief_complaint', no: '①', label: '主诉' },
  { key: 'present_illness', no: '②', label: '现病史' },
  { key: 'past_history',    no: '③', label: '既往史' },
  { key: 'allergy_history', no: '④', label: '过敏史' },
  { key: 'assistant_exam',  no: '⑤', label: '辅助检查' },
  { key: 'diagnosis',       no: '⑥', label: '诊断' },
  { key: 'treatment',       no: '⑦', label: '处理意见' }
]

const regs = ref([])
const recs = ref([])
const dlg = ref(false)
const cur = ref(null)

async function load() {
  regs.value = await api.get('/patient/registrations')
  recs.value = await api.get('/patient/records')
}
async function open(id) {
  cur.value = await api.get('/patient/record/detail', { recordId: id })
  dlg.value = true
}
onMounted(load)
</script>

<style scoped>
.seg { margin-bottom: 10px; }
.seg-title { font-weight: 600; color: var(--vital-teal-d); font-size: 12px; margin-bottom: 4px; }
.seg-body { line-height: 1.7; color: var(--vital-ink); background: var(--vital-panel-soft); border-radius: 8px; padding: 10px 12px; font-size: 12px; }
</style>
