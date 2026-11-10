<template>
  <div class="dashboard">
    <!-- 页面头 -->
    <div class="dash-head">
      <div class="dash-head-text">
        <div class="dash-eyebrow">工作台 / 数据总览</div>
        <h1 class="dash-greeting">{{ greeting }}</h1>
        <p class="dash-meta">
          <span>{{ today }}</span>
          <span class="dot-sep">·</span>
          <span v-if="lastUpdate">数据更新于 {{ lastUpdate }}</span>
          <span v-if="liveConnected" class="live-badge"><i></i>实时连接</span>
        </p>
        <p class="dash-msg">用心守护每一位长者的健康，今日业务数据已同步。</p>
      </div>
      <router-link to="/assistant" class="dash-cta">
        <div class="cta-icon"><el-icon :size="20"><ChatDotRound /></el-icon></div>
        <div class="cta-text">
          <div class="cta-title">开启智能分诊</div>
          <div class="cta-sub">多 Agent 协作 · 透明推理</div>
        </div>
        <el-icon class="cta-arrow" :size="16"><Right /></el-icon>
      </router-link>
    </div>

    <!-- 加载骨架屏 -->
    <el-row v-if="loading" :gutter="16" class="stats-row">
      <el-col v-for="i in 4" :key="i" :xs="12" :sm="6">
        <el-skeleton animated>
          <template #template>
            <div class="stat-card skeleton-card">
              <el-skeleton-item variant="circle" style="width:38px;height:38px" />
              <div style="flex:1;margin-left:14px">
                <el-skeleton-item variant="text" style="width:55%" />
                <el-skeleton-item variant="text" style="width:40%;height:28px;margin-top:8px" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </el-col>
    </el-row>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-card">
      <el-icon :size="40" color="#E2564D"><WarningFilled /></el-icon>
      <p class="error-text">数据加载失败，请检查服务连接</p>
      <el-button type="primary" @click="loadDashboardData">重新加载</el-button>
    </div>

    <template v-else>
      <el-row :gutter="16" class="stats-row">
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

      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :lg="16">
          <div class="page-card chart-card">
            <div class="chart-header">
              <div>
                <h3 class="chart-title">近 7 天评估趋势</h3>
                <p class="chart-sub">每日健康评估完成人次</p>
              </div>
              <el-tag size="small" effect="plain" round>本周</el-tag>
            </div>
            <div ref="trendChart" class="chart-body"></div>
          </div>
        </el-col>
        <el-col :xs="24" :lg="8">
          <div class="page-card chart-card">
            <div class="chart-header">
              <div>
                <h3 class="chart-title">年龄分布</h3>
                <p class="chart-sub">在册会员结构占比</p>
              </div>
            </div>
            <div ref="ageChart" class="chart-body"></div>
          </div>
        </el-col>
      </el-row>

      <div class="page-card timeline-card">
        <div class="chart-header">
          <div>
            <h3 class="chart-title">最近动态</h3>
            <p class="chart-sub">业务事件实时同步</p>
          </div>
        </div>
        <EmptyState v-if="activities.length === 0" description="暂无动态" />
        <el-timeline v-else class="modern-timeline">
          <el-timeline-item
            v-for="(item, idx) in activities"
            :key="idx"
            :timestamp="item.time"
            placement="top"
            :color="item.type === '评估' ? '#0E9F8A' : '#17A673'"
            size="normal"
          >
            <div class="timeline-item-content">
              <el-tag :type="item.type === '评估' ? 'primary' : 'success'" effect="light" size="small" round>{{ item.type }}</el-tag>
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
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
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
      borderColor: '#E1E8EE',
      textStyle: { color: '#16283A' },
      boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
    },
    grid: { left: 20, right: 30, top: 20, bottom: 20 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
      axisLine: { lineStyle: { color: '#E1E8EE' } },
      axisTick: { show: false },
      axisLabel: { color: '#9AA8B5' },
    },
    yAxis: {
      type: 'value',
      name: '人次',
      splitLine: { lineStyle: { color: '#EDF1F5', type: 'dashed' } },
      axisLabel: { color: '#9AA8B5' },
    },
    series: [{
      data: data.map(d => d.count),
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: '#0E9F8A', width: 3 },
      itemStyle: {
        color: '#0E9F8A',
        borderColor: '#fff',
        borderWidth: 2,
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(14,159,138,0.20)' },
          { offset: 1, color: 'rgba(14,159,138,0.01)' },
        ]),
      },
    }],
  })
}

