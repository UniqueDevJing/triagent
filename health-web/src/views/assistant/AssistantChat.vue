<template>
  <div class="triage-wrap">
    <!-- 主对话区 -->
    <div class="chat-col">
      <div class="triage-header">
        <div class="hd-left">
          <div class="logo">⚕️</div>
          <div>
            <h2>智能分诊 · 就医助手</h2>
            <p class="hd-sub">多 Agent 协作 · 意图连贯 · 透明推理 · 知识溯源</p>
          </div>
        </div>
        <div class="hd-right">
          <el-tag v-if="agentLabel" size="small" round effect="light" :type="agentTagType(agentLabel)">
            <span v-if="busy" class="pulse">●</span> {{ agentLabel }}
          </el-tag>
          <span v-else class="idle-tag">等待输入</span>
        </div>
      </div>

      <!-- 对话流 -->
      <div ref="scrollRef" class="msg-list">
        <div v-if="messages.length === 0" class="empty">
          <div class="empty-icon">🩺</div>
          <p>描述你的不适或需求，例如：</p>
          <div class="quick-chips">
            <span v-for="q in quick" :key="q.text" class="chip" @click="send(q.text)">{{ q.label }}</span>
          </div>
          <p class="empty-tip">分诊咨询、体检报告解读、门诊预约，一个入口全部完成</p>
        </div>

        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <div class="avatar" :class="m.role">{{ m.role === 'user' ? '🧑' : '🤖' }}</div>
          <div class="bubble" :class="{ error: m.error }">
            <template v-if="m.role === 'user'">{{ m.text }}</template>
            <template v-else>
              <div class="m-meta" v-if="m.agent || m.streaming">
                <span class="m-agent">{{ m.agent || '正在思考…' }}</span>
                <span v-if="m.triage" class="m-urgency" :class="(m.triage.urgency || '').toLowerCase()">
                  {{ urgencyText(m.triage.urgency) }}
                </span>
              </div>

              <div class="answer" v-html="renderMd(m.text)"></div>

              <!-- Supervisor 编排决策（可视化步骤） -->
              <div v-if="m.plans && m.plans.length" class="plan-block">
                <div class="sec-label">🧭 编排决策（Supervisor）</div>
                <div v-for="(p, j) in m.plans" :key="j" class="plan-row">
                  <span class="plan-chip">{{ planLabel(p.decision) }}</span>
                  <span class="plan-reason">{{ p.reason }}</span>
                </div>
              </div>

              <!-- 工具过程 -->
              <div v-if="m.tools && m.tools.length" class="tools">
                <div class="sec-label">🔧 工具过程</div>
                <div v-for="(t, j) in m.tools" :key="j" class="tool-row">
                  <span class="tool-name">{{ t.name }}</span>
                  <span v-if="t.args" class="tool-args">{{ t.args }}</span>
                  <span v-if="t.result" class="tool-result">{{ t.result }}</span>
                </div>
              </div>

              <!-- 澄清追问 + 快捷回复 -->
              <div v-if="m.clarify" class="clarify">
                <div class="sec-label">✍️ 需要你补充</div>
                <div class="clarify-q">{{ m.clarify.question }}</div>
                <div class="clarify-chips">
                  <span
                    v-for="opt in quickAnswers(m.clarify.missing)"
                    :key="opt"
                    class="chip small"
                    :class="{ disabled: busy }"
                    @click="!busy && send(opt)"
                  >{{ opt }}</span>
                </div>
              </div>

              <!-- 分诊结果卡 -->
              <div v-if="m.triage" class="triage-card" :class="m.triage.urgency.toLowerCase()">
                <div class="tc-head">
                  <span class="urgency" :class="m.triage.urgency.toLowerCase()">{{ urgencyText(m.triage.urgency) }}</span>
                  <el-tag v-for="d in m.triage.departments" :key="d" size="small" effect="plain">{{ d }}</el-tag>
                </div>
                <div v-if="m.triage.hospitalLevel" class="tc-row"><b>建议就医：</b>{{ m.triage.hospitalLevel }}</div>
                <div v-if="m.triage.followUp && m.triage.followUp.length" class="tc-row">
                  <b>下一步：</b>
                  <ul><li v-for="(f, k) in m.triage.followUp" :key="k">{{ f }}</li></ul>
                </div>
                <div v-if="m.triage.confidence != null" class="tc-row"><b>置信度：</b>{{ Math.round(m.triage.confidence * 100) }}%</div>
                <div v-if="m.triage.disclaimer" class="tc-disclaimer">⚠️ {{ m.triage.disclaimer }}</div>
              </div>

              <!-- 知识来源 -->
              <div v-if="m.sources && m.sources.length" class="sources">
                <el-collapse>
                  <el-collapse-item :title="'📚 知识来源（' + m.sources.length + '）'">
                    <div v-for="(s, k) in m.sources" :key="k" class="src-row">
                      [{{ s.source }} #{{ s.refId }}] {{ s.title }}
                      <span class="score">{{ s.score }}</span>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>

              <span v-if="m.streaming" class="caret">▌</span>
            </template>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-bar">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="输入你的症状 / 需求，例如：帮孙明伟约呼吸内科…"
          :disabled="busy"
          @keydown.enter.exact.prevent="send()"
        />
        <div class="send-col">
          <el-button type="primary" :loading="busy" @click="send()">发送</el-button>
        </div>
      </div>
    </div>

    <!-- 会话信息栏 -->
    <aside class="info-col">
      <div class="panel">
        <div class="panel-title">会话上下文</div>
        <div class="kv">
          <span class="k">当前 Agent</span>
          <span class="v" :class="{ muted: !agentLabel }">{{ agentLabel || '—' }}</span>
        </div>
        <div class="kv">
          <span class="k">澄清进度</span>
          <span class="v chips-inline">
            <el-tag v-for="f in fieldKeys" :key="f" size="small" :type="fieldsProgress[f] === 'done' ? 'success' : 'info'" effect="light" round>
              {{ f === 'done' ? '✓' : '·' }} {{ fieldLabel(f) }}
            </el-tag>
          </span>
        </div>
        <el-divider />
        <div class="panel-sub">快捷开始</div>
        <div class="rail-links">
          <span v-for="q in quick" :key="q.text" class="rail-link" :class="{ off: busy }" @click="!busy && send(q.text)">{{ q.label }}</span>
        </div>
        <el-divider />
        <div class="rail-tip">
          <p>· 同一会话内意图保持连贯：预约管家收主诉时，描述症状不会被误判为急诊求助。</p>
          <p>· 说「取消 / 算了」可退出当前流程；说「预约 / 解读报告」会切换 Agent。</p>
        </div>
        <el-button class="clear-btn" size="small" plain @click="resetSession">清空会话</el-button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { chatStream } from '@/api/modules/assistant'

const input = ref('')
const busy = ref(false)
const agentLabel = ref('')
const messages = ref([])
const scrollRef = ref(null)
let sessionId = 'triage-' + Date.now().toString(36)

const quick = [
  { label: '🤔 最近不太舒服', text: '我最近不太舒服' },
  { label: '🫀 胸痛伴呼吸困难', text: '我胸痛还呼吸困难，持续半小时了' },
  { label: '🤕 头痛三天伴恶心', text: '左边头痛，持续3天，有点恶心，没有发烧' },
  { label: '📑 解读体检报告', text: '解读会员孙明伟的体检报告，告诉我风险等级和后续建议' },
  { label: '📅 预约门诊', text: '帮我给会员孙明伟预约呼吸内科的号' },
]

const fieldKeys = ['bodyPart', 'duration', 'accompany']
const fieldsProgress = ref({ bodyPart: 'wait', duration: 'wait', accompany: 'wait' })
let previousMissing = []

const fieldLabel = (f) => ({ bodyPart: '部位', duration: '时长', accompany: '伴随' }[f] || f)
const urgencyText = (u) =>
  ({ EMERGENCY: '🚨 需立即急诊', URGENT: '⚠️ 尽快就诊', ROUTINE: '💚 普通门诊' }[u] || u || '')
const agentTagType = (name) =>
  (name || '').includes('预约') ? 'success' : (name || '').includes('解读') || (name || '').includes('报告') ? 'warning' : 'primary'

const planLabel = (decision) =>
  ({ TRIAGE_FIRST: '应急分诊优先', SAFETY_BLOCK: '安全拦截预约', RESUME: '恢复预约' }[decision] || decision)

const answerMap = {
  bodyPart: ['头痛', '胸痛', '腹痛', '关节痛', '咳嗽'],
  duration: ['2天', '一周', '一个月', '反复好几年'],
  accompany: ['有发热', '有点恶心', '没有其他症状'],
  default: ['持续3天', '有点恶心，没有发烧'],
}
function quickAnswers(missing) {
  if (!missing || !missing.length) return []
  const set = new Set()
  for (const f of missing) (answerMap[f] || answerMap.default).forEach((x) => set.add(x))
  return Array.from(set)
}

function renderMd(t) {
  if (!t) return ''
  return String(t)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/\n/g, '<br/>')
}

