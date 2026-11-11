<template>
  <div>
    <div class="card">
      <div class="section-title">我的挂号 · REGISTRATIONS</div>
      <div class="toolbar">
        <el-input v-model="patientId" placeholder="患者ID" style="width:120px"></el-input>
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>科室</th><th>医生</th><th>日期</th><th>时段</th></tr></thead>
        <tbody>
          <tr v-for="r in regs" :key="r.id">
            <td>{{ r.id }}</td><td>{{ r.department_id }}</td><td>{{ r.doctor_id }}</td>
            <td>{{ r.work_date }}</td><td>{{ r.period }}</td>
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
            <td><span class="chip" :class="r.status==='SIGNED'?'ok':'warn'">{{ r.status }}</span></td>
            <td>
              <el-button v-if="r.status!=='SIGNED'" size="small" type="primary" plain
                         @click="sign(r.id)">签名归档</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!recs.length" class="dns">暂无病历</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const patientId = ref(1)
const regs = ref([])
const recs = ref([])
async function load() {
  regs.value = await api.get('/api/patient/registrations', { patientId: patientId.value })
  recs.value = await api.get('/api/patient/records', { patientId: patientId.value })
}
async function sign(id) {
  await api.post('/api/patient/record/sign', { recordId: id })
  ElMessage.success('已签名归档')
  load()
}
onMounted(load)
</script>
