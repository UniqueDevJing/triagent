<template>
  <div class="page-container">
    <div class="page-header">
      <h2>心理评测</h2>
      <el-button type="primary" @click="openDialog()">新增评测</el-button>
    </div>
    <el-table :data="tableData" stripe v-loading="loading" @row-click="showPreview">
      <template #empty>
        <EmptyState title="暂无心理评测" description="为会员创建心理健康评估问卷，关注心理状态" icon="ChatDotRound" action-text="新增评测" @action="openDialog()" />
      </template>
      <el-table-column prop="assessmentType" label="评测类型" min-width="160" />
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="resultLevel" label="结果等级" width="100" />
      <el-table-column prop="analysis" label="分析" min-width="200" show-overflow-tooltip />
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑评测' : '新增评测'" width="700px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="评测类型"><el-input v-model="form.assessmentType" /></el-form-item>
        <el-form-item label="分析"><el-input v-model="form.analysis" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="题目">
          <div class="questions-editor">
            <div v-for="(q, qi) in questionList" :key="qi" class="question-block">
              <div class="question-block-header">
                <span class="q-num">{{ qi + 1 }}</span>
                <el-input v-model="q.question" placeholder="题目内容" style="flex:1" />
                <el-button text type="danger" @click="questionList.splice(qi, 1)"><el-icon><Delete /></el-icon></el-button>
              </div>
              <div class="options-list">
                <div v-for="(opt, oi) in q.options" :key="oi" class="option-row">
                  <el-input v-model="opt.label" placeholder="选项" style="width:200px" size="small" />
                  <el-input-number v-model="opt.score" :min="0" :max="100" size="small" style="width:80px" placeholder="分值" />
                  <el-button text size="small" type="danger" @click="q.options.splice(oi, 1)"><el-icon><Delete /></el-icon></el-button>
                </div>
                <el-button size="small" text type="primary" @click="q.options.push({label:'', score:0})">+ 选项</el-button>
              </div>
            </div>
            <el-button size="small" type="primary" @click="questionList.push({question:'', options:[]})">+ 添加题目</el-button>
          </div>
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
import EmptyState from '@/components/EmptyState.vue'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({})
const questionList = ref([])
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
  if (row) {
    form.value = { ...row }
    try { questionList.value = JSON.parse(row.questions || '[]') } catch { questionList.value = [] }
  } else {
    form.value = { assessmentType: '', analysis: '' }
    questionList.value = []
  }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = { ...form.value, questions: JSON.stringify(questionList.value) }
    if (form.value.id) await api.updatePsychologyAssessment(form.value.id, payload)
    else await api.createPsychologyAssessment(payload)
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
  previewTitle.value = `题目预览 - ${row.assessmentType}`
  previewLoading.value = false
  try {
    if (row.questions) {
      previewItems.value = JSON.parse(row.questions)
      if (previewItems.value.length > 0) return
    }
    const res = await api.getPsychologyAssessment(row.id)
    const detail = res.data
    if (detail.questions) {
      previewItems.value = typeof detail.questions === 'string' ? JSON.parse(detail.questions) : detail.questions
    }
  } catch { previewItems.value = [] }
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
.questions-editor { width: 100%; max-height: 400px; overflow-y: auto; }
.question-block {
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  background: #FAFAFA;
}
.question-block-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.q-num {
  display: inline-flex; align-items: center; justify-content: center;
  width: 24px; height: 24px; border-radius: 50%;
  background: #3B6FF5; color: #fff; font-size: 12px; font-weight: 600;
  flex-shrink: 0;
}
.options-list { padding-left: 32px; }
.option-row {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 6px;
}
</style>
