<template>
  <main class="workspace doctor-workbench">
    <section class="doctor-brief">
      <div><span class="eyebrow">CLINICAL WORKSPACE</span><h2>{{ currentUser.name || currentUser.username }}医生，今天还有 {{ pendingCount }} 位患者。</h2><p>患者队列、健康背景与七段式病历集中在同一工作流中。</p></div>
      <button class="secondary-button" @click="load">刷新队列</button>
    </section>

    <section class="clinical-grid">
      <aside class="patient-queue surface">
        <header class="panel-head"><div><span class="eyebrow">TODAY'S QUEUE</span><h3>今日接诊</h3></div><span class="queue-count">{{ regs.length }}</span></header>
        <div class="queue-filter"><span class="active">全部</span><span>待接诊 {{ pendingCount }}</span><span>已完成 {{ regs.length - pendingCount }}</span></div>
        <div class="queue-list">
          <button v-for="row in regs" :key="row.registration_id" :class="['patient-row', { active: emr.registrationId === row.registration_id }]" @click="openEmr(row)">
            <span class="patient-avatar">{{ (row.patient_name || '患').slice(0, 1) }}</span>
            <span class="patient-summary"><b>{{ row.patient_name }}</b><small>{{ row.gender }} · {{ row.age }} 岁 · {{ row.period }}</small></span>
            <span :class="['queue-status', row.status === 'DONE' ? 'done' : 'waiting']">{{ row.status === 'DONE' ? '已完成' : '待接诊' }}</span>
          </button>
          <div v-if="!regs.length" class="empty-state"><span>✓</span><b>当前没有待诊患者</b><p>新挂号会自动进入这里。</p></div>
        </div>
      </aside>

      <article class="clinical-canvas surface">
        <template v-if="activePatient">
          <header class="patient-hero">
            <div class="patient-avatar patient-avatar--large">{{ (activePatient.patient_name || '患').slice(0, 1) }}</div>
            <div><span class="eyebrow">PATIENT / #{{ activePatient.registration_id }}</span><h3>{{ activePatient.patient_name }}</h3><p>{{ activePatient.gender }} · {{ activePatient.age }} 岁 · {{ activePatient.department }} · {{ activePatient.work_date }} {{ activePatient.period }}</p></div>
            <span :class="['queue-status', activePatient.status === 'DONE' ? 'done' : 'waiting']">{{ activePatient.status === 'DONE' ? '已结诊' : '接诊中' }}</span>
          </header>
          <div class="clinical-alerts"><div><span>既往史</span><b>{{ activePatient.past_history || '无特殊记录' }}</b></div><div :class="{ alert: activePatient.allergic_history }"><span>过敏史</span><b>{{ activePatient.allergic_history || '无记录' }}</b></div></div>
          <div class="record-progress"><span v-for="(label, i) in recordSections" :key="label" :class="{ filled: sectionFilled(i) }"><i>{{ i + 1 }}</i>{{ label }}</span></div>
          <div class="record-form" :class="{ locked: emr.locked }">
            <label><span><b>01</b>主诉</span><el-input v-model="emr.chief_complaint" :disabled="emr.locked" placeholder="症状、持续时间与就诊原因" /></label>
            <label><span><b>02</b>现病史</span><el-input v-model="emr.present_illness" :disabled="emr.locked" type="textarea" :rows="3" /></label>
            <div class="record-pair"><label><span><b>03</b>既往史</span><el-input v-model="emr.past_history" :disabled="emr.locked" type="textarea" :rows="2" /></label><label><span><b>04</b>过敏史</span><el-input v-model="emr.allergy_history" :disabled="emr.locked" type="textarea" :rows="2" /></label></div>
            <label><span><b>05</b>辅助检查</span><el-input v-model="emr.assistant_exam" :disabled="emr.locked" type="textarea" :rows="2" placeholder="血常规、影像或其他检查结果" /></label>
            <label><span><b>06</b>诊断</span><el-input v-model="emr.diagnosis" :disabled="emr.locked" placeholder="明确或初步诊断" /></label>
            <label><span><b>07</b>处理意见</span><el-input v-model="emr.treatment" :disabled="emr.locked" type="textarea" :rows="3" placeholder="用药、处置、复诊与注意事项" /></label>
          </div>
          <footer class="record-actions"><span>{{ emr.locked ? '病历已提交并锁定' : '提交后病历将锁定，无法再次编辑' }}</span><div><button v-if="!emr.id && !emr.locked" class="text-button" @click="finish(activePatient)">直接结诊</button><button v-if="!emr.locked" class="secondary-button" @click="saveDraft">保存草稿</button><button v-if="!emr.locked" class="primary-button" @click="submit">提交并完成接诊</button></div></footer>
        </template>
        <div v-else class="canvas-placeholder"><span class="welcome-mark">+</span><h3>从左侧选择一位患者</h3><p>患者健康背景与结构化病历将在这里展开。</p></div>
      </article>
    </section>

  </main>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const currentUser = JSON.parse(localStorage.getItem('hospital_user') || '{}')
