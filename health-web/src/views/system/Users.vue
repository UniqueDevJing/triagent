<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="openDialog()">新增用户</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'DOCTOR' ? 'warning' : 'info'">
            {{ roleMap[row.role] || row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" @click="handleResetPwd(row)">重置密码</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="size" @current-change="fetch" layout="prev,pager,next" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码" required><el-input v-model="form.password" type="password" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.role"><el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" /></el-select></el-form-item>
        <el-form-item label="科室"><el-select v-model="form.departmentId"><el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" /></el-select></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as sysApi from '@/api/modules/system'

const roleMap = { ADMIN: '管理员', DOCTOR: '医生', NURSE: '护士' }
const roles = [{ code: 'ADMIN', name: '管理员' }, { code: 'DOCTOR', name: '医生' }, { code: 'NURSE', name: '护士' }]
const departments = ref([])
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const form = ref({})

onMounted(() => { fetch(); loadDepartments() })

async function fetch() {
  loading.value = true
  const res = await sysApi.getUsers({ page: page.value, size: size.value })
  tableData.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

async function loadDepartments() {
  const res = await sysApi.getDepartments({ page: 1, size: 100 })
  departments.value = res.data.records
}

function openDialog(row) {
  form.value = row ? { ...row } : { role: 'NURSE', status: 1 }
  dialogVisible.value = true
}

async function save() {
  if (form.value.id) {
    await sysApi.updateUser(form.value.id, form.value)
  } else {
    await sysApi.createUser(form.value)
  }
  dialogVisible.value = false
  ElMessage.success('保存成功')
  fetch()
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await sysApi.updateUserStatus(row.id, newStatus)
  row.status = newStatus
  ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
}

async function handleResetPwd(row) {
  try { const pwd = '123456'; await sysApi.resetPassword(row.id, pwd); ElMessage.success(`已重置为 ${pwd}`) } catch {}
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await sysApi.deleteUser(row.id)
  ElMessage.success('已删除')
  fetch()
}
</script>
