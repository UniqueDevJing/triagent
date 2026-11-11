<template>
  <main class="workspace registry-workspace">
    <section class="page-intro"><div><span class="eyebrow">MCP TOOL REGISTRY</span><h2>Agent 能力注册中心</h2><p>每个已启用工具都会进入模型的实时能力清单。先验证，再开放给决策链。</p></div><button class="primary-button" @click="openReg(null)">＋ 注册新工具</button></section>
    <section class="registry-summary"><div><strong>{{ tools.length }}</strong><span>已注册工具</span></div><div><strong>{{ tools.filter(t => t.enabled).length }}</strong><span>当前启用</span></div><div><strong>{{ tools.filter(t => !t.enabled).length }}</strong><span>待校验 / 停用</span></div><p><i></i>运行时工具池已同步</p></section>
    <section class="tool-grid">
      <article v-for="(t, i) in tools" :key="t.id" :class="['tool-card', { disabled: !t.enabled }]">
        <header><span class="tool-icon">{{ String(i + 1).padStart(2, '0') }}</span><span :class="['tool-state', t.enabled ? 'on' : 'off']"><i></i>{{ t.enabled ? '可调用' : '已停用' }}</span></header>
        <code>{{ t.tool_code }}</code><h3>{{ t.name }}</h3><p>{{ t.description || '暂无能力描述' }}</p>
        <footer><button class="text-button" @click="openTest(t)">运行测试</button><button class="text-button" @click="openReg(t)">配置</button><button class="text-button" @click="toggle(t)">{{ t.enabled ? '停用' : '启用' }}</button><button class="danger-link" @click="del(t)">删除</button></footer>
      </article>
      <button class="tool-card tool-card--new" @click="openReg(null)"><span>＋</span><b>注册新的 Agent 工具</b><small>定义工具描述、参数契约与调用顺序</small></button>
    </section>

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
  </main>
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

async function load() { tools.value = await api.get('/admin/tools') }
async function toggle(t) {
  await api.post('/admin/tools/toggle', { id: t.id })
  ElMessage.success(`${t.tool_code} 已${t.enabled ? '停用' : '启用'}`)
  load()
}
function openTest(t) { cur.value = t; args.value = '{}'; result.value = ''; dialog.value = true }
async function runTest() {
  try {
    let parsed = {}
    try { parsed = JSON.parse(args.value || '{}') } catch (e) { ElMessage.error('参数不是合法 JSON'); return }
    const r = await api.post('/admin/tools/test', { code: cur.value.tool_code }, parsed)
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
  await api.post('/admin/tools/save', null, { ...reg })
  ElMessage.success('工具已保存——下一次 Agent 运行即按最新工具池执行')
  regDlg.value = false; load()
}
async function del(t) {
  await api.post('/admin/tools/delete', { id: t.id })
  ElMessage.success('已删除'); load()
}
onMounted(load)
</script>
