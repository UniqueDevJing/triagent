<template>
  <div class="triage-wrap">
    <!-- 主对话区 -->
    <div class="chat-col">
      <div class="triage-header">
        <div class="hd-left">
          <div class="logo" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><path d="M12 4v16M4 12h16" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/><circle cx="12" cy="12" r="4.2" stroke="currentColor" stroke-width="1.5"/></svg></div>
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
          <div class="empty-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><path d="M3 12h3l2-5 3 10 2.5-7 1.5 2h6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg></div>
          <p>描述你的不适或需求，例如：</p>
          <div class="quick-chips">
            <span v-for="q in quick" :key="q.text" class="chip" @click="send(q.text)">{{ q.label }}</span>
          </div>
          <p class="empty-tip">分诊咨询、体检报告解读、门诊预约，一个入口全部完成</p>
        </div>

        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <div class="avatar" :class="m.role">{{ m.role === 'user' ? '我' : 'AI' }}</div>
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
                <div class="sec-label">编排决策 · Supervisor</div>
                <div v-for="(p, j) in m.plans" :key="j" class="plan-row">
                  <span class="plan-chip">{{ planLabel(p.decision) }}</span>
                  <span class="plan-reason">{{ p.reason }}</span>
                </div>
              </div>

              <!-- 工具过程 -->
              <div v-if="m.tools && m.tools.length" class="tools">
                <div class="sec-label">工具调用过程</div>
                <div v-for="(t, j) in m.tools" :key="j" class="tool-row">
                  <span class="tool-name">{{ t.name }}</span>
                  <span v-if="t.args" class="tool-args">{{ t.args }}</span>
                  <span v-if="t.result" class="tool-result">{{ t.result }}</span>
                </div>
              </div>

              <!-- 两步确认第二步：待确认预订单卡片。
                   后端 createPreOrder 的 tool_result 现在结构化下发 orderId/status，
                   此前只有纯文本摘要，confirmPreOrder 接口一直是死代码、两步确认断在第二步。 -->
              <div v-if="m.pendingOrder" class="pending-order">
                <div class="sec-label">待确认预约 · 第二步</div>
                <div class="po-meta">
                  #{{ m.pendingOrder.orderId }} · {{ m.pendingOrder.department }} · {{ m.pendingOrder.date }}
                </div>
                <button
                  class="po-btn"
                  :disabled="m.pendingOrder.confirming || m.pendingOrder.confirmed"
                  @click="confirmOrder(m)"
                >
                  {{ m.pendingOrder.confirmed ? '已确认' : (m.pendingOrder.confirming ? '确认中…' : '确认预约') }}
                </button>
                <div v-if="m.pendingOrder.confirmed" class="po-done">预约已确认，请按时到院</div>
              </div>

              <!-- 澄清追问 + 快捷回复 -->
              <div v-if="m.clarify" class="clarify">
                <div class="sec-label">需要你补充</div>
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
                <div v-if="m.triage.disclaimer" class="tc-disclaimer">注意 · {{ m.triage.disclaimer }}</div>
              </div>

              <!-- 知识来源 -->
              <div v-if="m.sources && m.sources.length" class="sources">
                <el-collapse>
                  <el-collapse-item :title="'知识来源（' + m.sources.length + '）'">
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
import { chatStream, confirmPreOrder } from '@/api/modules/assistant'

const input = ref('')
const busy = ref(false)
const agentLabel = ref('')
const messages = ref([])

// 两步确认第二步：确认预订单。此前后端端点已存在、前端接口函数已定义，但从未被调用。
async function confirmOrder(m) {
  const po = m.pendingOrder
  if (!po || po.confirming || po.confirmed) return
  po.confirming = true
  try {
    await confirmPreOrder(po.orderId)
    po.confirmed = true
    po.confirming = false
    m.text += '\n\n预订单 #' + po.orderId + ' 已确认，请按时到院。'
  } catch (e) {
    po.confirming = false
  }
  scrollBottom()
}
const scrollRef = ref(null)
let sessionId = 'triage-' + Date.now().toString(36)

