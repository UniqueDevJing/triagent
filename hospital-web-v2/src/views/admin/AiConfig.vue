<template>
  <main class="workspace model-workspace">
    <section class="page-intro"><div><span class="eyebrow">MODEL RUNTIME</span><h2>AI 模型与提示词</h2><p>将对话模型和向量模型作为可切换的运行时资源管理，配置保存后立即生效。</p></div><button class="primary-button" @click="openNew">＋ 新增模型配置</button></section>
    <section class="model-stage">
      <article v-for="m in models" :key="m.id" :class="['model-card', { disabled: !m.enabled }]">
        <header><span class="provider-mark">{{ (m.provider || 'AI').slice(0, 2).toUpperCase() }}</span><span :class="['tool-state', m.enabled ? 'on' : 'off']"><i></i>{{ m.enabled ? '运行中' : '未启用' }}</span></header>
        <span class="eyebrow">{{ m.purpose }}</span><h3>{{ m.name }}</h3><code>{{ m.model }}</code>
        <div class="model-endpoint"><span>API ENDPOINT</span><p>{{ m.base_url }}</p></div>
        <div class="model-capabilities"><span>{{ m.purpose === 'CHAT' ? '多轮对话' : '语义向量' }}</span><span>运行时热更新</span><span v-if="m.prompt_template">自定义 Prompt</span></div>
        <footer><button class="secondary-button" @click="openEdit(m)">编辑配置</button><button class="text-button" :disabled="m.purpose !== 'CHAT'" @click="test(m)">连通测试 →</button></footer>
      </article>
      <button class="model-card model-card--new" @click="openNew"><span>＋</span><b>连接另一个模型</b><small>OpenAI-compatible API</small></button>
    </section>

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
  </main>
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
