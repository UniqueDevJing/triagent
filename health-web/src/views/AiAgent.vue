<template>
  <div class="ai-agent-page">
    <div class="page-header">
      <h2 class="page-title">AI 健康助手</h2>
      <p class="page-subtitle">智能健康分析、用药提醒、情感陪伴与行为识别</p>
    </div>
    <el-row :gutter="20" class="content-row">
      <!-- 功能卡片区 -->
      <el-col :span="8">
        <el-card>
          <template #header><span>AI 功能</span></template>
          <div class="feature-cards">
            <div
              v-for="feature in features"
              :key="feature.type"
              class="feature-card"
              :class="{ active: currentFeature === feature.type }"
              @click="switchFeature(feature.type)"
            >
              <el-icon :size="24" :color="feature.color"><component :is="feature.icon" /></el-icon>
              <div class="feature-info">
                <div class="feature-name">{{ feature.name }}</div>
                <div class="feature-desc">{{ feature.desc }}</div>
              </div>
            </div>
          </div>

          <!-- 快捷示例 -->
          <div v-if="currentFeature === 'ANALYSIS'" class="quick-examples">
            <el-divider>示例数据</el-divider>
            <el-button size="small" @click="sendQuickMsg('血压150/95，空腹血糖6.3，总胆固醇5.8，帮我分析一下')">
              高血压+血糖分析
            </el-button>
            <el-button size="small" @click="sendQuickMsg('体检报告显示尿酸偏高480，低密度脂蛋白3.8')">
              血脂异常分析
            </el-button>
          </div>

          <div v-if="currentFeature === 'MEDICATION'" class="quick-examples">
            <el-divider>常用药品</el-divider>
            <el-button size="small" @click="sendQuickMsg('硝苯地平缓释片30mg，早晚各一次')">
              硝苯地平-降压药
            </el-button>
            <el-button size="small" @click="sendQuickMsg('二甲双胍0.5g，随餐服用，一天三次')">
              二甲双胍-降糖药
            </el-button>
          </div>

          <div v-if="currentFeature === 'BEHAVIOR'" class="quick-examples">
            <el-divider>行为描述示例</el-divider>
            <el-button size="small" @click="sendQuickMsg('老人今天在卫生间滑倒了，说头晕站不稳')">
              跌倒事件
            </el-button>
            <el-button size="small" @click="sendQuickMsg('最近总是忘记关煤气，有时候找不到回家的路')">
              记忆力下降
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 对话区 -->
      <el-col :span="16">
        <el-card class="chat-card">
          <template #header>
            <div class="chat-header">
              <span>AI 健康助手 · {{ featureLabel }}</span>
              <el-button size="small" @click="clearChat">清空对话</el-button>
            </div>
          </template>

          <div class="chat-messages" ref="chatContainer">
            <div v-if="messages.length === 0" class="welcome-message">
              <el-icon :size="48" color="#dcdfe6"><ChatDotRound /></el-icon>
              <h3>👋 您好，我是传智健康 AI 助手</h3>
              <p>选择左侧功能，我可以帮您：</p>
              <ul>
                <li>📊 分析健康数据，评估潜在风险</li>
                <li>💊 生成个性化用药提醒计划</li>
                <li>💝 提供温暖的情感陪伴对话</li>
                <li>⚠️ 识别老年人异常行为风险</li>
              </ul>
            </div>

            <div
              v-for="(msg, idx) in messages"
              :key="idx"
              class="message-item"
              :class="msg.role === 'user' ? 'user-msg' : 'ai-msg'"
            >
              <div class="msg-avatar">
                <el-avatar :size="32" :icon="msg.role === 'user' ? UserFilled : Service" />
              </div>
              <div class="msg-bubble">
                <div class="msg-content">{{ msg.content }}</div>

                <!-- 健康分析结构化展示 -->
                <div v-if="msg.structured && msg.featureType === 'ANALYSIS'" class="structured-result">
                  <el-alert
                    :title="'风险等级: ' + msg.structured.risk_level"
                    :type="msg.structured.risk_level === '高' ? 'error' : msg.structured.risk_level === '中' ? 'warning' : 'success'"
                    :closable="false"
                    style="margin-bottom: 8px;"
                  />
                  <p><strong>分析：</strong>{{ msg.structured.analysis }}</p>
                  <div v-if="msg.structured.suggestions">
                    <strong>建议：</strong>
                    <ul>
                      <li v-for="(s, i) in msg.structured.suggestions" :key="i">{{ s }}</li>
                    </ul>
                  </div>
                </div>

                <!-- 用药提醒结构化展示 -->
                <div v-if="msg.structured && msg.featureType === 'MEDICATION'" class="structured-result">
                  <h4>{{ msg.structured.medication_name }}</h4>
                  <div v-for="(s, i) in msg.structured.schedule" :key="i" class="med-schedule">
                    <el-tag type="primary">{{ s.time }}</el-tag>
                    <span>{{ s.dosage }} - {{ s.note }}</span>
                  </div>
                  <el-divider />
                  <div v-if="msg.structured.warnings">
                    <el-tag type="danger" v-for="(w, i) in msg.structured.warnings" :key="i" style="margin: 2px;">{{ w }}</el-tag>
                  </div>
                </div>

                <!-- 行为检测结构化展示 -->
                <div v-if="msg.structured && msg.featureType === 'BEHAVIOR'" class="structured-result">
                  <el-alert
                    :title="'风险等级: ' + msg.structured.risk_level"
                    :type="msg.structured.risk_level === '高危' ? 'error' : msg.structured.risk_level === '关注' ? 'warning' : 'success'"
                    :closable="false"
                  />
                  <p><strong>行为类型：</strong>{{ msg.structured.behavior_type }}</p>
                  <p><strong>分析：</strong>{{ msg.structured.analysis }}</p>
                  <p><strong>建议措施：</strong>{{ msg.structured.action_suggested }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="inputText"
              :placeholder="inputPlaceholder"
              @keyup.enter="sendMessage"
              clearable
            >
              <template #append>
                <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
              </template>
            </el-input>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { UserFilled, Service } from '@element-plus/icons-vue'
import { chat, healthAnalysis, medicationReminder, companion, behaviorDetect } from '@/api/modules/ai'
import { useAuthStore } from '@/stores/auth'

const features = [
  { type: 'CHAT', name: '通用对话', desc: '健康咨询问答', icon: 'ChatDotRound', color: '#409EFF' },
  { type: 'ANALYSIS', name: '健康数据分析', desc: '解读体检报告和健康指标', icon: 'DataAnalysis', color: '#67C23A' },
  { type: 'MEDICATION', name: '用药提醒生成', desc: '智能生成用药计划', icon: 'AlarmClock', color: '#E6A23C' },
  { type: 'COMPANION', name: '情感陪伴对话', desc: '老年人心理疏导陪伴', icon: 'Sunny', color: '#F56C6C' },
  { type: 'BEHAVIOR', name: '异常行为识别', desc: '识别潜在健康安全风险', icon: 'WarningFilled', color: '#9C27B0' },
]

const currentFeature = ref('CHAT')
const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const chatContainer = ref(null)
const sessionId = ref('session_' + Date.now())

const featureLabel = computed(() => features.find(f => f.type === currentFeature.value)?.name || '')

const inputPlaceholder = computed(() => {
  const map = {
    CHAT: '请输入您想咨询的健康问题...',
    ANALYSIS: '请粘贴您的体检数据或健康指标...',
    MEDICATION: '请输入药品名称和用法...',
    COMPANION: '想说点什么都可以，我在这里陪您...',
    BEHAVIOR: '请描述观察到的异常行为...',
  }
  return map[currentFeature.value] || '请输入...'
})

function switchFeature(type) {
  currentFeature.value = type
}

function sendQuickMsg(msg) {
  inputText.value = msg
  sendMessage()
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return

  messages.value.push({ role: 'user', content: text, featureType: currentFeature.value })
  inputText.value = ''
  sending.value = true

  await nextTick()
  scrollToBottom()

  try {
    const auth = useAuthStore()
    const userId = auth.user?.userId || auth.user?.id || null
    const featureType = currentFeature.value
    let res

    // 根据功能类型调用不同 API
    if (featureType === 'CHAT') {
      res = await chat({
        userId,
        sessionId: sessionId.value,
        message: text,
        featureType: 'CHAT',
      })
    } else if (featureType === 'ANALYSIS') {
      res = await healthAnalysis(text, userId)
    } else if (featureType === 'MEDICATION') {
      res = await medicationReminder(text, userId)
    } else if (featureType === 'COMPANION') {
      res = await companion(text, userId)
    } else if (featureType === 'BEHAVIOR') {
      res = await behaviorDetect(text, userId)
    } else {
      res = await chat({
        userId,
        sessionId: sessionId.value,
        message: text,
        featureType: featureType,
      })
    }

    const data = res.data
    if (data) {
      const content = data.reply || data.analysis || data.schedule_desc || JSON.stringify(data)
      messages.value.push({
        role: 'ai',
        content: content,
        featureType: featureType,
        structured: data,
      })
    }
  } catch (e) {
    messages.value.push({
      role: 'ai',
      content: '抱歉，AI 服务暂时不可用，请稍后重试。',
      featureType: currentFeature.value,
    })
  } finally {
    sending.value = false
    await nextTick()
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
}

function scrollToBottom() {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}
</script>

<style scoped>
.ai-agent-page {
  min-height: calc(100vh - 120px);
  animation: fadeIn 0.4s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.content-row { margin-top: 0; }

.feature-cards { display: flex; flex-direction: column; gap: 10px; }

.feature-card {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #EBEEF5;
  background: #fff;
}
.feature-card:hover {
  background: #F8FAFE;
  border-color: #5B8DEF;
  transform: translateX(4px);
}
.feature-card.active {
  background: linear-gradient(135deg, #ECF5FF, #F0F4FF);
  border-color: #5B8DEF;
  box-shadow: 0 2px 12px rgba(91,141,239,0.12);
}

.feature-info { flex: 1; }
.feature-name { font-size: 14px; font-weight: 600; color: #303133; }
.feature-desc { font-size: 12px; color: #909399; margin-top: 3px; }

.quick-examples { margin-top: 12px; }
.quick-examples .el-button { margin: 4px; border-radius: 8px; }

.chat-card {
  height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.chat-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.chat-header { display: flex; justify-content: space-between; align-items: center; }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 16px 4px;
  display: flex; flex-direction: column; gap: 16px;
}

.welcome-message { text-align: center; padding: 40px 20px; color: #909399; }
.welcome-message h3 { color: #303133; margin: 16px 0 8px; font-size: 18px; }
.welcome-message ul { list-style: none; padding: 0; line-height: 2.2; }

.message-item { display: flex; gap: 10px; }
.user-msg { flex-direction: row-reverse; }
.msg-bubble {
  max-width: 75%; padding: 12px 16px; border-radius: 14px;
  font-size: 14px; line-height: 1.7;
}
.user-msg .msg-bubble {
  background: linear-gradient(135deg, #5B8DEF, #7BA8F7);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(91,141,239,0.3);
}
.ai-msg .msg-bubble {
  background: #F5F7FA;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.structured-result {
  margin-top: 10px; padding: 12px; background: #fff; border-radius: 10px;
  color: #303133; font-size: 13px; box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.user-msg .structured-result { color: #303133; }
.structured-result ul { padding-left: 18px; margin: 4px 0; }
.structured-result li { margin-bottom: 3px; }

.med-schedule { display: flex; align-items: center; gap: 8px; margin: 6px 0; }

.chat-input { padding-top: 14px; border-top: 1px solid #EBEEF5; }
</style>
