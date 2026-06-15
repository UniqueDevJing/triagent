<template>
  <div class="assessment-page">
    <div class="page-header">
      <h2 class="page-title">健康评估</h2>
      <p class="page-subtitle">使用专业量表为长者进行健康评估</p>
    </div>

    <!-- 评估量表 -->
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <span>评估量表列表</span>
        </div>
      </template>

      <el-row :gutter="20" v-loading="tplLoading">
        <el-col :span="8" v-for="tpl in templates" :key="tpl.id">
          <el-card shadow="hover" class="tpl-card" @click="startAssessment(tpl)">
            <div class="tpl-icon">
              <el-icon :size="36" color="#409EFF"><DocumentChecked /></el-icon>
            </div>
            <h4>{{ tpl.title }}</h4>
            <p class="tpl-desc">{{ tpl.description }}</p>
            <el-tag>{{ tpl.category }}</el-tag>
          </el-card>
        </el-col>
        <el-col :span="24" v-if="templates.length === 0 && !tplLoading">
          <el-empty description="暂无评估量表" />
        </el-col>
      </el-row>
    </el-card>

    <!-- 评估历史 -->
    <el-card style="margin-top: 20px;" class="content-card">
      <template #header><span>评估记录</span></template>
      <el-table :data="records" stripe v-loading="recLoading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="totalScore" label="得分" width="80" />
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="riskType(row.riskLevel)">{{ riskLabel(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportText" label="评估报告" min-width="250" />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="viewReport(row)">查看报告</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="recPage"
        v-model:page-size="recSize"
        :total="recTotal"
        layout="total, prev, pager, next"
        @change="fetchRecords"
      />
    </el-card>

    <!-- 评估答题弹窗 -->
    <el-dialog v-model="assessmentVisible" :title="currentTemplate?.title" width="700px">
      <div v-for="(q, idx) in questions" :key="idx" class="question-item">
        <h4>{{ idx + 1 }}. {{ q.text }}</h4>
        <el-radio-group v-model="answers[q.id]" class="options-group">
          <el-radio v-for="opt in q.options" :key="opt.label" :value="opt.score" border size="large" class="option-item">
            {{ opt.label }} ({{ opt.score }}分)
          </el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="assessmentVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssessment" :loading="submitting">提交评估</el-button>
      </template>
    </el-dialog>

    <!-- 评估报告弹窗 -->
    <el-dialog v-model="reportVisible" title="评估报告" width="500px">
      <el-result
        :icon="currentReport?.riskLevel === 'LOW' ? 'success' : currentReport?.riskLevel === 'MEDIUM' ? 'warning' : 'error'"
        :title="riskLabel(currentReport?.riskLevel) + '风险'"
        :sub-title="currentReport?.reportText"
      >
        <template #extra>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="评估得分">{{ currentReport?.totalScore }} 分</el-descriptions-item>
            <el-descriptions-item label="评估时间">{{ currentReport?.createdAt }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAuthStore } from '@/stores/auth'

const templates = ref([])
const records = ref([])
const tplLoading = ref(false)
const recLoading = ref(false)
const submitting = ref(false)
const recPage = ref(1)
const recSize = ref(10)
const recTotal = ref(0)

const assessmentVisible = ref(false)
const reportVisible = ref(false)
const currentTemplate = ref(null)
const currentReport = ref(null)
const questions = ref([])
const answers = ref({})

const presetQuestions = [
  { id: 1, text: '日常生活自理能力', options: [
    { label: '完全自理', score: 0 }, { label: '部分需要帮助', score: 5 }, { label: '完全依赖', score: 10 }
  ]},
  { id: 2, text: '近三个月跌倒次数', options: [
    { label: '0次', score: 0 }, { label: '1-2次', score: 5 }, { label: '3次以上', score: 10 }
  ]},
  { id: 3, text: '睡眠质量', options: [
    { label: '良好', score: 0 }, { label: '一般', score: 3 }, { label: '差', score: 6 }
  ]},
  { id: 4, text: '情绪状态', options: [
    { label: '稳定乐观', score: 0 }, { label: '偶尔低落', score: 4 }, { label: '长期抑郁', score: 8 }
  ]},
]

onMounted(() => {
  fetchTemplates()
  fetchRecords()
})

async function fetchTemplates() {
  tplLoading.value = true
  try {
    const res = await request.get('/assessments/templates')
    templates.value = res.data || []
  } catch {
    templates.value = [
      { id: 1, title: '老年人健康综合评估', description: '评估老年人整体健康状况', category: '综合评估' },
      { id: 2, title: '慢性病风险评估', description: '高血压、糖尿病等慢病风险评估', category: '慢病管理' },
      { id: 3, title: '认知功能评估', description: '评估记忆力和认知能力', category: '心理健康' },
    ]
  }
  tplLoading.value = false
}

async function fetchRecords() {
  recLoading.value = true
  try {
    const res = await request.get('/assessments/records', {
      params: { page: recPage.value, size: recSize.value }
    })
    records.value = res.data?.records || []
    recTotal.value = res.data?.total || 0
  } catch { /* keep existing */ }
  recLoading.value = false
}

function startAssessment(tpl) {
  currentTemplate.value = tpl
  // 尝试解析量表题目，否则使用预设
  try {
    if (tpl.questions) {
      questions.value = typeof tpl.questions === 'string' ? JSON.parse(tpl.questions) : tpl.questions
    } else {
      questions.value = presetQuestions
    }
  } catch { questions.value = presetQuestions }
  answers.value = {}
  assessmentVisible.value = true
}

async function submitAssessment() {
  if (Object.keys(answers.value).length === 0) {
    ElMessage.warning('请至少回答一题')
    return
  }
  submitting.value = true
  try {
    const auth = useAuthStore()
    await request.post('/assessments/submit', {
      userId: auth.user?.userId || auth.user?.id || 1,
      templateId: currentTemplate.value.id,
      answers: answers.value,
    })
    ElMessage.success('评估提交成功')
    assessmentVisible.value = false
    fetchRecords()
  } catch { ElMessage.warning('提交失败，请检查后端服务') }
  submitting.value = false
}

function viewReport(row) { currentReport.value = row; reportVisible.value = true }

function riskType(level) {
  if (level === 'LOW') return 'success'
  if (level === 'MEDIUM') return 'warning'
  return 'danger'
}
function riskLabel(level) {
  if (level === 'LOW') return '低'
  if (level === 'MEDIUM') return '中'
  return '高'
}
</script>

<style scoped>
.assessment-page { animation: fadeIn 0.4s ease; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.content-card { border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.tpl-card {
  cursor: pointer; text-align: center; margin-bottom: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 14px; border: 1px solid #EBEEF5;
}
.tpl-card:hover { transform: translateY(-6px); box-shadow: 0 12px 28px rgba(0,0,0,0.12); border-color: #5B8DEF; }
.tpl-icon { margin-bottom: 16px; }
.tpl-icon .el-icon { background: #ECF5FF; padding: 16px; border-radius: 16px; }
.tpl-desc { color: #909399; font-size: 13px; margin: 10px 0; line-height: 1.6; }
.question-item { margin-bottom: 28px; padding: 20px; background: #F8FAFC; border-radius: 12px; }
.question-item h4 { margin-bottom: 14px; color: #303133; font-size: 15px; }
.options-group { display: flex; gap: 12px; flex-wrap: wrap; }
.option-item { margin-right: 0 !important; border-radius: 10px !important; }
</style>
