<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <h1 class="welcome-greeting">{{ greeting }}</h1>
        <p class="welcome-date">{{ today }} <span class="live-indicator" v-if="liveConnected">● 实时连接</span></p>
        <p class="welcome-msg">祝您工作顺利，用心守护每一位长者的健康 <span class="update-time" v-if="lastUpdate">· 数据更新于 {{ lastUpdate }}</span></p>
      </div>
      <div class="welcome-illustration">
        <svg viewBox="0 0 200 120" fill="none" xmlns="http://www.w3.org/2000/svg">
          <ellipse cx="100" cy="110" rx="80" ry="10" fill="rgba(255,255,255,0.15)"/>
          <circle cx="70" cy="50" r="28" fill="rgba(255,255,255,0.3)" stroke="rgba(255,255,255,0.6)" stroke-width="2"/>
          <circle cx="130" cy="50" r="28" fill="rgba(255,255,255,0.3)" stroke="rgba(255,255,255,0.6)" stroke-width="2"/>
          <path d="M90 85 Q100 65 110 85" stroke="rgba(255,255,255,0.5)" stroke-width="2" fill="none"/>
          <circle cx="60" cy="20" r="4" fill="rgba(255,255,255,0.4)"/>
          <circle cx="140" cy="15" r="3" fill="rgba(255,255,255,0.4)"/>
          <circle cx="100" cy="10" r="5" fill="rgba(255,255,255,0.35)"/>
        </svg>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="(card, idx) in statCards" :key="card.title">
        <div class="stat-card" :class="card.gradient">
          <div class="stat-card-inner">
            <div class="stat-icon-box">
              <el-icon :size="26"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">{{ card.title }}</div>
              <div class="stat-value">
                <span class="count-up">{{ card.value }}</span>
                <span v-if="card.unit" class="stat-unit">{{ card.unit }}</span>
              </div>
              <div class="stat-trend" v-if="card.trend">
                <el-icon :size="12"><component :is="card.trend > 0 ? 'Top' : 'Bottom'" /></el-icon>
                <span>{{ Math.abs(card.trend) }}% 较昨日</span>
              </div>
            </div>
          </div>
          <div class="stat-card-bg">
            <el-icon :size="72" color="rgba(255,255,255,0.08)"><component :is="card.icon" /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <div class="page-card chart-card">
          <div class="chart-header">
            <h3 class="chart-title">
              <el-icon :size="18" color="#5B8DEF"><TrendCharts /></el-icon>
              近7天评估趋势
            </h3>
            <el-tag size="small" effect="plain" round>本周</el-tag>
          </div>
          <div ref="trendChart" class="chart-body"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="page-card chart-card">
          <div class="chart-header">
            <h3 class="chart-title">
              <el-icon :size="18" color="#FF6B6B"><PieChart /></el-icon>
              年龄分布
            </h3>
          </div>
          <div ref="ageChart" class="chart-body"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近动态 -->
    <div class="page-card timeline-card">
      <div class="chart-header">
        <h3 class="chart-title">
          <el-icon :size="18" color="#FAAD14"><Clock /></el-icon>
          最近动态
        </h3>
      </div>
      <el-timeline class="modern-timeline">
        <el-timeline-item
          v-for="(item, idx) in activities"
          :key="idx"
          :timestamp="item.time"
          placement="top"
          :color="item.type === '评估' ? '#5B8DEF' : '#52C41A'"
          size="large"
        >
          <div class="timeline-item-content">
            <el-tag
              :type="item.type === '评估' ? '' : 'success'"
              :color="item.type === '评估' ? '#5B8DEF' : undefined"
              effect="dark"
              size="small"
              round
            >
              {{ item.type }}
            </el-tag>
            <span class="timeline-desc">{{ item.desc }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const statCards = ref([
  { title: '用户总数', value: 0, icon: 'User', gradient: 'grad-blue', trend: 8, unit: '人' },
  { title: '今日评估', value: 0, icon: 'DocumentChecked', gradient: 'grad-green', trend: 12, unit: '次' },
  { title: '干预中计划', value: 0, icon: 'SetUp', gradient: 'grad-orange', trend: -3, unit: '项' },
  { title: '知识库文章', value: 0, icon: 'Reading', gradient: 'grad-purple', trend: 5, unit: '篇' },
])

const activities = ref([])
const trendChart = ref(null)
const ageChart = ref(null)
const lastUpdate = ref('')
const liveConnected = ref(false)
let refreshTimer = null
let sseConnection = null

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 9) return '早上好 ☀️'
  if (h < 12) return '上午好 🌤️'
  if (h < 14) return '中午好 ☀️'
  if (h < 18) return '下午好 🌈'
  return '晚上好 🌙'
})

