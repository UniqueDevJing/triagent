<template>
  <main class="workspace knowledge-workspace">
    <section class="page-intro"><div><span class="eyebrow">CLINICAL KNOWLEDGE</span><h2>医学知识工作室</h2><p>组织可信资料，观察切片入库状态，并直接验证 Agent 的检索上下文。</p></div></section>
    <section class="knowledge-layout">
      <aside class="surface knowledge-library">
        <header class="panel-head"><div><span class="eyebrow">LIBRARY</span><h3>资料库</h3></div><span class="queue-count">{{ docs.length }}</span></header>
        <el-select v-model="baseId" placeholder="选择知识库" style="width:100%" @change="loadDocs">
          <el-option v-for="b in bases" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <div class="document-list"><article v-for="d in docs" :key="d.id"><span class="doc-icon">MD</span><div><b>{{ d.title }}</b><small>#{{ d.id }} · {{ d.status }}</small></div><i></i></article><div v-if="!docs.length" class="empty-inline">知识库尚无文档</div></div>
      </aside>
      <article class="surface ingest-studio">
        <header class="panel-head"><div><span class="eyebrow">INGEST</span><h3>内容入库</h3></div><span class="panel-note">自动切片 + 向量化</span></header>
        <div class="ingest-fields">
          <label>文档标题<el-input v-model="newTitle" placeholder="例如：成人哮喘诊疗指南" /></label>
          <label>结构化内容<el-input v-model="newContent" type="textarea" :rows="8" placeholder="粘贴可信医学资料，系统会自动完成分段和向量化…" /></label>
        </div>
        <div class="ingest-actions"><el-upload :show-file-list="false" accept=".txt,.md" :auto-upload="false" :on-change="onFilePick" :disabled="!baseId"><button class="secondary-button" :disabled="!baseId">选择 TXT / MD 文件</button></el-upload><span v-if="pickFile">{{ pickFile.name }} · {{ (pickFile.size/1024).toFixed(1) }} KB</span><button v-if="pickFile" class="secondary-button" :disabled="uploading" @click="doUpload">{{ uploading ? '处理中…' : '上传并向量化' }}</button><button class="primary-button" @click="addDoc">保存到知识库</button></div>
      </article>
    </section>

    <section class="surface retrieval-lab">
      <header class="panel-head"><div><span class="eyebrow">RETRIEVAL LAB</span><h3>语义检索验证</h3></div><span class="panel-note">TOP K = 4</span></header>
      <div class="retrieval-search"><input v-model="q" placeholder="输入一个真实的临床问题，例如：哮喘急性发作如何处理？" @keyup.enter="retrieve"><button class="primary-button" @click="retrieve">执行检索</button></div>
      <div class="retrieval-results">
        <article v-for="(s, i) in hits" :key="i"><header><span>RESULT {{ String(i + 1).padStart(2, '0') }}</span><b>{{ s.score }} 相似度</b></header><p>{{ s.content }}</p><span class="score-bar"><i :style="{ width: `${Math.min(Number(s.score) * 100, 100)}%` }"></i></span></article>
        <div v-if="searched && !hits.length" class="empty-state"><span>⌕</span><b>没有找到相关切片</b><p>尝试换一种问法或补充知识库内容。</p></div>
        <div v-if="!searched" class="retrieval-placeholder">输入问题后，这里会展示实际进入 Agent 上下文的知识切片。</div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const bases = ref([])
const baseId = ref(null)
const docs = ref([])
const newTitle = ref('')
const newContent = ref('')
const q = ref('')
const hits = ref([])
const searched = ref(false)
const pickFile = ref(null)
const uploading = ref(false)

function onFilePick(file) { pickFile.value = file.raw }
async function doUpload() {
  if (!pickFile.value || !baseId.value) return
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', pickFile.value)
    await fetch(`/v2/api/admin/knowledge/upload?baseId=${baseId.value}`, { method: 'POST', body: fd,
      headers: { Authorization: `Bearer ${localStorage.getItem('hospital_token')}` } })
      .then(r => r.json())
      .then(b => { if (b.code !== 200) throw new Error(b.msg || '上传失败') })
    ElMessage.success('文件已解析、切片并向量化入库')
    pickFile.value = null
    loadDocs()
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally { uploading.value = false }
}

async function loadBases() {
  bases.value = await api.get('/admin/knowledge/bases')
  if (bases.value.length) { baseId.value = bases.value[0].id; loadDocs() }
}
async function loadDocs() { if (baseId.value) docs.value = await api.get('/admin/knowledge/docs', { baseId: baseId.value }) }
async function addDoc() {
  if (!newTitle.value || !newContent.value) { ElMessage.warning('请填标题与内容'); return }
  await api.post('/admin/knowledge/add', { baseId: baseId.value, title: newTitle.value, content: newContent.value })
  ElMessage.success('已切片并向量化入库')
  newTitle.value = ''; newContent.value = ''
  loadDocs()
}
async function retrieve() {
  searched.value = true
  const r = await api.get('/admin/knowledge/retrieve', { q: q.value, topK: 4 })
  hits.value = (r || []).map(x => ({ content: x.content, score: (x.score || 0).toFixed(3) }))
}
onMounted(loadBases)
</script>
