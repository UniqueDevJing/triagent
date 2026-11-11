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
