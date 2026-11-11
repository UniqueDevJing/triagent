<template>
  <div class="card">
    <div class="section-title">工具注册表 · MCP TOOL REGISTRY</div>
    <p class="dns">运行时由 mcp_tool 表驱动：enabled=0 的工具有模型下一次真的调不到。支持注册新工具。</p>
    <div style="margin-bottom:12px">
      <el-button type="primary" size="small" @click="openReg(null)">注册新工具</el-button>
    </div>
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
            <el-button size="small" link @click="openReg(t)">编辑</el-button>
            <el-button size="small" link type="danger" @click="del(t)">删除</el-button>
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

    <el-dialog v-model="regDlg" :title="reg.id ? '编辑工具' : '注册新工具'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="工具码"><el-input v-model="reg.tool_code" placeholder="如 knowledge_retrieve" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="reg.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="reg.description" placeholder="这段描述会直接进入模型的工具清单" /></el-form-item>
        <el-form-item label="参数 Schema">
          <el-input v-model="reg.param_schema" type="textarea" :rows="4"
                    placeholder='[{"name":"query","type":"string","required":true,"desc":"检索问句"}]' />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="reg.sort" :min="0" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="reg.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regDlg=false">取消</el-button>
        <el-button type="primary" @click="saveReg">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const tools = ref([])
const dialog = ref(false)
const regDlg = ref(false)
const cur = ref(null)
const args = ref('{}')
const result = ref('')
const reg = reactive({ id: null, tool_code: '', name: '', description: '', param_schema: '[]', enabled: 1, sort: 70 })

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
function openReg(t) {
  Object.assign(reg, t ? { id: t.id, tool_code: t.tool_code, name: t.name, description: t.description,
    param_schema: t.param_schema || '[]', enabled: t.enabled, sort: t.sort } :
    { id: null, tool_code: '', name: '', description: '', param_schema: '[]', enabled: 1, sort: 70 })
  regDlg.value = true
}
async function saveReg() {
  await api.post('/api/admin/tools/save', null, { ...reg })
  ElMessage.success('工具已保存——下一次 Agent 运行即按最新工具池执行')
  regDlg.value = false; load()
}
async function del(t) {
  await api.post('/api/admin/tools/delete', { id: t.id })
  ElMessage.success('已删除'); load()
}
onMounted(load)
</script>
