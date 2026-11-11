<template>
  <div class="card">
    <div class="section-title">门诊接诊台 · 医生视角</div>
    <div style="margin-bottom:12px;display:flex;gap:10px;align-items:center">
      <span style="font-size:13px">当前医生：</span>
      <el-select v-model="doctorId" style="width:220px" @change="load">
        <el-option v-for="d in doctors" :key="d.id" :label="d.name + ' · ' + (d.title || '')" :value="d.id" />
      </el-select>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <el-table :data="regs" size="small" border>
      <el-table-column prop="registration_id" label="挂号ID" width="80" />
      <el-table-column prop="patient_name" label="患者" width="100" />
      <el-table-column label="性别/年龄" width="90"><template #default="{ row }">{{ row.gender }} / {{ row.age }}</template></el-table-column>
      <el-table-column prop="department" label="科室" width="100" />
      <el-table-column prop="work_date" label="就诊日" width="105" />
      <el-table-column prop="period" label="时段" width="65" />
      <el-table-column prop="status" label="挂号状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'DONE' ? 'info' : 'success'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="past_history" label="既往史" min-width="150" />
      <el-table-column prop="allergic_history" label="过敏史" width="120" />
      <el-table-column label="病历" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openEmr(row)">
            {{ row.record_id ? (row.record_status === 'SUBMITTED' ? '查看病历' : '继续编辑') : '写病历' }}
          </el-button>
          <el-button v-if="!row.record_id && row.status !== 'DONE'" size="small" link @click="finish(row)">直接结诊</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 七段病历书写对话框 -->
    <el-dialog v-model="emr.visible" title="病历书写 · 七段结构化" width="760px" top="4vh">
      <el-alert v-if="emr.locked" type="warning" :closable="false"
                title="该病历已提交锁定，只能查看" style="margin-bottom:12px" />
      <el-form label-width="90px" :disabled="emr.locked">
        <el-form-item label="① 主诉"><el-input v-model="emr.chief_complaint" placeholder="如：咳嗽伴发热 3 天" /></el-form-item>
        <el-form-item label="② 现病史"><el-input v-model="emr.present_illness" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="③ 既往史"><el-input v-model="emr.past_history" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="④ 过敏史"><el-input v-model="emr.allergy_history" /></el-form-item>
        <el-form-item label="⑤ 辅助检查"><el-input v-model="emr.assistant_exam" type="textarea" :rows="2" placeholder="血常规/胸片/CT 等结果" /></el-form-item>
        <el-form-item label="⑥ 诊断"><el-input v-model="emr.diagnosis" placeholder="如：急性上呼吸道感染" /></el-form-item>
        <el-form-item label="⑦ 处理意见"><el-input v-model="emr.treatment" type="textarea" :rows="3" placeholder="用药、复诊、注意事项" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emr.visible = false">关闭</el-button>
        <template v-if="!emr.locked">
          <el-button type="default" @click="saveDraft">保存草稿</el-button>
          <el-button type="primary" @click="submit">提交并锁定</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const doctors = ref([])
const doctorId = ref(null)
const regs = ref([])
const emr = reactive({ visible: false, locked: false, id: null, registrationId: null,
  chief_complaint: '', present_illness: '', past_history: '', allergy_history: '',
  assistant_exam: '', diagnosis: '', treatment: '' })

onMounted(async () => {
  doctors.value = await api.get('/admin/doctors')
  doctorId.value = doctors.value[0]?.id || null
  await load()
})

async function load() {
  if (!doctorId.value) return
  regs.value = await api.get('/doctor/registrations', { doctorId: doctorId.value })
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

async function saveDraft() {
  const r = await api.post('/doctor/record/save', null, {
    id: emr.id, registration_id: emr.registrationId,
    patient_id: currentPatientId(), doctor_id: doctorId.value,
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
      patient_id: currentPatientId(), doctor_id: doctorId.value,
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
