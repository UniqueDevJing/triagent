<template>
  <div style="display:grid;grid-template-columns:1fr 360px;gap:16px;">
    <div class="card">
      <div class="section-title">智能预问诊 · PRE-TRIAGE (SSE)</div>
      <div class="toolbar">
        <el-input v-model="patientId" placeholder="患者ID" style="width:120px"></el-input>
        <el-button @click="start">开始会话</el-button>
        <span class="readout" v-if="sessionId">session #{{ sessionId }}</span>
      </div>
      <div class="chat-box" ref="box">
        <div v-for="(m, i) in msgs" :key="i" class="msg" :class="m.role">
          <div class="bubble">{{ m.text }}<span v-if="m.typing" class="readout">▍</span></div>
        </div>
      </div>
      <div class="toolbar" style="margin-top:12px;">
        <el-input v-model="input" placeholder="描述你的症状…" style="flex:1" @keyup.enter="send"></el-input>
        <el-button type="primary" @click="send" :disabled="!sessionId || streaming">发送</el-button>
        <el-button type="success" @click="doTriage" :disabled="!sessionId || streaming || suggested">发起分诊</el-button>
      </div>
    </div>

    <div>
      <div class="card" v-if="suggested">
        <div class="section-title">分诊建议 · RESULT</div>
        <div v-if="sug.red_flag" class="chip red" style="display:inline-block;margin-bottom:10px;">⚠ 危急硬拦截 · 已转急诊绿色通道</div>
        <table>
          <tbody>
            <tr><td class="dns">推荐科室</td><td><b>{{ sug.department_name }}</b></td></tr>
            <tr><td class="dns">推荐医生</td><td>{{ sug.doctor_name }}</td></tr>
            <tr><td class="dns">建议时段</td><td>{{ sug.suggested_date }}</td></tr>
            <tr><td class="dns">置信度</td><td><span class="readout">{{ (sug.confidence*100).toFixed(0) }}%</span></td></tr>
            <tr><td class="dns">依据</td><td style="line-height:1.6;">{{ sug.reason }}</td></tr>
          </tbody>
        </table>
        <el-button type="primary" style="margin-top:12px;width:100%" @click="doRegister"
                   :disabled="registered">挂号预约</el-button>
        <p v-if="registered" class="readout" style="margin-top:8px;">已挂号：{{ reg.department_id }} 科，{{ reg.work_date }} {{ reg.period }}</p>
      </div>
      <div class="card" v-else>
        <div class="section-title">说明</div>
        <p class="dns" style="line-height:1.7;">
          1. 点「开始会话」建立预问诊 session。<br>
          2. 描述症状，模型会多轮追问（SSE 真流式）。<br>
          3. 点「发起分诊」触发 ReAct Agent：先查危急征象 → 工具调用 → 代码二次核验 → 出建议。<br>
          4. 命中危急（如胸痛大汗）会硬拦截转急诊科。
        </p>
      </div>

      <div class="card" style="margin-top:16px;">
        <div class="section-title">个人档案 · PROFILE</div>
        <table>
          <tbody>
            <tr><td class="dns">姓名</td><td><el-input v-model="profile.name" size="small" style="width:160px"></el-input></td></tr>
            <tr><td class="dns">性别</td><td>
              <el-radio-group v-model="profile.gender" size="small">
                <el-radio-button label="男">男</el-radio-button>
                <el-radio-button label="女">女</el-radio-button>
              </el-radio-group></td></tr>
            <tr><td class="dns">年龄</td><td><el-input-number v-model="profile.age" size="small" :min="0" :max="120" style="width:130px"></el-input-number></td></tr>
            <tr><td class="dns">慢病史</td><td><el-input v-model="profile.past_history" type="textarea" :rows="2" placeholder="如：高血压 8 年"></el-input></td></tr>
            <tr><td class="dns">过敏史</td><td><el-input v-model="profile.allergic_history" type="textarea" :rows="2" placeholder="如：青霉素过敏"></el-input></td></tr>
          </tbody>
        </table>
        <el-button type="primary" style="margin-top:12px;width:100%" :loading="saving" @click="saveProfile">保存档案</el-button>
        <p class="dns" style="margin:8px 0 0;">档案会进入分诊上下文，慢病史/过敏史参与推荐与安全校验。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const patientId = ref(1)
const sessionId = ref(null)
const msgs = ref([])
const input = ref('')
const streaming = ref(false)
const suggested = ref(false)
const sug = ref({})
const registered = ref(false)
const reg = ref({})
const box = ref(null)
const saving = ref(false)
const profile = reactive({ name: '', gender: '男', age: null, past_history: '', allergic_history: '' })

async function loadProfile() {
  const p = await api.get('/api/patient/profile', { patientId: patientId.value })
  if (p) Object.assign(profile, {
    name: p.name || '', gender: p.gender || '男', age: p.age,
    past_history: p.past_history || '', allergic_history: p.allergic_history || ''
  })
}
async function saveProfile() {
  saving.value = true
  try {
    await api.post('/api/patient/profile/save', null, { id: patientId.value, ...profile })
    ElMessage.success('档案已保存')
  } finally { saving.value = false }
}
watch(patientId, loadProfile)
onMounted(loadProfile)

function scroll() { nextTick(() => { if (box.value) box.value.scrollTop = box.value.scrollHeight }) }
async function start() {
  const r = await api.post('/api/patient/session', { patientId: patientId.value })
  sessionId.value = r.sessionId
  msgs.value = []
  suggested.value = false; registered.value = false; sug.value = {}
  ElMessage.success('会话已建立')
}
async function send() {
  const text = input.value.trim()
  if (!text || streaming.value) return
  msgs.value.push({ role: 'user', text })
  input.value = ''
  streaming.value = true
  const assistant = { role: 'bot', text: '', typing: true }
  msgs.value.push(assistant)
  scroll()
  try {
    const url = `/v2/api/patient/chat?sessionId=${sessionId.value}&msg=${encodeURIComponent(text)}`
    const resp = await fetch(url)
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buf = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      const blocks = buf.split('\n\n'); buf = blocks.pop()
      for (const b of blocks) {
        const line = b.trim()
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data === '[DONE]') { assistant.typing = false; continue }
          assistant.text += data
          scroll()
        }
      }
    }
    assistant.typing = false
  } catch (e) { assistant.text += '（连接中断）'; assistant.typing = false }
  streaming.value = false
}
async function doTriage() {
  const complaint = input.value.trim() || msgs.value.filter(m=>m.role==='user').map(m=>m.text).join('，')
  if (!complaint) { ElMessage.warning('请先描述症状'); return }
  sug.value = await api.post('/api/patient/triage', { sessionId: sessionId.value, complaint, patientId: patientId.value })
  suggested.value = true
}
async function doRegister() {
  reg.value = await api.post('/api/patient/register', { suggestionId: sug.value.suggestion_id, patientId: patientId.value })
  registered.value = true
  ElMessage.success('挂号成功')
}
</script>
