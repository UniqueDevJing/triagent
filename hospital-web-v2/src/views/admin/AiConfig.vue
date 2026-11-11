<template>
  <div class="card">
    <div class="section-title">AI 模型配置 · 改完立刻生效（对话/向量）</div>
    <div style="margin-bottom:10px">
      <el-button type="primary" size="small" @click="openNew">新增模型配置</el-button>
    </div>
    <el-table :data="models" size="small" border>
      <el-table-column prop="id" label="ID" width="55" />
      <el-table-column prop="name" label="名称" width="190" />
      <el-table-column prop="purpose" label="用途" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.purpose === 'CHAT' ? 'primary' : 'success'">{{ row.purpose }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="model" label="模型" width="160"><template #default="{ row }"><code>{{ row.model }}</code></template></el-table-column>
      <el-table-column prop="base_url" label="端点" />
      <el-table-column prop="enabled" label="启用" width="70">
        <template #default="{ row }"><el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? 'ON' : 'OFF' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" link :type="row.purpose === 'CHAT' ? 'success' : 'info'"
                     :disabled="row.purpose !== 'CHAT'" @click="test(row)">连通测试</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" :title="form.id ? '编辑配置' : '新增配置'" width="620px">
      <el-form label-width="110px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="用途">
          <el-select v-model="form.purpose" style="width:100%">
            <el-option label="CHAT 对话" value="CHAT" /><el-option label="EMBEDDING 向量" value="EMBEDDING" />
          </el-select>
        </el-form-item>
        <el-form-item label="Provider"><el-input v-model="form.provider" placeholder="dashscope / deepseek / openai" /></el-form-item>
        <el-form-item label="Base URL"><el-input v-model="form.base_url" /></el-form-item>
        <el-form-item label="模型名"><el-input v-model="form.model" placeholder="qwen-flash / text-embedding-v3" /></el-form-item>
        <el-form-item label="Prompt 模板">
          <el-input v-model="form.prompt_template" type="textarea" :rows="4"
                    placeholder="分诊 Agent 的系统提示词（留空则用代码内置默认值）" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="testDlg" title="连通性测试结果" width="480px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="模型">{{ testing.model }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="testing.ok ? 'success' : 'danger'">{{ testing.ok ? '连通正常' : '连通失败' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ testing.ms }} ms</el-descriptions-item>
        <el-descriptions-item label="模型回复">{{ testing.reply }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const models = ref([])
const dlg = ref(false)
const form = reactive({})
const testDlg = ref(false)
const testing = reactive({ model: '', ok: false, ms: 0, reply: '' })

async function load() { models.value = await api.get('/admin/ai-models') }
onMounted(load)

function openNew() {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, { name: '', provider: 'dashscope', purpose: 'CHAT', enabled: 1, sort: 40,
    base_url: 'https://dashscope.aliyuncs.com/compatible-mode/v1', model: '', prompt_template: '' })
  dlg.value = true
}
function openEdit(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, { ...row })
  dlg.value = true
}
async function save() {
  await api.post('/admin/ai-models/save', null, form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
async function test(row) {
  testing.model = row.model; testing.ok = false; testing.ms = 0; testing.reply = '测试中...'; testDlg.value = true
  const r = await api.post('/admin/ai-models/test', null,
    { base_url: row.base_url, model: row.model, purpose: row.purpose })
  testing.ok = r.ok; testing.ms = r.ms; testing.reply = r.reply
}
</script>
