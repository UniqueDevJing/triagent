<template>
  <div>
    <div class="card">
      <div class="section-title">我的挂号 · REGISTRATIONS</div>
      <div class="toolbar">
        <el-input v-model="patientId" placeholder="患者ID" style="width:120px"></el-input>
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>科室</th><th>医生</th><th>日期</th><th>时段</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="r in regs" :key="r.id">
            <td>{{ r.id }}</td><td>{{ r.department_id }}</td><td>{{ r.doctor_id }}</td>
            <td>{{ r.work_date }}</td><td>{{ r.period }}</td>
            <td><span class="chip" :class="r.status==='DONE'?'ok':'warn'">{{ r.status }}</span></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!regs.length" class="dns">暂无挂号</p>
    </div>

    <div class="card">
      <div class="section-title">电子病历 · MEDICAL RECORDS</div>
      <table>
        <thead><tr><th>ID</th><th>医生</th><th>主诉</th><th>诊断</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in recs" :key="r.id">
            <td>{{ r.id }}</td><td>{{ r.doctor_name || r.doctor_id }}</td>
            <td>{{ r.chief_complaint }}</td><td>{{ r.diagnosis }}</td>
            <td><span class="chip" :class="r.status==='SUBMITTED'?'ok':'warn'">{{ r.status==='SUBMITTED'?'已归档':'医生书写中' }}</span></td>
            <td>
              <el-button size="small" type="primary" plain @click="open(r.id)">查看七段病历</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!recs.length" class="dns">暂无病历（医生接诊后会出现在这里）</p>
    </div>

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
  </div>
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

const patientId = ref(1)
const regs = ref([])
const recs = ref([])
const dlg = ref(false)
const cur = ref(null)

async function load() {
  regs.value = await api.get('/api/patient/registrations', { patientId: patientId.value })
  recs.value = await api.get('/api/patient/records', { patientId: patientId.value })
}
async function open(id) {
  cur.value = await api.get('/api/doctor/record', { recordId: id })
  dlg.value = true
}
onMounted(load)
</script>

<style scoped>
.seg { margin-bottom: 10px; }
.seg-title { font-weight: 600; color: #1f6feb; font-size: 13px; margin-bottom: 4px; }
.seg-body { line-height: 1.7; color: #333; background: #f7f9fb; border-radius: 6px; padding: 8px 12px; font-size: 13px; }
</style>