function scrollBottom() {
  nextTick(() => {
    if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  })
}

function resetSession() {
  sessionId = 'triage-' + Date.now().toString(36)
  messages.value = []
  agentLabel.value = ''
  fieldsProgress.value = { bodyPart: 'wait', duration: 'wait', accompany: 'wait' }
  previousMissing = []
}

async function send(text) {
  const content = (text ?? input.value).trim()
  if (!content || busy.value) return
  input.value = ''
  busy.value = true

  messages.value.push({ role: 'user', text: content })
  const bot = { role: 'assistant', text: '', tools: [], plans: [], streaming: true, error: false }
  messages.value.push(bot)
  scrollBottom()

  try {
    await chatStream(sessionId, content, {
      token(d) {
        bot.text += d
        scrollBottom()
      },
      tool_call(d) {
        bot.tools.push({ name: d.name, args: d.arguments, result: '' })
        scrollBottom()
      },
      plan(d) {
        bot.plans.push(d)
        scrollBottom()
      },
      tool_result(d) {
        const t = bot.tools.find((x) => x.name === d.name && !x.result)
        if (t) t.result = d.result
        else bot.tools.push({ name: d.name, args: '', result: d.result })
        scrollBottom()
      },
      clarify(d) {
        bot.clarify = d
        if (!bot.text) bot.text = ''
        agentLabel.value = '分诊顾问'
        bot.agent = '分诊顾问 · 追问'
        // 进度推进：上轮缺失但本轮不再追问 → 视为已采集
        const current = d.missing || []
        for (const f of previousMissing) {
          if (!current.includes(f) && fieldsProgress.value[f] === 'wait') fieldsProgress.value[f] = 'done'
        }
        previousMissing = current
        scrollBottom()
      },
      done(d) {
        const dupClarify = bot.clarify && d.answer === bot.clarify.question
        if (d.answer && d.answer !== bot.text && !dupClarify) bot.text = d.answer
        bot.triage = d.triage || null
        bot.sources = d.sources || []
        agentLabel.value = d.agent || agentLabel.value
        bot.agent = d.agent || bot.agent
        bot.streaming = false
        // 无进一步追问 → 补齐字段进度展示为已采集（结合状态机语义的近似）
        if (!bot.clarify && previousMissing.length) {
          for (const f of previousMissing) fieldsProgress.value[f] = 'done'
          previousMissing = []
        }
        scrollBottom()
      },
      error(d) {
        bot.text = (bot.text ? bot.text + '\n' : '') + '⚠️ ' + (d.message || '服务异常')
        bot.streaming = false
        bot.error = true
        scrollBottom()
      },
    })
  } catch (e) {
    bot.text = '⚠️ 连接失败：' + (e.message || e)
    bot.streaming = false
    bot.error = true
    scrollBottom()
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.triage-wrap {
  display: flex;
  gap: 14px;
  height: calc(100vh - 60px);
  padding: 14px 18px;
  box-sizing: border-box;
}
.chat-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background:
    radial-gradient(1100px 380px at 88% -8%, rgba(91, 141, 239, 0.10), transparent 60%),
    radial-gradient(800px 320px at -8% 108%, rgba(122, 91, 216, 0.08), transparent 55%),
    #ffffff;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 16px;
  padding: 14px 16px;
}
.triage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.hd-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.logo {
  width: 46px; height: 46px; border-radius: 14px; font-size: 22px;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #5b8def, #7a5bd8); color: #fff;
  box-shadow: 0 6px 16px rgba(91, 141, 239, 0.3);
}
.triage-header h2 { margin: 0; font-size: 18px; color: #2c3e50; }
.hd-sub { margin: 2px 0 0; font-size: 12px; color: #8a94a6; }
.hd-right { display: flex; align-items: center; }
.pulse { animation: pulse 1s infinite; margin-right: 4px; }
@keyframes pulse { 50% { opacity: 0.2; } }
.idle-tag { font-size: 12px; color: #a8b0bd; background: #f3f5f9; padding: 4px 12px; border-radius: 999px; }

.msg-list {
  flex: 1;
  overflow-y: auto;
  background: #f5f7fb;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  scroll-behavior: smooth;
}
.msg-list::-webkit-scrollbar { width: 6px; }

.empty { text-align: center; padding-top: 44px; color: #7d8aa0; }
.empty-icon {
  display: inline-flex; width: 76px; height: 76px;
  align-items: center; justify-content: center;
  border-radius: 24px; font-size: 36px;
  background: linear-gradient(135deg, rgba(91,141,239,0.12), rgba(122,91,216,0.12));
  animation: floaty 3s ease-in-out infinite;
}
@keyframes floaty { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-6px); } }
.empty-tip { font-size: 12px; margin-top: 10px; opacity: 0.7; }

.quick-chips { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 10px; }
.chip {
  padding: 5px 12px; border-radius: 999px; cursor: pointer; font-size: 13px;
  background: #fff; border: 1px solid #dbe3f2; color: #3d5a99; transition: all 0.2s ease;
  user-select: none;
}
.chip:hover { border-color: #5b8def; color: #5b8def; transform: translateY(-1px); }
.chip.small { font-size: 12px; padding: 3px 10px; }
.chip.disabled { opacity: 0.6; cursor: not-allowed; }

.msg { display: flex; gap: 10px; align-items: flex-start; animation: bubbleIn 0.28s ease both; }
@keyframes bubbleIn {
  from { opacity: 0; transform: translateY(6px) scale(0.99); }
  to { opacity: 1; transform: none; }
}
.avatar {
  flex: 0 0 34px; width: 34px; height: 34px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 16px; background: #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
}
.avatar.user { background: linear-gradient(135deg, #e8efff, #efe9ff); }
.bubble {
  max-width: 78%;
  min-width: 0;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}
.msg.user { flex-direction: row-reverse; }
.msg.user .bubble {
  background: linear-gradient(135deg, #5b8def, #7a5bd8); color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 6px 16px rgba(91, 141, 239, 0.25);
}
.msg.assistant .bubble {
  background: #fff; border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.bubble.error .answer { color: #c45656; }

.m-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; font-size: 11px; }
.m-agent { color: #7d8aa0; background: #f0f4fc; padding: 2px 8px; border-radius: 999px; }
.m-urgency { font-weight: 600; }
.m-urgency.emergency { color: #e0533d; }
.m-urgency.urgent { color: #d98c1b; }
.m-urgency.routine { color: #4fae2f; }
.caret { color: #5b8def; animation: blink 1s infinite; margin-left: 2px; }
@keyframes blink { 50% { opacity: 0; } }

.sec-label { font-size: 11px; color: #98a2b3; margin: 10px 0 6px; letter-spacing: 0.5px; }
.plan-block { display: flex; flex-direction: column; gap: 6px; }
.plan-row {
  display: flex; align-items: center; gap: 8px; font-size: 12px;
  background: #f6f3ff; border: 1px solid #e9e0fb; border-left: 3px solid #7a5bd8;
  border-radius: 8px; padding: 6px 10px;
}
.plan-chip {
  flex: 0 0 auto;
  color: #6a42c8; background: #efe7ff; font-weight: 600;
  padding: 2px 8px; border-radius: 999px; font-size: 11px;
}
.plan-reason { color: #6b5a8f; }
.tools { display: flex; flex-direction: column; gap: 6px; }
.tool-row {
  display: flex; flex-direction: column; gap: 2px; font-size: 12px;
  background: #f4f7fd; border: 1px solid #e4eaf6; border-radius: 8px; padding: 6px 10px;
  transition: background 0.2s ease;
}
.tool-row:hover { background: #edf3fd; }
.tool-name { color: #4a6bcf; font-weight: 600; }
.tool-args { color: #6b7688; }
.tool-result { color: #2f9e63; }

.clarify { margin-top: 4px; }
.clarify-q { font-weight: 500; }
.clarify-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }

.triage-card {
  margin-top: 10px; border-radius: 12px; padding: 10px 12px;
  border-left: 4px solid #999; background: #fafbfd; font-size: 13px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.triage-card.emergency { border-left-color: #e0533d; background: #fdf1ef; }
.triage-card.urgent { border-left-color: #e6a23c; background: #fdf7ec; }
.triage-card.routine { border-left-color: #67c23a; background: #f2faf0; }
.tc-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.urgency { font-weight: 700; }
.urgency.emergency { color: #e0533d; }
.urgency.urgent { color: #d98c1b; }
.urgency.routine { color: #4fae2f; }
.tc-row { margin-top: 6px; }
.tc-row ul { margin: 4px 0 0 18px; padding: 0; }
.tc-disclaimer { margin-top: 8px; font-size: 12px; color: #a97474; }

.sources { margin-top: 8px; }
.src-row { font-size: 12px; padding: 2px 0; }
.score { color: #999; margin-left: 6px; }

.input-bar { display: flex; gap: 10px; align-items: flex-end; }
.send-col { flex: 0 0 auto; }
.input-bar :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: box-shadow 0.25s ease, background 0.25s ease;
}
.input-bar :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #5b8def inset, 0 2px 10px rgba(91,141,239,0.12);
}

/* 右侧信息栏 */
.info-col { flex: 0 0 292px; display: flex; }
.panel {
  width: 100%;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  font-size: 13px;
}
.panel-title { font-weight: 700; color: #2c3e50; display: flex; align-items: center; gap: 6px; }
.panel-title::before { content: ''; width: 4px; height: 14px; border-radius: 2px; background: linear-gradient(180deg, #5b8def, #7a5bd8); }
.kv { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.k { color: #8a94a6; }
.v { color: #33415c; font-weight: 500; text-align: right; }
.v.muted { color: #a8b0bd; font-weight: 400; }
.chips-inline { display: flex; flex-wrap: wrap; gap: 4px; justify-content: flex-end; }
.panel-sub { font-size: 12px; color: #8a94a6; }
.rail-links { display: flex; flex-direction: column; gap: 6px; }
.rail-link {
  font-size: 13px; color: #3d5a99; background: #f4f7fd;
  border-radius: 8px; padding: 7px 10px; cursor: pointer;
  border: 1px solid transparent; transition: all 0.2s ease; user-select: none;
}
.rail-link:hover { border-color: #5b8def; background: #eef4ff; }
.rail-link.off { opacity: 0.6; cursor: not-allowed; }
.rail-tip p { margin: 0 0 6px; color: #7d8aa0; font-size: 12px; line-height: 1.6; }
.clear-btn { margin-top: auto; align-self: flex-start; }

@media (max-width: 1080px) {
  .info-col { display: none; }
  .bubble { max-width: 88%; }
}
</style>