const quick = [
  { label: '最近不太舒服', text: '我最近不太舒服' },
  { label: '胸痛伴呼吸困难', text: '我胸痛还呼吸困难，持续半小时了' },
  { label: '头痛三天伴恶心', text: '左边头痛，持续3天，有点恶心，没有发烧' },
  { label: '解读体检报告', text: '解读会员孙明伟的体检报告，告诉我风险等级和后续建议' },
  { label: '预约门诊', text: '帮我给会员孙明伟预约呼吸内科的号' },
]

const fieldKeys = ['bodyPart', 'duration', 'accompany']
const fieldsProgress = ref({ bodyPart: 'wait', duration: 'wait', accompany: 'wait' })
let previousMissing = []

const fieldLabel = (f) => ({ bodyPart: '部位', duration: '时长', accompany: '伴随' }[f] || f)
const urgencyText = (u) =>
  ({ EMERGENCY: '需立即急诊', URGENT: '尽快就诊', ROUTINE: '普通门诊' }[u] || u || '')
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
        const apply = (text) => {
          if (t) t.result = text
          else bot.tools.push({ name: d.name, args: '', result: text })
        }
        // createPreOrder 的结果是结构化 JSON（orderId/status/department/date）
        if (d.name === 'createPreOrder') {
          try {
            const o = JSON.parse(d.result)
            if (o && o.orderId) {
              apply(o.summary || '已生成预订单')
              bot.pendingOrder = {
                orderId: o.orderId, status: o.status,
                department: o.department, date: o.date,
                confirming: false, confirmed: false,
              }
              scrollBottom()
              return
            }
          } catch (e) { /* 非结构化（如日期格式错误提示），按纯文本落回工具行 */ }
        }
        apply(d.result)
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
        bot.text = (bot.text ? bot.text + '\n' : '') + (d.message || '服务异常')
        bot.streaming = false
        bot.error = true
        scrollBottom()
      },
    })
  } catch (e) {
    bot.text = '连接失败：' + (e.message || e)
    bot.streaming = false
    bot.error = true
    scrollBottom()
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.triage-wrap { display: flex; gap: 16px; height: calc(100vh - 108px); min-height: 560px; }
.chat-col {
  flex: 1; min-width: 0; display: flex; flex-direction: column;
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius);
  box-shadow: var(--shadow-sm); overflow: hidden;
}

/* ---------- 头部 ---------- */
.triage-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--line); flex-shrink: 0; }
.hd-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.logo {
  width: 38px; height: 38px; border-radius: 10px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: var(--ink-900); color: #4FD1B5; font-size: 18px;
}
.logo svg { width: 20px; height: 20px; }
.triage-header h2 { margin: 0; font-size: 15px; font-weight: 650; color: var(--text-1); }
.hd-sub { margin: 3px 0 0; font-size: 11px; color: var(--text-4); letter-spacing: .01em; }
.hd-right { display: flex; align-items: center; flex-shrink: 0; }
.pulse { color: var(--brand); animation: pulse 1.2s infinite; margin-right: 4px; }
@keyframes pulse { 50% { opacity: .25; } }
.idle-tag { font-size: 11px; color: var(--text-3); background: var(--surface-alt); border: 1px solid var(--line); padding: 4px 10px; border-radius: 999px; }

/* ---------- 消息流 ---------- */
.msg-list { flex: 1; overflow-y: auto; background: var(--surface-alt); padding: 18px; display: flex; flex-direction: column; gap: 16px; scroll-behavior: smooth; }
.msg-list::-webkit-scrollbar { width: 6px; }
.empty { text-align: center; padding-top: 52px; color: var(--text-3); }
.empty-icon {
  display: inline-flex; width: 62px; height: 62px; align-items: center; justify-content: center;
  border-radius: 16px; font-size: 26px; margin-bottom: 18px;
  background: var(--brand-soft); color: var(--brand-strong); border: 1px solid #D3EDE7;
}
.empty-icon svg { width: 28px; height: 28px; }
.empty p { font-size: 13px; margin: 0 0 14px; }
.empty-tip { font-size: 11.5px !important; color: var(--text-4); margin-top: 18px !important; }
.quick-chips { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; max-width: 520px; margin: 0 auto; }
.chip {
  display: inline-flex; align-items: center; font-size: 12px; color: var(--text-2);
  background: #fff; border: 1px solid var(--line); border-radius: 999px;
  padding: 7px 14px; cursor: pointer; transition: all .18s ease;
}
.chip:hover { border-color: var(--brand); color: var(--brand-strong); background: var(--brand-soft); }
.chip.small { font-size: 11.5px; padding: 5px 12px; }
.chip.disabled { opacity: .5; cursor: not-allowed; }