function renderAgeChart(data) {
  if (!ageChart.value) return
  ageChartInst?.dispose()
  ageChartInst = echarts.init(ageChart.value)
  const colors = ['#0E9F8A', '#2F6FED', '#E8A33D', '#8C7BD8']
  ageChartInst.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#E1E8EE', textStyle: { color: '#16283A' } },
    color: colors,
    series: [{
      type: 'pie',
      radius: ['50%', '78%'],
      center: ['50%', '55%'],
      data: data,
      label: {
        formatter: '{b}\n{d}%',
        color: '#42566B',
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
.dash-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.dash-eyebrow { font-size: 11px; font-weight: 650; letter-spacing: .12em; color: var(--text-4); text-transform: uppercase; margin-bottom: 10px; }
.dash-greeting { font-size: 27px; font-weight: 680; color: var(--text-1); letter-spacing: -.02em; margin: 0 0 8px; }
.dash-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 12.5px; color: var(--text-3); margin: 0; }
.dot-sep { color: #C9D3DC; }
.live-badge { display: inline-flex; align-items: center; gap: 6px; color: var(--success); font-size: 11.5px; margin-left: 6px; }
.live-badge i { width: 6px; height: 6px; border-radius: 50%; background: var(--success); box-shadow: 0 0 0 3px rgba(23,166,115,.14); animation: livePulse 2s ease-in-out infinite; }
@keyframes livePulse { 50% { opacity: .45; } }
.dash-msg { font-size: 12.5px; color: var(--text-4); margin: 10px 0 0; }

.dash-cta {
  display: flex; align-items: center; gap: 13px; flex-shrink: 0;
  background: var(--ink-900); color: #fff; text-decoration: none;
  border-radius: var(--radius); padding: 14px 18px; min-width: 246px;
  border: 1px solid rgba(255,255,255,.06);
  transition: transform .18s ease, box-shadow .18s ease;
}
.dash-cta:hover { transform: translateY(-2px); box-shadow: 0 10px 28px rgba(12,34,55,.22); }
.cta-icon { width: 38px; height: 38px; border-radius: 10px; display: flex; align-items: center; justify-content: center; background: rgba(20,179,155,.18); color: #4FD1B5; flex-shrink: 0; }
.cta-text { flex: 1; }
.cta-title { font-size: 13.5px; font-weight: 650; }
.cta-sub { font-size: 11px; color: rgba(255,255,255,.45); margin-top: 4px; }
.cta-arrow { color: rgba(255,255,255,.5); }

.stats-row { margin-bottom: 0; }
.skeleton-card { border-radius: var(--radius); padding: 20px 22px; background: var(--surface); display: flex; align-items: center; height: 118px; }
.error-card { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 72px 20px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); }
.error-text { color: var(--text-3); font-size: 13.5px; margin: 16px 0 20px; }

.chart-row { margin-top: 16px; }
.chart-card { height: 366px; }
.chart-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; padding-bottom: 14px; border-bottom: 1px solid var(--line-soft); }
.chart-title { font-size: 14.5px; font-weight: 650; color: var(--text-1); margin: 0; }
.chart-sub { font-size: 11.5px; color: var(--text-4); margin: 5px 0 0; }
.chart-body { height: 282px; }
.timeline-card { margin-top: 16px; }
.modern-timeline { padding: 10px 0 4px; }
.timeline-item-content { display: flex; align-items: center; gap: 12px; padding: 5px 0; }
.timeline-desc { font-size: 13px; color: var(--text-2); }

@media (max-width: 768px) {
  .dash-head { flex-direction: column; align-items: stretch; gap: 16px; }
  .dash-greeting { font-size: 22px; }
  .dash-cta { min-width: 0; }
  .chart-card { height: 310px; }
  .chart-body { height: 226px; }
}
</style>
