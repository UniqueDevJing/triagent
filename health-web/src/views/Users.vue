<template>
  <div class="users-page">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <p class="page-subtitle">管理系统中的健康用户档案信息</p>
    </div>
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <el-button type="primary" @click="showAddDialog">新增用户</el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索姓名/手机号" clearable style="width: 240px;" @keyup.enter="fetchUsers" />
        <el-button type="primary" @click="fetchUsers">搜索</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '未知' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="bloodType" label="血型" width="60" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" @click="viewDetail(row)">档案</el-button>
            <el-button size="small" type="danger" @click="deleteUser(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="fetchUsers"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '新增用户'" width="600px">
      <el-form ref="userFormRef" :model="form" :rules="userRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option :value="1" label="男" />
                <el-option :value="2" label="女" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年龄"><el-input-number v-model="form.age" :min="0" :max="120" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" style="width: 100%">
                <el-option v-for="t in ['A','B','AB','O']" :key="t" :value="t" :label="t" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="紧急联系人"><el-input v-model="form.emergencyContact" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系电话"><el-input v-model="form.emergencyPhone" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身高(cm)"><el-input-number v-model="form.height" :precision="1" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体重(kg)"><el-input-number v-model="form.weight" :precision="1" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 健康档案弹窗 -->
    <el-dialog v-model="detailVisible" title="健康档案" width="700px" @opened="fetchUserHealthData">
      <el-row :gutter="20">
        <el-col :span="11">
          <h4 class="section-title">基本信息</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="姓名">{{ currentUser?.name }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ currentUser?.age }} 岁</el-descriptions-item>
            <el-descriptions-item label="血型">{{ currentUser?.bloodType }}</el-descriptions-item>
            <el-descriptions-item label="身高">{{ currentUser?.height }} cm</el-descriptions-item>
            <el-descriptions-item label="体重">{{ currentUser?.weight }} kg</el-descriptions-item>
            <el-descriptions-item label="BMI">
              <el-tag :type="bmiTag">{{ calcBMI(currentUser?.height, currentUser?.weight) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="过敏史">{{ currentUser?.allergies || '无' }}</el-descriptions-item>
            <el-descriptions-item label="既往病史">{{ currentUser?.medicalHistory || '无' }}</el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :span="13">
          <h4 class="section-title">最新指标</h4>
          <el-descriptions v-if="latestMetrics && Object.keys(latestMetrics).length" :column="2" border size="small">
            <el-descriptions-item v-for="(v, k) in latestMetrics" :key="k" :label="k">
              {{ v }}
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无健康指标" :image-size="50" />
        </el-col>
      </el-row>

      <el-divider />

      <h4 class="section-title">健康档案历史</h4>
      <el-table :data="userRecords" stripe size="small" v-loading="recordsLoading" max-height="200">
        <el-table-column prop="recordDate" label="日期" width="110" />
        <el-table-column prop="type" label="类型" width="70" />
        <el-table-column label="指标" min-width="200">
          <template #default="{ row }">
            <div class="inline-metrics">
              <template v-for="(v, k) in parseMetrics(row.metrics)" :key="k">
                <span class="mini-chip">{{ k }}: {{ v }}</span>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />

      <h4 class="section-title">最近评估</h4>
      <el-table :data="userAssessments" stripe size="small" v-loading="assessLoading" max-height="200">
        <el-table-column prop="createdAt" label="时间" width="160" />
        <el-table-column prop="totalScore" label="得分" width="70" />
        <el-table-column prop="riskLevel" label="风险" width="80">
          <template #default="{ row }">
            <el-tag :type="riskType(row.riskLevel)" size="small">{{ riskLabel(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportText" label="评估报告" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, createUser, updateUser, deleteUser as deleteUserApi } from '@/api/modules/users'
import { getHealthRecordsByUser, getLatestHealthRecord } from '@/api/modules/healthRecords'
import { getRecords as getAssessments } from '@/api/modules/assessments'
import { calcBMI as formatBMI } from '@/utils/format'
import { useRealtime } from '@/composables/useRealtime'
import { useFormDraft } from '@/composables/useFormDraft'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editId = ref(null)
const currentUser = ref(null)
const userRecords = ref([])
const userAssessments = ref([])
const latestMetrics = ref(null)
const recordsLoading = ref(false)
const assessLoading = ref(false)

const userFormRef = ref(null)

const form = reactive({
  name: '', gender: 1, age: 0, phone: '', email: '', address: '',
  emergencyContact: '', emergencyPhone: '', bloodType: '', height: null, weight: null,
})

const userRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const { hasDraft, restoreDraft, clearDraft } = useFormDraft('user-form', { form })

onMounted(() => {
  fetchUsers()
  useRealtime('users', (eventName) => {
    if (eventName !== 'connected') fetchUsers()
  }).connect()
})

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUsers({ page: page.value, size: size.value, keyword: keyword.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch {
    tableData.value = mockUsers()
    total.value = 4
  }
  loading.value = false
}

async function showAddDialog() {
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
  Object.keys(form).forEach(k => form[k] = null)
  form.gender = 1; form.age = 0
  dialogVisible.value = true
  userFormRef.value?.resetFields()
}

function showEditDialog(row) {
  editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function saveUser() {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (editId.value) {
      await updateUser(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createUser(form)
      ElMessage.success('添加成功')
      clearDraft()
    }
    dialogVisible.value = false
    fetchUsers()
  } catch {
    ElMessage.warning('后端未启动，操作已模拟')
    dialogVisible.value = false
  }
}

async function deleteUser(id) {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
  try {
    await deleteUserApi(id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch {
    ElMessage.warning('后端未启动，操作已模拟')
  }
}

function viewDetail(row) {
  currentUser.value = row
  userRecords.value = []
  userAssessments.value = []
  latestMetrics.value = null
  detailVisible.value = true
}

async function fetchUserHealthData() {
  const uid = currentUser.value?.id
  if (!uid) return
  try {
    const [metricsRes, recordsRes, assessRes] = await Promise.all([
      getLatestHealthRecord(uid),
      getHealthRecordsByUser(uid),
      getAssessments({ userId: uid, page: 1, size: 5 }),
    ])
    latestMetrics.value = metricsRes.data || {}
    userRecords.value = (recordsRes.data || []).slice(0, 10)
    userAssessments.value = assessRes.data?.records || []
  } catch { /* backend may be offline */ }
}

function parseMetrics(metricsStr) {
  if (!metricsStr) return {}
  try { return typeof metricsStr === 'string' ? JSON.parse(metricsStr) : metricsStr }
  catch { return {} }
}

function riskType(level) {
  return { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }[level] || 'info'
}
function riskLabel(level) {
  return { LOW: '低', MEDIUM: '中', HIGH: '高' }[level] || level
}

function calcBMI(h, w) {
  const result = formatBMI(h, w)
  if (!result) return '-'
  return `${result.value} (${result.label})`
}

const bmiTag = computed(() => {
  const result = formatBMI(currentUser.value?.height, currentUser.value?.weight)
  if (!result) return 'info'
  const { value } = result
  if (value < 18.5) return 'danger'
  if (value < 24) return 'success'
  if (value < 28) return 'warning'
  return 'danger'
})

function mockUsers() {
  return [
    { id: 1, name: '张三', gender: 1, age: 65, phone: '13800138001', email: 'zhangsan@test.com', address: '北京市朝阳区', bloodType: 'A', height: 172.5, weight: 70.0 },
    { id: 2, name: '李四', gender: 2, age: 58, phone: '13800138002', email: 'lisi@test.com', address: '上海市浦东新区', bloodType: 'B', height: 160.0, weight: 55.0 },
    { id: 3, name: '王五', gender: 1, age: 72, phone: '13800138003', email: 'wangwu@test.com', address: '广州市天河区', bloodType: 'O', height: 168.0, weight: 68.0 },
    { id: 4, name: '赵六', gender: 2, age: 80, phone: '13800138004', email: 'zhaoliu@test.com', address: '深圳市南山区', bloodType: 'AB', height: 155.0, weight: 50.0 },
  ]
}
</script>

<style scoped>
.users-page {
  animation: fadeIn 0.4s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.content-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.section-title { color: #303133; font-size: 14px; font-weight: 600; margin: 0 0 10px 0; padding-bottom: 8px; border-bottom: 1px solid #F2F3F5; }
.inline-metrics { display: flex; flex-wrap: wrap; gap: 3px; }
.mini-chip { display: inline-block; padding: 1px 6px; background: #F0F5FF; border-radius: 4px; font-size: 11px; color: #3B6FF5; }
</style>
