<template>
  <div class="page-container">
    <div class="page-header">
      <h2>运动项目库</h2>
      <el-button type="primary" @click="openDialog()">新增运动项目</el-button>
    </div>
    <div class="page-filters">
      <el-input v-model="keyword" placeholder="搜索名称..." style="width:180px" clearable @change="fetch" />
      <el-select v-model="catFilter" placeholder="运动类型" clearable style="width:140px" @change="fetch">
        <el-option v-for="(v,k) in catMap" :key="k" :label="v" :value="k" />
      </el-select>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <template #empty>
        <EmptyState title="暂无运动项目" description="创建运动项目库，为会员提供科学的运动指导方案" icon="Timer" action-text="新增运动项目" @action="openDialog()" />
      </template>
      <el-table-column prop="exerciseName" label="名称" width="120" />
      <el-table-column prop="exerciseType" label="类型" width="90">
        <template #default="{ row }"><el-tag size="small">{{ catMap[row.exerciseType] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="caloriesBurn" label="千卡/时" width="80" />
      <el-table-column prop="difficulty" label="难度" width="80">
        <template #default="{ row }"><el-tag :type="row.difficulty==='HIGH'?'danger':row.difficulty==='MEDIUM'?'warning':'success'" size="small">{{ intMap[row.difficulty] }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="size" @current-change="fetch" layout="prev,pager,next" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑运动项目' : '新增运动项目'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required><el-input v-model="form.exerciseName" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.exerciseType"><el-option v-for="(v,k) in catMap" :key="k" :label="v" :value="k" /></el-select></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="千卡/小时"><el-input-number v-model="form.caloriesBurn" :min="0" :max="2000" /></el-form-item>
        <el-form-item label="难度"><el-select v-model="form.difficulty"><el-option v-for="(v,k) in intMap" :key="k" :label="v" :value="k" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/modules/knowledgeAdmin'
import EmptyState from '@/components/EmptyState.vue'

const catMap = { AEROBIC: '有氧运动', STRENGTH: '力量训练', FLEXIBILITY: '柔韧性', BALANCE: '平衡训练', TRADITIONAL: '传统养生', CORE: '核心训练' }
const intMap = { EASY: '低强度', MEDIUM: '中等强度', HARD: '高强度' }
const tableData = ref([])
const loading = ref(false)
const page = ref(1), size = ref(20), total = ref(0)
const keyword = ref(''), catFilter = ref('')
const dialogVisible = ref(false), form = ref({ category: 'AEROBIC', difficulty: 'EASY' })

onMounted(() => fetch())
async function fetch() {
  loading.value = true
  try {
    const res = await api.getExercises({ page: page.value, size: size.value, keyword: keyword.value, category: catFilter.value })
    tableData.value = res.data.records; total.value = res.data.total
  } catch (_) { tableData.value = []; total.value = 0 }
  loading.value = false
}
function openDialog(row) { form.value = row ? { ...row } : { exerciseType: 'AEROBIC', difficulty: 'EASY' }; dialogVisible.value = true }
async function save() {
  if (form.value.id) await api.updateExercise(form.value.id, form.value)
  else await api.createExercise(form.value)
  dialogVisible.value = false; ElMessage.success('保存成功'); fetch()
}
async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await api.deleteExercise(row.id); ElMessage.success('已删除'); fetch()
}
</script>
