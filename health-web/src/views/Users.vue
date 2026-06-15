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
    <el-dialog v-model="detailVisible" title="健康档案" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="姓名">{{ currentUser?.name }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ currentUser?.age }} 岁</el-descriptions-item>
        <el-descriptions-item label="血型">{{ currentUser?.bloodType }}</el-descriptions-item>
        <el-descriptions-item label="身高">{{ currentUser?.height }} cm</el-descriptions-item>
        <el-descriptions-item label="体重">{{ currentUser?.weight }} kg</el-descriptions-item>
        <el-descriptions-item label="BMI">
          {{ calcBMI(currentUser?.height, currentUser?.weight) }}
        </el-descriptions-item>
        <el-descriptions-item label="过敏史">{{ currentUser?.allergies || '无' }}</el-descriptions-item>
        <el-descriptions-item label="既往病史">{{ currentUser?.medicalHistory || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { calcBMI as formatBMI } from '@/utils/format'

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

onMounted(fetchUsers)

async function fetchUsers() {
  loading.value = true
  try {
    const res = await request.get('/users', { params: { page: page.value, size: size.value, keyword: keyword.value } })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch {
    tableData.value = mockUsers()
    total.value = 4
  }
  loading.value = false
}

function showAddDialog() {
  editId.value = null
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
      await request.put(`/users/${editId.value}`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/users', form)
      ElMessage.success('添加成功')
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
    await request.delete(`/users/${id}`)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch {
    ElMessage.warning('后端未启动，操作已模拟')
  }
}

function viewDetail(row) {
  currentUser.value = row
  detailVisible.value = true
}

function calcBMI(h, w) {
  const result = formatBMI(h, w)
  if (!result) return '-'
  return `${result.value} (${result.label})`
}

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
</style>
