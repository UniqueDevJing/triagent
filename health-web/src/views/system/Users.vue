<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="openDialog()">新增用户</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="userName" label="用户名" />
      <el-table-column prop="nickName" label="姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : row.role === 'doctor' ? 'warning' : 'info'">
            {{ row.roleName || roleMap[row.role] || row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phoneNumber" label="手机号" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === '0'" :loading="row._statusLoading" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" @click="handleResetPwd(row)">重置密码</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无用户数据" /></template>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="size" @current-change="fetch" layout="prev,pager,next" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required><el-input v-model="form.userName" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.nickName" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码" required><el-input v-model="form.password" type="password" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.role"><el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" /></el-select></el-form-item>
        <el-form-item label="科室"><el-select v-model="form.deptId"><el-option v-for="d in departments" :key="d.id" :label="d.deptName || d.name" :value="d.id" /></el-select></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phoneNumber" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save" :loading="saving">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as sysApi from '@/api/modules/system'

const roleMap = { admin: '管理员', doctor: '医生', nurse: '护士' }
const roles = [{ code: 'admin', name: '管理员' }, { code: 'doctor', name: '医生' }, { code: 'nurse', name: '护士' }]
const departments = ref([])
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const form = ref({})
const saving = ref(false)

onMounted(() => { fetch(); loadDepartments() })

async function fetch() {
  loading.value = true
  try {
    const res = await sysApi.getUsers({ page: page.value, size: size.value })
    tableData.value = res.data.records; total.value = res.data.total
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

async function loadDepartments() {
  try {
    const res = await sysApi.getDepartments({ page: 1, size: 100 })
    departments.value = res.data.records
  } catch { departments.value = [] }
}

function openDialog(row) {
  form.value = row ? { ...row } : { role: 'nurse', status: '0' }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await sysApi.updateUser(form.value.id, form.value)
    else await sysApi.createUser(form.value)
    dialogVisible.value = false; ElMessage.success('保存成功'); fetch()
  } catch (e) { ElMessage.error(e?.message || '保存失败') }
  saving.value = false
}

async function toggleStatus(row) {
  const oldStatus = row.status
  const newStatus = row.status === '0' ? '1' : '0'
  row._statusLoading = true
  try {
    await sysApi.updateUserStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === '0' ? '已启用' : '已禁用')
  } catch (e) {
    row.status = oldStatus
    ElMessage.error(e?.message || '状态切换失败')
  }
  row._statusLoading = false
}

async function handleResetPwd(row) {
  try { const pwd = '123456'; await sysApi.resetPassword(row.id, pwd); ElMessage.success(`已重置为 ${pwd}`) } catch {}
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }) } catch { return }
  try {
    await sysApi.deleteUser(row.id); ElMessage.success('已删除'); fetch()
  } catch (e) { ElMessage.error(e?.message || '删除失败') }
}
</script>