.msg { display: flex; gap: 10px; align-items: flex-start; }
.msg.user { flex-direction: row-reverse; }
.avatar { width: 30px; height: 30px; border-radius: 9px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; }
.avatar.assistant { background: var(--ink-900); color: #4FD1B5; }
.avatar.user { background: #E4EDF3; color: var(--ink-700); }
.bubble {
  max-width: min(78%, 760px); background: #fff; border: 1px solid var(--line);
  border-radius: 4px 12px 12px 12px; padding: 12px 15px;
  font-size: 13px; line-height: 1.75; color: var(--text-1); box-shadow: var(--shadow-sm);
}
.msg.user .bubble { background: var(--brand); border-color: var(--brand); color: #fff; border-radius: 12px 4px 12px 12px; }
.bubble.error { border-color: #F3C5C1; background: #FEF6F5; }
.bubble.error .answer { color: #B8403A; }
.m-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid var(--line-soft); }
.m-agent { font-size: 11px; font-weight: 650; color: var(--brand-strong); letter-spacing: .02em; }
.m-urgency { font-size: 10.5px; font-weight: 650; padding: 1px 7px; border-radius: 4px; background: var(--surface-alt); color: var(--text-3); }
.m-urgency.urgent { color: #B8781A; background: #FDF4E6; }
.m-urgency.emergency { color: #C2453C; background: #FDEDEB; }
.answer { word-break: break-word; }
.answer p { margin: 0 0 8px; }
.answer p:last-child { margin-bottom: 0; }
.answer ul, .answer ol { margin: 6px 0 8px 18px; }
.answer code { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11.5px; background: var(--surface-alt); padding: 1px 5px; border-radius: 4px; }
.answer strong { font-weight: 650; }
.caret { color: var(--brand); animation: pulse 1s infinite; }
.sec-label { font-size: 10.5px; font-weight: 700; letter-spacing: .08em; color: var(--text-4); text-transform: uppercase; margin-bottom: 8px; }

/* ---------- 过程可视化卡片 ---------- */
.plan-block, .tools, .pending-order, .clarify, .sources { margin-top: 12px; padding: 11px 13px; background: var(--surface-alt); border: 1px solid var(--line-soft); border-radius: 9px; }
.plan-row, .tool-row { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; font-size: 11.5px; padding: 4px 0; border-bottom: 1px dashed var(--line); }
.plan-row:last-child, .tool-row:last-child { border-bottom: none; }
.plan-chip { flex-shrink: 0; font-size: 10.5px; font-weight: 650; color: var(--brand-strong); background: var(--brand-soft); border-radius: 4px; padding: 2px 7px; }
.plan-reason { color: var(--text-2); }
.tool-name { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11px; font-weight: 650; color: var(--ink-700); background: #EAF0F4; padding: 2px 6px; border-radius: 4px; }
.tool-args { color: var(--text-3); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10.5px; }
.tool-result { color: var(--success); }
.pending-order { background: #F3FAF8; border-color: #CFEBE4; }
.po-meta { font-size: 12px; color: var(--text-2); margin-bottom: 10px; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
.po-btn { border: none; background: var(--brand); color: #fff; font-size: 12px; font-weight: 600; padding: 7px 14px; border-radius: 7px; cursor: pointer; }
.po-btn:hover:not(:disabled) { background: var(--brand-strong); }
.po-btn:disabled { opacity: .6; cursor: not-allowed; }
.po-done { font-size: 11.5px; color: var(--success); margin-top: 8px; }
.clarify { background: #FDF9F1; border-color: #F1E4CB; }
.clarify-q { font-size: 12.5px; color: var(--text-1); margin-bottom: 10px; font-weight: 550; }
.clarify-chips { display: flex; flex-wrap: wrap; gap: 7px; }
.sources { padding: 4px 12px; }
.sources :deep(.el-collapse-item__header) { font-size: 11.5px; background: transparent; border: none; height: 36px; color: var(--text-3); }
.sources :deep(.el-collapse-item__wrap) { background: transparent; border: none; }
.src-row { font-size: 11.5px; color: var(--text-2); padding: 4px 0; border-top: 1px dashed var(--line); }
.score { color: var(--text-4); font-variant-numeric: tabular-nums; margin-left: 6px; }

/* ---------- 分诊结论 ---------- */
.triage-card { margin-top: 12px; padding: 13px 15px; border: 1px solid var(--line); border-left: 3px solid var(--brand); border-radius: 4px 9px 9px 4px; background: var(--surface-alt); }
.triage-card.emergency { border-left-color: var(--danger); background: #FDF4F3; }
.triage-card.urgent { border-left-color: var(--warning); background: #FDF9F1; }
.triage-card.routine { border-left-color: var(--success); background: #F2FAF6; }
.tc-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
.urgency { font-weight: 700; font-size: 12px; color: var(--brand-strong); }
.urgency.emergency { color: #C2453C; }
.urgency.urgent { color: #B8781A; }
.urgency.routine { color: #128A5E; }
.tc-row { margin-top: 6px; font-size: 12px; color: var(--text-2); }
.tc-row b { color: var(--text-1); font-weight: 650; }
.tc-row ul { margin: 4px 0 0 18px; }
.tc-disclaimer { margin-top: 10px; padding-top: 9px; border-top: 1px dashed var(--line); font-size: 11px; color: #A2605A; }

/* ---------- 输入区 ---------- */
.input-bar { display: flex; gap: 10px; align-items: flex-end; padding: 12px 16px; border-top: 1px solid var(--line); background: #fff; flex-shrink: 0; }
.input-bar :deep(.el-textarea__inner) { border-radius: 9px; background: var(--surface-alt); box-shadow: 0 0 0 1px var(--line) inset; padding: 10px 12px; font-size: 13px; }
.input-bar :deep(.el-textarea__inner:focus) { background: #fff; box-shadow: 0 0 0 1.5px var(--brand) inset; }
.send-col { flex-shrink: 0; }
.send-col .el-button { height: 54px; padding: 0 22px; border-radius: 9px; font-weight: 650; }

/* ---------- 右侧上下文 ---------- */
.info-col { width: 258px; flex-shrink: 0; }
.panel { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); padding: 18px; box-shadow: var(--shadow-sm); height: 100%; overflow-y: auto; }
.panel-title { font-size: 13px; font-weight: 650; color: var(--text-1); padding-bottom: 12px; border-bottom: 1px solid var(--line-soft); margin-bottom: 14px; }
.kv { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.k { font-size: 10.5px; font-weight: 650; letter-spacing: .06em; color: var(--text-4); text-transform: uppercase; }
.v { font-size: 12.5px; color: var(--text-1); font-weight: 600; }
.v.muted { color: var(--text-4); font-weight: 400; }
.chips-inline { display: flex; flex-wrap: wrap; gap: 5px; }
.panel-sub { font-size: 10.5px; font-weight: 650; letter-spacing: .06em; color: var(--text-4); text-transform: uppercase; margin-bottom: 10px; }
.rail-links { display: flex; flex-direction: column; gap: 6px; }
.rail-link { font-size: 12px; color: var(--text-2); padding: 8px 10px; border: 1px solid var(--line); border-radius: 7px; cursor: pointer; transition: all .18s ease; }
.rail-link:hover { border-color: var(--brand); color: var(--brand-strong); background: var(--brand-soft); }
.rail-link.off { opacity: .5; cursor: not-allowed; }
.rail-tip { font-size: 11px; line-height: 1.7; color: var(--text-3); background: var(--surface-alt); border-radius: 8px; padding: 10px 12px; }
.rail-tip p { margin: 0 0 6px; }
.rail-tip p:last-child { margin-bottom: 0; }
.clear-btn { width: 100%; margin-top: 14px; }

@media (max-width: 1080px) { .info-col { display: none; } }
@media (max-width: 768px) {
  .triage-wrap { height: calc(100vh - 92px); min-height: 480px; }
  .msg-list { padding: 12px; }
  .bubble { max-width: 88%; }
  .triage-header { padding: 12px; }
}
</style>
