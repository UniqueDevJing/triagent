<template>
  <div>
    <div class="card">
      <div class="section-title">AI 调用总览 · USAGE SUMMARY</div>
      <div class="stat-grid">
        <div class="stat"><div class="num">{{ sum.total }}</div><div class="lbl">总调用</div></div>
        <div class="stat"><div class="num">{{ sum.success }}</div><div class="lbl">成功</div></div>
        <div class="stat"><div class="num">{{ sum.avg_ms }}ms</div><div class="lbl">平均耗时</div></div>
        <div class="stat warn"><div class="num">{{ sum.tokens }}</div><div class="lbl">Token 消耗</div></div>
      </div>
      <p class="dns" style="margin-top:10px;">本地降级（无 API Key）时 token=0；配置真实模型后此处统计真实消耗。</p>
    </div>
    <div class="card">
      <div class="section-title">按场景聚合 · BY CASE</div>
      <table>
        <thead><tr><th>场景</th><th>次数</th><th>成功率</th><th>平均耗时</th><th>Token</th></tr></thead>
        <tbody>
          <tr v-for="c in byCase" :key="c.use_case">
            <td><code>{{ c.use_case }}</code></td><td>{{ c.total }}</td>
            <td><span class="readout">{{ rate(c) }}%</span></td>
            <td>{{ c.avg_ms }}ms</td><td>{{ c.tokens }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'
const sum = ref({ total: 0, success: 0, avg_ms: 0, tokens: 0 })
const byCase = ref([])
function rate(c) { return c.total ? ((c.success / c.total) * 100).toFixed(1) : '0.0' }
async function load() { const r = await api.get('/api/admin/usage'); sum.value = r.summary; byCase.value = r.byCase }
onMounted(load)
</script>
