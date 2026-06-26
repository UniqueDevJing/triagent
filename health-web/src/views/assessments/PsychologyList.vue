<template>
  <div class="page-container">
    <div class="page-header">
      <h2>心理评测</h2>
      <el-button type="primary" @click="openDialog()">新增评测</el-button>
    </div>
    <el-table :data="tableData" stripe v-loading="loading" @row-click="showPreview">
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click.stop="showPreview(row)">预览</el-button>
          <el-button text size="small" @click.stop="openDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="page-pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetch"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑评测' : '新增评测'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="题目">
          <el-alert type="info" :closable="false" show-icon title="题目编辑功能开发中，请通过后端 API 配置题目" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewVisible" :title="previewTitle" width="600px">
      <div v-if="previewLoading" v-loading="previewLoading" style="min-height:100px"></div>
      <div v-else-if="!previewItems || previewItems.length === 0" style="text-align:center;color:#909399;padding:40px 0">暂无题目数据</div>
      <div v-else>
        <div v-for="(item, idx) in previewItems" :key="idx" class="question-item">
          <div class="question-title">{{ idx + 1 }}. {{ item.question || item.title || item.content }}</div>
          <div v-if="item.options && item.options.length" class="question-options">
            <div v-for="(opt, oi) in item.options" :key="oi" class="option-item">
              <el-radio disabled :model-value="false" :label="opt.label || opt.value || opt" style="margin-right:4px" />
              <span>{{ opt.label || opt.value || opt }}</span>
              <span v-if="opt.score" class="option-score">({{ opt.score }}分)</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/modules/assessmentAdmin'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({})
const page = ref(1)
const size = ref(20)
const total = ref(0)

const previewVisible = ref(false)
const previewTitle = ref('')
const previewLoading = ref(false)
const previewItems = ref([])

onMounted(() => fetch())
async function fetch() {
  loading.value = true
  try {
    const res = await api.getPsychologyAssessments({ page: page.value, size: size.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function openDialog(row) {
  form.value = row ? { ...row } : { name: '', description: '' }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await api.updatePsychologyAssessment(form.value.id, form.value)
    else await api.createPsychologyAssessment(form.value)
    dialogVisible.value = false
    ElMessage.success('保存成功')
    fetch()
  } catch { /* handled by interceptor */ }
  saving.value = false
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该评测？', '提示', { type: 'warning' })
  await api.deletePsychologyAssessment(row.id)
  ElMessage.success('已删除')
  fetch()
}

async function showPreview(row) {
  previewVisible.value = true
  previewTitle.value = `题目预览 - ${row.name}`
  previewLoading.value = true
  previewItems.value = []
  try {
    const res = await api.getPsychologyAssessment(row.id)
    const detail = res.data
    if (detail.items && Array.isArray(detail.items)) {
      previewItems.value = detail.items
    } else if (detail.questions && Array.isArray(detail.questions)) {
      previewItems.value = detail.questions
    }
  } catch { previewItems.value = [] }
  previewLoading.value = false
}
</script>

<style scoped>
.question-item {
  padding: 12px 0;
  border-bottom: 1px solid #EBEEF5;
}
.question-item:last-child { border-bottom: none; }
.question-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
}
.question-options { padding-left: 20px; }
.option-item {
  display: flex;
  align-items: center;
  padding: 4px 0;
  font-size: 13px;
  color: #606266;
}
.option-score {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}
</style>
