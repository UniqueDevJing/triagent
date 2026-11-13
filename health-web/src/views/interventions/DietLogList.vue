<template>
  <div class="page-container">
    <div class="page-header">
      <h2>膳食日志</h2>
      <div class="header-actions">
        <el-select
          v-model="memberFilter"
          placeholder="筛选会员"
          clearable
          filterable
          remote
          :remote-method="searchMembers"
          style="width:180px;margin-right:12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="m in memberOptions"
            :key="m.id"
            :label="m.name"
            :value="m.id"
          />
        </el-select>
        <el-date-picker
          v-model="dateFilter"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          style="width:150px;margin-right:12px"
          @change="handleFilterChange"
        />
        <el-button type="primary" @click="openDialog()">新增日志</el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe v-loading="loading">
      <template #empty>
        <EmptyState title="暂无膳食日志" description="记录会员日常饮食，为健康干预提供数据支持" icon="Dish" action-text="新增日志" @action="openDialog()" />
      </template>
      <el-table-column label="会员" min-width="120">
        <template #default="{ row }">
          {{ getMemberName(row.memberId) }}
        </template>
      </el-table-column>
      <el-table-column label="餐食类型" width="100">
        <template #default="{ row }">
          <el-tag :type="mealTagType(row.mealType)" size="small">
            {{ mealLabel(row.mealType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="foodName" label="食物" min-width="200" show-overflow-tooltip />
      <el-table-column prop="calories" label="热量(kcal)" width="100" />
      <el-table-column prop="logDate" label="日期" width="120" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="page-pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetch"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="新增膳食日志" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="会员">
          <el-select
            v-model="form.memberId"
            placeholder="搜索并选择会员"
            filterable
            remote
            :remote-method="searchMembers"
            style="width:100%"
          >
            <el-option
              v-for="m in memberOptions"
              :key="m.id"
              :label="m.name"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="餐食类型">
          <el-select v-model="form.mealType" style="width:100%">
            <el-option label="早餐" value="BREAKFAST" />
            <el-option label="午餐" value="LUNCH" />
            <el-option label="晚餐" value="DINNER" />
            <el-option label="加餐" value="SNACK" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.logDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="食物">
          <el-input v-model="form.foodName" type="textarea" :rows="3" placeholder="请输入食物信息" />
        </el-form-item>
        <el-form-item label="热量">
          <el-input-number v-model="form.calories" :min="0" :max="99999" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/modules/interventionAdmin'
import { getMembers } from '@/api/modules/members'
import EmptyState from '@/components/EmptyState.vue'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({})
const page = ref(1)
const size = ref(20)
const total = ref(0)
const memberFilter = ref('')
const dateFilter = ref('')
const memberOptions = ref([])
const memberMap = ref({})

const mealTagMap = { BREAKFAST: 'success', LUNCH: 'warning', DINNER: '', SNACK: 'info' }
const mealLabelMap = { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }
function mealTagType(v) { return mealTagMap[v] || '' }
function mealLabel(v) { return mealLabelMap[v] || v }
function getMemberName(id) { return memberMap.value[id] || `ID:${id}` }

onMounted(async () => {
  await loadMembers()
  await fetch()
})

async function loadMembers(keyword) {
  try {
    const res = await getMembers({ page: 1, size: 200, keyword: keyword || '' })
    const records = res.data.records || []
    memberOptions.value = records
    records.forEach(m => { memberMap.value[m.id] = m.name })
  } catch { /* ignore */ }
}

async function searchMembers(keyword) {
  await loadMembers(keyword)
}

function handleFilterChange() {
  page.value = 1
  fetch()
}

async function fetch() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (memberFilter.value) params.memberId = memberFilter.value
    if (dateFilter.value) params.logDate = dateFilter.value
    const res = await api.getDietLogs(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function openDialog() {
  form.value = { memberId: '', mealType: 'BREAKFAST', logDate: '', foodName: '', calories: undefined }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    await api.createDietLog(form.value)
    dialogVisible.value = false
    ElMessage.success('保存成功')
    fetch()
  } catch { /* handled by interceptor */ }
  saving.value = false
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该膳食日志？', '提示', { type: 'warning' })
  await api.deleteDietLog(row.id)
  ElMessage.success('已删除')
  fetch()
}
</script>
