<template>
  <div class="card">
    <div class="section-title">工具注册表 · MCP TOOL REGISTRY</div>
    <p class="dns">运行时由 mcp_tool 表驱动：enabled=0 的工具有模型根本调不到。这是「数据库驱动的工具中心」。</p>
    <table>
      <thead>
        <tr><th>工具码</th><th>名称</th><th>描述</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="t in tools" :key="t.id">
          <td><code>{{ t.tool_code }}</code></td>
          <td>{{ t.name }}</td>
          <td style="color:var(--vital-ink-soft)">{{ t.description }}</td>
          <td>
            <span class="chip" :class="t.enabled ? 'ok' : 'fail'">{{ t.enabled ? 'ENABLED' : 'DISABLED' }}</span>
          </td>
          <td>
            <el-button size="small" @click="toggle(t)">{{ t.enabled ? '停用' : '启用' }}</el-button>
            <el-button size="small" type="primary" plain @click="openTest(t)">测试</el-button>
          </td>
        </tr>
      </tbody>
    </table>

    <el-dialog v-model="dialog" :title="'测试工具 · ' + (cur && cur.tool_code)" width="520px">
      <el-input type="textarea" :rows="4" v-model="args" placeholder='参数 JSON，例：{"keyword":"呼吸"}'></el-input>
      <pre class="readout" v-if="result" style="margin-top:12px;white-space:pre-wrap;">{{ result }}</pre>
      <template #footer>
        <el-button @click="dialog=false">关闭</el-button>
        <el-button type="primary" @click="runTest">调用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const tools = ref([])
const dialog = ref(false)
const cur = ref(null)
const args = ref('{}')
const result = ref('')

async function load() { tools.value = await api.get('/api/admin/tools') }
async function toggle(t) {
  await api.post('/api/admin/tools/toggle', { id: t.id })
  ElMessage.success(`${t.tool_code} 已${t.enabled ? '停用' : '启用'}`)
  load()
}
function openTest(t) { cur.value = t; args.value = '{}'; result.value = ''; dialog.value = true }
async function runTest() {
  try {
    let parsed = {}
    try { parsed = JSON.parse(args.value || '{}') } catch (e) { ElMessage.error('参数不是合法 JSON'); return }
    const r = await api.post('/api/admin/tools/test', { code: cur.value.tool_code }, parsed)
    result.value = typeof r.result === 'string' ? r.result : JSON.stringify(r.result, null, 2)
  } catch (e) { ElMessage.error(e.message) }
}
onMounted(load)
</script>