const regs = ref([])
const activePatient = computed(() => regs.value.find(r => r.registration_id === emr.registrationId))
const pendingCount = computed(() => regs.value.filter(r => r.status !== 'DONE').length)
const recordSections = ['主诉', '现病史', '既往史', '过敏史', '检查', '诊断', '处理']
const emr = reactive({ visible: false, locked: false, id: null, registrationId: null,
  chief_complaint: '', present_illness: '', past_history: '', allergy_history: '',
  assistant_exam: '', diagnosis: '', treatment: '' })

onMounted(async () => {
  await load()
})

async function load() {
  regs.value = await api.get('/doctor/registrations')
}

function openEmr(row) {
  emr.visible = true
  emr.locked = row.record_status === 'SUBMITTED'
  emr.id = row.record_id || null
  emr.registrationId = row.registration_id
  emr.chief_complaint = ''; emr.present_illness = ''; emr.past_history = row.past_history || ''
  emr.allergy_history = row.allergic_history || ''; emr.assistant_exam = ''
  emr.diagnosis = ''; emr.treatment = ''
  if (row.record_id) {
    api.get('/doctor/record', { recordId: row.record_id }).then(d => {
      emr.chief_complaint = d.chief_complaint || ''; emr.present_illness = d.present_illness || ''
      emr.past_history = d.past_history || ''; emr.allergy_history = d.allergy_history || ''
      emr.assistant_exam = d.assistant_exam || ''; emr.diagnosis = d.diagnosis || ''; emr.treatment = d.treatment || ''
    })
  }
}

function sectionFilled(index) {
  const keys = ['chief_complaint', 'present_illness', 'past_history', 'allergy_history', 'assistant_exam', 'diagnosis', 'treatment']
  return Boolean(emr[keys[index]])
}

async function saveDraft() {
  const r = await api.post('/doctor/record/save', null, {
    id: emr.id, registration_id: emr.registrationId,
    patient_id: currentPatientId(),
    chief_complaint: emr.chief_complaint, present_illness: emr.present_illness,
    past_history: emr.past_history, allergy_history: emr.allergy_history,
    assistant_exam: emr.assistant_exam, diagnosis: emr.diagnosis, treatment: emr.treatment
  })
  emr.id = r.recordId
  ElMessage.success('草稿已保存，可继续编辑')
  load()
}

async function submit() {
  if (!emr.id) {
    const r = await api.post('/doctor/record/save', null, {
      id: null, registration_id: emr.registrationId,
      patient_id: currentPatientId(),
      chief_complaint: emr.chief_complaint, present_illness: emr.present_illness,
      past_history: emr.past_history, allergy_history: emr.allergy_history,
      assistant_exam: emr.assistant_exam, diagnosis: emr.diagnosis, treatment: emr.treatment
    })
    emr.id = r.recordId
  }
  await api.post('/doctor/record/submit?1=1', { recordId: emr.id, registrationId: emr.registrationId })
  ElMessage.success('病历已提交锁定，挂号已完成就诊')
  emr.visible = false
  load()
}

async function finish(row) {
  await api.post('/doctor/registration/done?1=1', { registrationId: row.registration_id })
  ElMessage.success('已结诊'); load()
}

function currentPatientId() {
  const row = regs.value.find(r => r.registration_id === emr.registrationId)
  return row ? row.patient_id : null
}
</script>
