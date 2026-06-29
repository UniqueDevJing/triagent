<template>
  <div class="health-records-page">
    <div class="page-header">
      <h2 class="page-title">健康档案</h2>
      <p class="page-subtitle">管理用户的体检、门诊和自测健康记录</p>
    </div>

    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-select v-model="filterUserId" placeholder="筛选用户" clearable style="width: 200px;" @change="fetchRecords">
              <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
            </el-select>
            <el-input v-model="keyword" placeholder="搜索记录..." clearable style="width: 240px; margin-left: 12px;" @keyup.enter="fetchRecords" />
            <el-button type="primary" @click="fetchRecords" style="margin-left: 12px;">查询</el-button>
          </div>
          <el-button type="primary" @click="showDialog(null)">新增档案</el-button>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" width="90">
          <template #default="{ row }">{{ getUserName(row.userId) }}</template>
        </el-table-column>
        <el-table-column prop="recordDate" label="记录日期" width="120">
          <template #default="{ row }">{{ fmtDate(row.recordDate) }}</template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="typeColor(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="健康指标" min-width="260">
          <template #default="{ row }">
            <div class="metrics-cell">
              <span v-for="(v, k) in parseMetrics(row.metrics)" :key="k" class="metric-chip">
                <strong>{{ k }}</strong>: {{ v }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="doctorNotes" label="医生备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="fetchRecords"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑档案' : '新增档案'" width="650px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户" prop="userId">
              <el-select v-model="form.userId" style="width: 100%" :disabled="!!editId">
                <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录日期" prop="recordDate">
              <el-date-picker v-model="form.recordDate" type="date" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="体检" value="体检" />
                <el-option label="门诊" value="门诊" />
                <el-option label="自测" value="自测" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="健康指标">
          <div class="metrics-editor">
            <div v-for="(item, idx) in metricsList" :key="idx" class="metric-row">
              <el-input v-model="item.key" placeholder="指标名" style="width: 140px;" />
              <el-input v-model="item.val" placeholder="值" style="width: 140px; margin-left: 8px;" />
              <el-button text type="danger" @click="metricsList.splice(idx, 1)"><el-icon><Delete /></el-icon></el-button>
            </div>
            <el-button size="small" @click="metricsList.push({ key: '', val: '' })">+ 添加指标</el-button>
          </div>
        </el-form-item>

        <el-form-item label="报告URL">
          <el-input v-model="form.reportUrl" placeholder="可选，体检报告链接" />
        </el-form-item>
        <el-form-item label="医生备注">
          <el-input v-model="form.doctorNotes" type="textarea" :rows="3" placeholder="可选，医生备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers } from '@/api/modules/users'
import { getHealthRecords, createHealthRecord, updateHealthRecord, deleteHealthRecord } from '@/api/modules/healthRecords'
import { formatDate } from '@/utils/format'
import { useRealtime } from '@/composables/useRealtime'
import { useFormDraft } from '@/composables/useFormDraft'

const users = ref([])
const tableData = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterUserId = ref(null)
const keyword = ref('')

const dialogVisible = ref(false)
const formRef = ref(null)
const editId = ref(null)
const metricsList = ref([])

const form = reactive({
  userId: null,
  recordDate: '',
  type: '体检',
  reportUrl: '',
  doctorNotes: '',
})

const rules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  recordDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

const typeMap = { '体检': 'primary', '门诊': 'warning', '自测': 'success' }
const typeLabel = (t) => typeMap[t] ? t : t
const typeColor = (t) => typeMap[t] || 'info'

const { hasDraft, restoreDraft, clearDraft } = useFormDraft('health-record-form', {
  form, metricsList,
})

const userMap = computed(() => {
  const m = {}
  users.value.forEach(u => { m[u.id] = u.name })
  return m
})
const getUserName = (uid) => userMap.value[uid] || `用户${uid}`

onMounted(async () => {
  try {
    const res = await getUsers({ page: 1, size: 100 })
    users.value = res.data?.records || []
  } catch { /* 无后端时保持空列表 */ }
  fetchRecords()
  useRealtime('health_records', (eventName) => {
    if (eventName !== 'connected') fetchRecords()
  }).connect()
})

async function fetchRecords() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filterUserId.value) params.userId = filterUserId.value
    const res = await getHealthRecords(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* keep existing */ }
  loading.value = false
}

function parseMetrics(metricsStr) {
  if (!metricsStr) return {}
  try {
    return typeof metricsStr === 'string' ? JSON.parse(metricsStr) : metricsStr
  } catch { return {} }
}

async function showDialog(row) {
  if (row) {
    editId.value = row.id
    form.userId = row.userId
    form.recordDate = row.recordDate
    form.type = row.type
    form.reportUrl = row.reportUrl || ''
    form.doctorNotes = row.doctorNotes || ''
    const m = parseMetrics(row.metrics)
    metricsList.value = Object.entries(m).map(([k, v]) => ({ key: k, val: String(v) }))
  } else {
    editId.value = null
    if (hasDraft.value) {
      try {
        await ElMessageBox.confirm('检测到未保存的草稿，是否恢复？', '提示', {
          confirmButtonText: '恢复草稿',
          cancelButtonText: '重新填写',
          type: 'info',
        })
        restoreDraft()
        dialogVisible.value = true
        return
      } catch {}
    }
    form.userId = null
    form.recordDate = ''
    form.type = '体检'
    form.reportUrl = ''
    form.doctorNotes = ''
    metricsList.value = [{ key: '', val: '' }]
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const metrics = {}
    metricsList.value.forEach(m => { if (m.key) metrics[m.key] = isNaN(m.val) ? m.val : Number(m.val) })
    const payload = {
      ...form,
      metrics: JSON.stringify(metrics),
      recordDate: form.recordDate,
    }
    if (editId.value) {
      await updateHealthRecord(editId.value, payload)
    } else {
      await createHealthRecord(payload)
      clearDraft()
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchRecords()
  } catch { ElMessage.warning('保存失败，请检查后端服务') }
  saving.value = false
}

function handleDelete(id) {
  ElMessageBox.confirm('确定要删除这条健康档案吗？', '确认删除', { type: 'warning' })
    .then(async () => {
      await deleteHealthRecord(id)
      ElMessage.success('已删除')
      fetchRecords()
    })
    .catch(() => {})
}

const fmtDate = (d) => formatDate(d)
</script>

<style scoped>
.health-records-page { animation: fadeIn 0.4s ease; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.content-card { border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; }
.metrics-cell { display: flex; flex-wrap: wrap; gap: 4px; }
.metric-chip {
  display: inline-flex; align-items: center; gap: 2px;
  padding: 2px 8px; background: #F0F5FF; border-radius: 6px;
  font-size: 12px; color: #3B6FF5; white-space: nowrap;
}
.metric-chip strong { font-weight: 600; }
.metrics-editor { width: 100%; }
.metric-row { display: flex; align-items: center; margin-bottom: 8px; }
</style>
