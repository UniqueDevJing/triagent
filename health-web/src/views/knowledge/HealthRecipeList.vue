<template>
  <div class="page-container">
    <div class="page-header">
      <h2>健康食谱库</h2>
      <el-button type="primary" @click="openDialog()">新增食谱</el-button>
    </div>
    <div class="page-filters">
      <el-input v-model="keyword" placeholder="搜索食谱名称..." style="width:200px" clearable @change="fetch" />
      <el-select v-model="catFilter" placeholder="食谱分类" clearable style="width:140px" @change="fetch">
        <el-option v-for="(v,k) in catMap" :key="k" :label="v" :value="k" />
      </el-select>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" width="140" />
      <el-table-column prop="category" label="分类" width="100">
        <template #default="{ row }"><el-tag size="small">{{ catMap[row.category] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="calories" label="热量(kcal)" width="100" />
      <el-table-column prop="cookingTime" label="烹饪时间" width="90">
        <template #default="{ row }">{{ row.cookingTime }}分钟</template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="80">
        <template #default="{ row }"><el-tag :type="row.difficulty==='HARD'?'danger':row.difficulty==='MEDIUM'?'warning':'success'" size="small">{{ diffMap[row.difficulty] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="suitableFor" label="适用人群" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" @click="viewDetail(row)">详情</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="size" @current-change="fetch" layout="prev,pager,next" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑食谱' : '新增食谱'" width="700px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类"><el-select v-model="form.category"><el-option v-for="(v,k) in catMap" :key="k" :label="v" :value="k" /></el-select></el-form-item>
        <el-form-item label="食材清单"><el-input v-model="form.ingredients" type="textarea" :rows="3" placeholder='[{"name":"鱼","amount":"1条"}]' /></el-form-item>
        <el-form-item label="制作步骤"><el-input v-model="form.steps" type="textarea" :rows="4" placeholder='[{"step":1,"description":"..."}]' /></el-form-item>
        <el-form-item label="总热量"><el-input-number v-model="form.calories" :min="0" /> kcal</el-form-item>
        <el-form-item label="营养信息"><el-input v-model="form.nutritionInfo" placeholder='{"protein":10,"fat":5,"carbs":20,"fiber":3}' /></el-form-item>
        <el-form-item label="适用人群"><el-input v-model="form.suitableFor" /></el-form-item>
        <el-form-item label="烹饪时间"><el-input-number v-model="form.cookingTime" :min="1" :max="300" /> 分钟</el-form-item>
        <el-form-item label="难度"><el-select v-model="form.difficulty"><el-option v-for="(v,k) in diffMap" :key="k" :label="v" :value="k" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="食谱详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ catMap[detail.category] }}</el-descriptions-item>
        <el-descriptions-item label="热量">{{ detail.calories }} kcal</el-descriptions-item>
        <el-descriptions-item label="难度">{{ diffMap[detail.difficulty] }}</el-descriptions-item>
        <el-descriptions-item label="烹饪时间">{{ detail.cookingTime }} 分钟</el-descriptions-item>
        <el-descriptions-item label="适用人群">{{ detail.suitableFor }}</el-descriptions-item>
        <el-descriptions-item label="食材清单" :span="2"><pre style="white-space:pre-wrap">{{ detail.ingredients }}</pre></el-descriptions-item>
        <el-descriptions-item label="制作步骤" :span="2"><pre style="white-space:pre-wrap">{{ detail.steps }}</pre></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/modules/knowledgeAdmin'

const catMap = { LOW_SALT: '低盐', LOW_SUGAR: '低糖', LOW_FAT: '低脂', HIGH_PROTEIN: '高蛋白', VEGETARIAN: '素食', GENERAL: '通用' }
const diffMap = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
const tableData = ref([])
const loading = ref(false)
const page = ref(1), size = ref(20), total = ref(0)
const keyword = ref(''), catFilter = ref('')
const dialogVisible = ref(false), form = ref({ category: 'GENERAL', difficulty: 'EASY' })
const detailVisible = ref(false), detail = ref({})

onMounted(() => fetch())
async function fetch() {
  loading.value = true
  const res = await api.getRecipes({ page: page.value, size: size.value, keyword: keyword.value, category: catFilter.value })
  tableData.value = res.data.records; total.value = res.data.total; loading.value = false
}
function openDialog(row) { form.value = row ? { ...row } : { category: 'GENERAL', difficulty: 'EASY' }; dialogVisible.value = true }
async function save() {
  if (form.value.id) await api.updateRecipe(form.value.id, form.value)
  else await api.createRecipe(form.value)
  dialogVisible.value = false; ElMessage.success('保存成功'); fetch()
}
function viewDetail(row) { detail.value = row; detailVisible.value = true }
async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await api.deleteRecipe(row.id); ElMessage.success('已删除'); fetch()
}
</script>
