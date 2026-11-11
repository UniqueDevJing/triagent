<template>
  <div>
    <div class="card">
      <div class="section-title">知识库 · KNOWLEDGE BASE</div>
      <div class="toolbar">
        <el-select v-model="baseId" placeholder="选择知识库" style="width:220px" @change="loadDocs">
          <el-option v-for="b in bases" :key="b.id" :label="b.name" :value="b.id"></el-option>
        </el-select>
        <el-input v-model="newTitle" placeholder="新文档标题" style="width:180px"></el-input>
        <el-input v-model="newContent" placeholder="文档内容（自动切片+向量化）" style="width:300px"></el-input>
        <el-button type="primary" @click="addDoc">入库</el-button>
      </div>
      <div class="toolbar" style="margin-top:10px;">
        <el-upload :show-file-list="false" accept=".txt,.md" :auto-upload="false"
                   :on-change="onFilePick" :disabled="!baseId">
          <el-button type="success" plain :disabled="!baseId">上传文档文件（txt / md）</el-button>
        </el-upload>
        <span v-if="pickFile" class="readout">已选：{{ pickFile.name }}（{{ (pickFile.size/1024).toFixed(1) }} KB）</span>
        <el-button v-if="pickFile" type="success" :loading="uploading" @click="doUpload">解析并向量化入库</el-button>
      </div>
      <table>
        <thead><tr><th>文档ID</th><th>标题</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="d in docs" :key="d.id"><td>{{ d.id }}</td><td>{{ d.title }}</td>
            <td><span class="chip ok">{{ d.status }}</span></td></tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div class="section-title">检索验证 · RETRIEVAL</div>
      <div class="toolbar">
        <el-input v-model="q" placeholder="输入检索问题，如：哮喘怎么处理" style="width:320px"
                  @keyup.enter="retrieve"></el-input>
        <el-button type="primary" @click="retrieve">向量检索</el-button>
      </div>
      <div v-for="(s, i) in hits" :key="i" class="card" style="margin-bottom:10px;padding:12px 14px;">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span class="readout">#{{ i + 1 }} · score {{ s.score }}</span>
        </div>
        <div style="margin-top:6px;line-height:1.6;">{{ s.content }}</div>
      </div>
      <p v-if="searched && !hits.length" class="dns">无命中（本地降级向量阈值 0.08，可调整 embedding 维度或切换真实模型）。</p>
    </div>
  </div>
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
    await fetch(`/v2/api/admin/knowledge/upload?baseId=${baseId.value}`, { method: 'POST', body: fd })
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
  bases.value = await api.get('/api/admin/knowledge/bases')
  if (bases.value.length) { baseId.value = bases.value[0].id; loadDocs() }
}
async function loadDocs() { if (baseId.value) docs.value = await api.get('/api/admin/knowledge/docs', { baseId: baseId.value }) }
async function addDoc() {
  if (!newTitle.value || !newContent.value) { ElMessage.warning('请填标题与内容'); return }
  await api.post('/api/admin/knowledge/add', { baseId: baseId.value, title: newTitle.value, content: newContent.value })
  ElMessage.success('已切片并向量化入库')
  newTitle.value = ''; newContent.value = ''
  loadDocs()
}
async function retrieve() {
  searched.value = true
  const r = await api.get('/api/admin/knowledge/retrieve', { q: q.value, topK: 4 })
  hits.value = (r || []).map(x => ({ content: x.content, score: (x.score || 0).toFixed(3) }))
}
onMounted(loadBases)
</script>
