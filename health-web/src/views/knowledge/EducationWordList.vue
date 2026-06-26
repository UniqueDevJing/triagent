<template>
  <div class="page-container">
    <div class="page-header">
      <h2>宣教词管理</h2>
      <el-button type="primary" @click="openDialog()">新增宣教词</el-button>
    </div>
    <div class="page-filters">
      <el-input v-model="keyword" placeholder="搜索术语或定义..." style="width:280px" clearable @change="fetch" />
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="term" label="术语" width="140" />
      <el-table-column prop="definition" label="定义解释" min-width="250" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="example" label="示例" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="size" @current-change="fetch" layout="prev,pager,next" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑宣教词' : '新增宣教词'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="术语" required><el-input v-model="form.term" /></el-form-item>
        <el-form-item label="定义"><el-input v-model="form.definition" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="示例"><el-input v-model="form.example" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/modules/knowledgeAdmin'

const tableData = ref([])
const loading = ref(false)
const page = ref(1), size = ref(20), total = ref(0)
const keyword = ref('')
const dialogVisible = ref(false), form = ref({})

onMounted(() => fetch())
async function fetch() {
  loading.value = true
  const res = await api.getEducationWords({ page: page.value, size: size.value, keyword: keyword.value })
  tableData.value = res.data.records; total.value = res.data.total; loading.value = false
}
function openDialog(row) { form.value = row ? { ...row } : {}; dialogVisible.value = true }
async function save() {
  if (form.value.id) await api.updateEducationWord(form.value.id, form.value)
  else await api.createEducationWord(form.value)
  dialogVisible.value = false; ElMessage.success('保存成功'); fetch()
}
async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await api.deleteEducationWord(row.id); ElMessage.success('已删除'); fetch()
}
</script>
