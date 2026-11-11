<template>
  <main class="workspace usage-workspace">
    <section class="page-intro"><div><span class="eyebrow">AI OBSERVABILITY</span><h2>模型运行与成本</h2><p>从调用成功率、响应延迟到 Token 消耗，观察每一种智能场景的真实运行状态。</p></div><span class="live-badge"><i></i> DATA LIVE</span></section>
    <section class="metric-grid metric-grid--four"><article class="metric-tile"><span>总调用</span><strong>{{ sum.total }}</strong><small>所有智能场景</small></article><article class="metric-tile"><span>成功调用</span><strong>{{ sum.success }}</strong><small>{{ sum.total ? rate({ total: sum.total, success: sum.success }) : 0 }}% 完成率</small></article><article class="metric-tile"><span>平均响应</span><strong>{{ sum.avg_ms }}<em>ms</em></strong><small>端到端模型耗时</small></article><article class="metric-tile"><span>Token 消耗</span><strong>{{ sum.tokens }}</strong><small>本地降级模式计为 0</small></article></section>
    <section class="usage-grid"><article class="surface usage-chart"><header class="panel-head"><div><span class="eyebrow">WORKLOAD</span><h3>场景调用分布</h3></div></header><div class="bar-chart"><div v-for="c in byCase" :key="c.use_case"><span><b>{{ c.use_case }}</b><small>{{ c.total }} 次</small></span><i><b :style="{ width: `${barWidth(c)}%` }"></b></i></div><div v-if="!byCase.length" class="empty-inline">暂无调用数据</div></div></article><article class="surface reliability-list"><header class="panel-head"><div><span class="eyebrow">RELIABILITY</span><h3>场景健康度</h3></div></header><div v-for="c in byCase" :key="c.use_case" class="reliability-row"><span :class="['health-dot', Number(rate(c)) >= 95 ? 'healthy' : 'warning']"></span><div><b>{{ c.use_case }}</b><small>{{ c.avg_ms }}ms · {{ c.tokens }} tokens</small></div><strong>{{ rate(c) }}%</strong></div></article></section>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'
const sum = ref({ total: 0, success: 0, avg_ms: 0, tokens: 0 })
const byCase = ref([])
function rate(c) { return c.total ? ((c.success / c.total) * 100).toFixed(1) : '0.0' }
function barWidth(c) { const max = Math.max(...byCase.value.map(x => x.total), 1); return Math.max((c.total / max) * 100, 4) }
async function load() { const r = await api.get('/admin/usage'); sum.value = r.summary; byCase.value = r.byCase }
onMounted(load)
</script>
