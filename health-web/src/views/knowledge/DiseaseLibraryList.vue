<template>
  <div class="page-container">
    <div class="page-header">
      <h2>疾病知识库</h2>
      <el-button type="primary" @click="openDialog()">新增疾病</el-button>
    </div>
    <div class="page-filters">
      <el-input v-model="keyword" placeholder="搜索疾病名称..." style="width:200px" clearable @change="fetch" />
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="name" label="疾病名称" width="140" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
      <el-table-column prop="symptoms" label="症状" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" @click="viewDetail(row)">详情</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="size" @current-change="fetch" layout="prev,pager,next" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑疾病' : '新增疾病'" width="700px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="症状"><el-input v-model="form.symptoms" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="病因"><el-input v-model="form.causes" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="治疗原则"><el-input v-model="form.treatment" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="预防措施"><el-input v-model="form.prevention" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="危险因素"><el-input v-model="form.riskFactors" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="疾病详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.category }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ detail.description }}</el-descriptions-item>
        <el-descriptions-item label="症状" :span="2">{{ detail.symptoms }}</el-descriptions-item>
        <el-descriptions-item label="病因" :span="2">{{ detail.causes }}</el-descriptions-item>
        <el-descriptions-item label="治疗原则" :span="2">{{ detail.treatment }}</el-descriptions-item>
        <el-descriptions-item label="预防措施" :span="2">{{ detail.prevention }}</el-descriptions-item>
        <el-descriptions-item label="危险因素" :span="2">{{ detail.riskFactors }}</el-descriptions-item>
      </el-descriptions>
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
const detailVisible = ref(false), detail = ref({})

onMounted(() => fetch())
async function fetch() {
  loading.value = true
  const res = await api.getDiseases({ page: page.value, size: size.value, keyword: keyword.value })
  tableData.value = res.data.records; total.value = res.data.total; loading.value = false
}
function openDialog(row) { form.value = row ? { ...row } : {}; dialogVisible.value = true }
async function save() {
  if (form.value.id) await api.updateDisease(form.value.id, form.value)
  else await api.createDisease(form.value)
  dialogVisible.value = false; ElMessage.success('保存成功'); fetch()
}
function viewDetail(row) { detail.value = row; detailVisible.value = true }
async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await api.deleteDisease(row.id); ElMessage.success('已删除'); fetch()
}
</script>
