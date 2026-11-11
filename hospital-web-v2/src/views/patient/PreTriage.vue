<template>
  <main class="workspace patient-journey">
    <section class="journey-head">
      <div><span class="eyebrow">GUIDED PRE-TRIAGE</span><h2>先说说哪里不舒服。</h2><p>TriAgent 会进行必要追问，并结合你的健康档案给出安全、可解释的就诊建议。</p></div>
      <div class="journey-steps">
        <span class="done"><i>1</i>健康档案</span><b></b><span :class="{ active: sessionId }"><i>2</i>症状沟通</span><b></b><span :class="{ active: suggested }"><i>3</i>就诊建议</span>
      </div>
    </section>

    <section class="consultation-shell">
      <article class="conversation-panel">
        <header class="conversation-head">
          <div class="agent-identity"><span class="agent-symbol">T</span><div><b>TriAgent 预问诊助手</b><small><i></i>{{ sessionId ? `会话 #${sessionId} 已连接` : '等待开始会话' }}</small></div></div>
          <button class="secondary-button" @click="start">{{ sessionId ? '重新开始' : '开始预问诊' }}</button>
        </header>

        <div ref="box" class="conversation-stream">
          <div v-if="!msgs.length" class="conversation-welcome">
            <span class="welcome-mark">+</span><h3>我会用几轮对话了解你的情况</h3><p>建议描述症状出现的时间、部位、程度，以及是否伴随发热、胸闷等情况。</p>
            <button v-if="!sessionId" class="primary-button" @click="start">开始安全预问诊</button>
          </div>
          <div v-for="(m, i) in msgs" :key="i" :class="['message-row', m.role]">
            <span v-if="m.role === 'bot'" class="message-avatar">T</span>
            <div class="message-content"><small>{{ m.role === 'bot' ? 'TriAgent' : '你' }}</small><p>{{ m.text }}<i v-if="m.typing" class="typing-caret"></i></p></div>
          </div>
        </div>

        <footer class="composer">
          <textarea v-model="input" rows="2" :disabled="!sessionId || streaming" placeholder="例如：从昨晚开始持续胸闷，活动后更明显……" @keydown.enter.exact.prevent="send"></textarea>
          <div><span>{{ streaming ? '正在生成回复…' : 'Enter 发送 · 信息仅用于本次分诊' }}</span><div><button class="secondary-button" :disabled="!sessionId || streaming || suggested" @click="doTriage">生成建议</button><button class="send-button" :disabled="!sessionId || streaming || !input.trim()" @click="send">发送 ↑</button></div></div>
        </footer>
      </article>

      <aside class="journey-side">
        <article v-if="suggested" :class="['result-ticket', { urgent: sug.red_flag }]">
          <header><span class="eyebrow">TRIAGE RESULT</span><span class="confidence">{{ Math.round((sug.confidence || 0) * 100) }}% 置信度</span></header>
          <div v-if="sug.red_flag" class="urgent-notice">检测到危急征象，已切换急诊绿色通道</div>
          <span class="result-label">推荐就诊</span><h3>{{ sug.department_name }}</h3><p>{{ sug.doctor_name }} · {{ sug.suggested_date }}</p>
          <div class="result-reason"><b>为什么这样推荐</b><p>{{ sug.reason }}</p></div>
          <button class="primary-button primary-button--wide" :disabled="registered" @click="doRegister">{{ registered ? '预约已完成' : '确认并预约挂号' }}</button>
          <small v-if="registered" class="booking-proof">预约凭证 · {{ reg.work_date }} {{ reg.period }}</small>
        </article>

        <article v-else class="safety-card">
          <span class="shield-mark">✦</span><div><b>三重安全校验</b><p>红旗征象识别、工具数据核验、规则引擎复核，在生成建议前依次执行。</p></div>
          <ul><li>危急情况自动转急诊</li><li>结合慢病与过敏信息</li><li>建议过程全链路可追踪</li></ul>
        </article>

        <details class="profile-card">
          <summary><div><span class="profile-avatar">{{ (profile.name || '患').slice(0, 1) }}</span><div><b>{{ profile.name || '完善健康档案' }}</b><small>{{ profile.age ? `${profile.gender} · ${profile.age} 岁` : '信息将参与安全校验' }}</small></div></div><span>编辑</span></summary>
          <div class="profile-form">
            <label>姓名<el-input v-model="profile.name" /></label>
            <div class="profile-pair"><label>性别<el-radio-group v-model="profile.gender"><el-radio-button value="男">男</el-radio-button><el-radio-button value="女">女</el-radio-button></el-radio-group></label><label>年龄<el-input-number v-model="profile.age" :min="0" :max="120" /></label></div>
            <label>慢病史<el-input v-model="profile.past_history" type="textarea" :rows="2" placeholder="如：高血压 8 年" /></label>
            <label>过敏史<el-input v-model="profile.allergic_history" type="textarea" :rows="2" placeholder="如：青霉素过敏" /></label>
            <button class="primary-button primary-button--wide" :disabled="saving" @click="saveProfile">{{ saving ? '保存中…' : '保存健康档案' }}</button>
          </div>
        </details>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { ref, nextTick, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

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
  const p = await api.get('/patient/profile')
  if (p) Object.assign(profile, {
    name: p.name || '', gender: p.gender || '男', age: p.age,
    past_history: p.past_history || '', allergic_history: p.allergic_history || ''
  })
}
async function saveProfile() {
  saving.value = true
  try {
    await api.post('/patient/profile/save', null, profile)
    ElMessage.success('档案已保存')
  } finally { saving.value = false }
}
onMounted(loadProfile)

function scroll() { nextTick(() => { if (box.value) box.value.scrollTop = box.value.scrollHeight }) }
async function start() {
  const r = await api.post('/patient/session')
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
    const resp = await fetch(url, { headers: { Authorization: `Bearer ${localStorage.getItem('hospital_token')}` } })
    if (!resp.ok) throw new Error(`SSE ${resp.status}`)
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
  sug.value = await api.post('/patient/triage', { sessionId: sessionId.value, complaint })
  suggested.value = true
}
async function doRegister() {
  reg.value = await api.post('/patient/register', { suggestionId: sug.value.suggestion_id })
  registered.value = true
  ElMessage.success('挂号成功')
}
</script>