const today = computed(() => {
  const d = new Date()
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${days[d.getDay()]}`
})

async function loadDashboardData() {
  try {
    const res = await request.get('/dashboard/stats')
    const data = res.data
    statCards.value[0].value = data.totalUsers
    statCards.value[1].value = data.todayAssessments
    statCards.value[2].value = data.activeInterventions
    statCards.value[3].value = data.knowledgeArticles
    activities.value = data.recentActivities || []
    const now = new Date()
    lastUpdate.value = now.getHours().toString().padStart(2,'0') + ':' +
                       now.getMinutes().toString().padStart(2,'0') + ':' +
                       now.getSeconds().toString().padStart(2,'0')
    await nextTick()
    renderTrendChart(data.assessmentTrend || [])
    renderAgeChart(data.ageDistribution || [])
  } catch (e) { /* 保持现有数据 */ }
}

function connectSSE() {
  try {
    sseConnection = new EventSource('/api/health-records/subscribe')
    sseConnection.onopen = () => { liveConnected.value = true }
    sseConnection.addEventListener('health_record_created', () => {
      loadDashboardData()
    })
    sseConnection.onerror = () => {
      liveConnected.value = false
      sseConnection?.close()
      // SSE 失败后退到轮询
      refreshTimer = setInterval(loadDashboardData, 30000)
    }
  } catch {
    // 浏览器不支持 SSE 时使用轮询
    refreshTimer = setInterval(loadDashboardData, 30000)
  }
}

onMounted(async () => {
  await loadDashboardData()
  connectSSE()
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  sseConnection?.close()
})

function renderTrendChart(data) {
  if (!trendChart.value) return
  const chart = echarts.init(trendChart.value)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#EBEEF5',
      textStyle: { color: '#303133' },
      boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
    },
    grid: { left: 20, right: 30, top: 20, bottom: 20 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
      axisLine: { lineStyle: { color: '#EBEEF5' } },
      axisTick: { show: false },
      axisLabel: { color: '#909399' },
    },
    yAxis: {
      type: 'value',
      name: '人次',
      splitLine: { lineStyle: { color: '#F5F7FA', type: 'dashed' } },
      axisLabel: { color: '#909399' },
    },
    series: [{
      data: data.map(d => d.count),
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: '#5B8DEF', width: 3 },
      itemStyle: {
        color: '#5B8DEF',
        borderColor: '#fff',
        borderWidth: 2,
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(91,141,239,0.25)' },
          { offset: 1, color: 'rgba(91,141,239,0.02)' },
        ]),
      },
    }],
  })
}

function renderAgeChart(data) {
  if (!ageChart.value) return
  const chart = echarts.init(ageChart.value)
  const colors = ['#5B8DEF', '#52C41A', '#FAAD14', '#FF6B6B']
  chart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#EBEEF5', textStyle: { color: '#303133' } },
    color: colors,
    series: [{
      type: 'pie',
      radius: ['50%', '78%'],
      center: ['50%', '55%'],
      data: data,
      label: {
        formatter: '{b}\n{d}%',
        color: '#606266',
        fontSize: 12,
      },
      emphasis: {
        label: { fontSize: 16, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 20, shadowColor: 'rgba(0,0,0,0.15)' },
      },
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 3,
      },
    }],
  })
}
</script>

<style scoped>
/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #5B8DEF 0%, #7BA8F7 40%, #91B8FA 100%);
  border-radius: 16px;
  padding: 32px 40px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  overflow: hidden;
}
.welcome-content {
  position: relative;
  z-index: 1;
}
.welcome-greeting {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px;
}
.welcome-date {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
  margin: 0 0 12px;
}
.welcome-msg {
  font-size: 14px;
  color: rgba(255,255,255,0.65);
  margin: 0;
}
.live-indicator {
  color: #52C41A;
  font-size: 12px;
  margin-left: 12px;
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.update-time {
  font-size: 12px;
  color: rgba(255,255,255,0.5);
}
.welcome-illustration {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 0;
}
.stat-card {
  position: relative;
  border-radius: 16px;
  padding: 24px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  height: 120px;
  display: flex;
  align-items: center;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0,0,0,0.12);
}
.stat-card.grad-blue { background: linear-gradient(135deg, #5B8DEF, #7BA8F7); }
.stat-card.grad-green { background: linear-gradient(135deg, #52C41A, #73D13D); }
.stat-card.grad-orange { background: linear-gradient(135deg, #FAAD14, #FFC53D); }
.stat-card.grad-purple { background: linear-gradient(135deg, #FF6B6B, #FF8787); }

.stat-card-inner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}
.stat-icon-box {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.stat-info {
  flex: 1;
}
.stat-label {
  font-size: 13px;
  color: rgba(255,255,255,0.8);
  margin-bottom: 4px;
}
.stat-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.count-up {
  font-size: 30px;
  font-weight: 800;
  color: #fff;
  line-height: 1;
}
.stat-unit {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
}
.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255,255,255,0.75);
  margin-top: 4px;
}
.stat-card-bg {
  position: absolute;
  right: -8px;
  top: -8px;
  z-index: 0;
}

/* 图表区 */
.chart-row {
  margin-top: 20px;
}
.chart-card {
  height: 390px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 16px;
  border-bottom: 1px solid #F5F7FA;
}
.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.chart-body {
  height: 300px;
}

/* 时间线 */
.timeline-card {
  margin-top: 20px;
}
.modern-timeline {
  padding: 8px 0;
}
.timeline-item-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
}
.timeline-desc {
  font-size: 14px;
  color: #606266;
}
</style>
