<template>
  <div class="page-container">
    <div class="page-header">
      <h2>科室管理</h2>
      <el-button type="primary" @click="openDialog()">新增科室</el-button>
    </div>
    <el-table :data="tableData" stripe>
      <el-table-column prop="name" label="科室名称" />
      <el-table-column prop="code" label="编码" />
      <el-table-column prop="sortOrder" label="排序" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑科室' : '新增科室'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as sysApi from '@/api/modules/system'

const tableData = ref([])
const dialogVisible = ref(false)
const form = ref({})

onMounted(() => fetch())
async function fetch() { const res = await sysApi.getDepartments({ page: 1, size: 100 }); tableData.value = res.data.records }
function openDialog(row) { form.value = row ? { ...row } : { sortOrder: 0 }; dialogVisible.value = true }
async function save() {
  if (form.value.id) await sysApi.updateDepartment(form.value.id, form.value)
  else await sysApi.createDepartment(form.value)
  dialogVisible.value = false; ElMessage.success('保存成功'); fetch()
}
async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await sysApi.deleteDepartment(row.id); ElMessage.success('已删除'); fetch()
}
</script>
