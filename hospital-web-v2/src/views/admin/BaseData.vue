<template>
  <div class="card">
    <div class="section-title">基础数据 · REFERENCE DATA</div>
    <div class="toolbar">
      <el-button v-for="tab in tabs" :key="tab.key" :type="active===tab.key?'primary':'default'"
                 size="small" @click="active=tab.key; load()">{{ tab.label }}</el-button>
    </div>
    <table>
      <thead><tr><th v-for="h in cols" :key="h">{{ h }}</th></tr></thead>
      <tbody>
        <tr v-for="(row, i) in rows" :key="i">
          <td v-for="h in cols" :key="h" :class="{dns: h==='id'}">{{ fmt(row[h]) }}</td>
        </tr>
      </tbody>
    </table>
    <p v-if="!rows.length" class="dns">暂无数据</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'

const tabs = [
  { key: 'departments', label: '科室', url: '/api/admin/departments' },
  { key: 'doctors', label: '医生', url: '/api/admin/doctors' },
  { key: 'schedules', label: '排班', url: '/api/admin/schedules' },
  { key: 'symptom-tags', label: '症状标签', url: '/api/admin/symptom-tags' },
  { key: 'red-flags', label: '危急征象', url: '/api/admin/red-flags' },
  { key: 'ai-models', label: '模型配置', url: '/api/admin/ai-models' }
]
const active = ref('departments')
const rows = ref([])
const cols = ref([])
const SKIP = new Set(['create_time', 'update_time', 'password'])
function fmt(v) { if (v === null || v === undefined) return '—'; if (typeof v === 'object') return JSON.stringify(v); return v }
async function load() {
  const tab = tabs.find(t => t.key === active.value)
  const data = await api.get(tab.url)
  rows.value = data || []
  const set = new Set()
  ;(data || []).forEach(r => Object.keys(r).forEach(k => { if (!SKIP.has(k)) set.add(k) }))
  cols.value = [...set]
}
onMounted(load)
</script>
