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

    <!-- 加载骨架屏 -->
    <el-row v-if="loading" :gutter="20" class="stats-row">
      <el-col v-for="i in 4" :key="i" :xs="12" :sm="6">
        <el-skeleton animated>
          <template #template>
            <div class="stat-card skeleton-card">
              <el-skeleton-item variant="circle" style="width:52px;height:52px" />
              <div style="flex:1;margin-left:16px">
                <el-skeleton-item variant="text" style="width:60%" />
                <el-skeleton-item variant="text" style="width:40%;height:30px;margin-top:8px" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </el-col>
    </el-row>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-card">
      <el-icon :size="48" color="#FF6B6B"><WarningFilled /></el-icon>
      <p class="error-text">数据加载失败</p>
      <el-button type="primary" @click="loadDashboardData">重新加载</el-button>
    </div>

    <template v-else>
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col v-for="card in statCards" :key="card.title" :xs="12" :sm="6">
          <StatCard
            :title="card.title"
            :value="card.value"
            :unit="card.unit"
            :icon="card.icon"
            :gradient="card.gradientClass"
            :trend="card.trend"
          />
        </el-col>
      </el-row>

      <!-- 图表区 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :xs="24" :lg="16">
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
        <el-col :xs="24" :lg="8">
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
        <EmptyState v-if="activities.length === 0" description="暂无动态" />
        <el-timeline v-else class="modern-timeline">
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
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getStats } from '@/api/modules/dashboard'
import { useRealtime } from '@/composables/useRealtime'
import StatCard from '@/components/StatCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(true)
const error = ref(false)

const statCards = ref([
  { title: '用户总数', value: 0, icon: 'User', gradientClass: 'grad-blue', trend: 8, unit: '人' },
  { title: '今日评估', value: 0, icon: 'DocumentChecked', gradientClass: 'grad-green', trend: 12, unit: '次' },
  { title: '干预中计划', value: 0, icon: 'SetUp', gradientClass: 'grad-orange', trend: -3, unit: '项' },
  { title: '知识库文章', value: 0, icon: 'Reading', gradientClass: 'grad-purple', trend: 5, unit: '篇' },
])

const activities = ref([])
const trendChart = ref(null)
const ageChart = ref(null)
const lastUpdate = ref('')
const { connected: liveConnected, connect: connectSSE } = useRealtime('dashboard', (eventName) => {
  if (eventName === 'connected') return
  loadDashboardData()
})
let trendChartInst = null
let ageChartInst = null

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
  error.value = false
  try {
    const res = await getStats()
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
    loading.value = false
    await nextTick()
    renderTrendChart(data.assessmentTrend || [])
    renderAgeChart(data.ageDistribution || [])
  } catch {
    loading.value = false
    error.value = true
  }
}

function onResize() {
  trendChartInst?.resize()
  ageChartInst?.resize()
}

onMounted(async () => {
  await loadDashboardData()
  connectSSE()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  trendChartInst?.dispose()
  ageChartInst?.dispose()
  window.removeEventListener('resize', onResize)
})

function renderTrendChart(data) {
  if (!trendChart.value) return
  trendChartInst?.dispose()
  trendChartInst = echarts.init(trendChart.value)
  trendChartInst.setOption({
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
  ageChartInst?.dispose()
  ageChartInst = echarts.init(ageChart.value)
  const colors = ['#5B8DEF', '#52C41A', '#FAAD14', '#FF6B6B']
  ageChartInst.setOption({
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
.welcome-content { position: relative; z-index: 1; }
.welcome-greeting { font-size: 26px; font-weight: 700; color: #fff; margin: 0 0 6px; }
.welcome-date { font-size: 14px; color: rgba(255,255,255,0.8); margin: 0 0 12px; }
.welcome-msg { font-size: 14px; color: rgba(255,255,255,0.65); margin: 0; }
.live-indicator { color: #52C41A; font-size: 12px; margin-left: 12px; animation: pulse 2s infinite; }
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.update-time { font-size: 12px; color: rgba(255,255,255,0.5); }
.welcome-illustration { position: relative; z-index: 1; flex-shrink: 0; }

.stats-row { margin-bottom: 0; }
.skeleton-card {
  border-radius: 16px;
  padding: 24px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  height: 120px;
}

.error-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 16px;
}
.error-text { color: #909399; font-size: 15px; margin: 16px 0 20px; }

.chart-row { margin-top: 20px; }
.chart-card { height: 390px; }
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
.chart-body { height: 300px; }

.timeline-card { margin-top: 20px; }
.modern-timeline { padding: 8px 0; }
.timeline-item-content { display: flex; align-items: center; gap: 12px; padding: 6px 0; }
.timeline-desc { font-size: 14px; color: #606266; }

@media (max-width: 768px) {
  .welcome-banner { padding: 20px; flex-direction: column; text-align: center; gap: 16px; }
  .welcome-greeting { font-size: 20px; }
  .welcome-illustration svg { width: 150px; }
  .chart-card { height: 320px; }
  .chart-body { height: 240px; }
}
</style>
