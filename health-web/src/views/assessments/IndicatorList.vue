<template>
  <div class="page-container">
    <div class="page-header">
      <h2>评估指标</h2>
      <div class="header-actions">
        <el-select v-model="categoryFilter" placeholder="分类筛选" clearable style="width:140px;margin-right:12px" @change="fetch">
          <el-option label="血液" value="BLOOD" />
          <el-option label="尿液" value="URINE" />
          <el-option label="影像" value="IMAGING" />
          <el-option label="体格" value="PHYSICAL" />
        </el-select>
        <el-button type="primary" @click="openDialog()">新增指标</el-button>
      </div>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="indicatorName" label="指标名称" min-width="140" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column label="参考范围" width="160">
        <template #default="{ row }">
          {{ row.minValue ?? '-' }} ~ {{ row.maxValue ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="分类" width="90">
        <template #default="{ row }">
          <el-tag :type="categoryTagType(row.indicatorType)" size="small">{{ categoryLabel(row.indicatorType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <EmptyState title="暂无评估指标" description="配置健康评估指标及参考范围，为评估提供量化标准" icon="DataLine" action-text="新增指标" @action="openDialog()" />
      </template>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑指标' : '新增指标'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="指标名称"><el-input v-model="form.indicatorName" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="参考最小值"><el-input-number v-model="form.minValue" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="参考最大值"><el-input-number v-model="form.maxValue" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="form.riskLevel" style="width:100%">
            <el-option label="低" value="低" />
            <el-option label="中" value="中" />
            <el-option label="高" value="高" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.indicatorType" style="width:100%">
            <el-option label="血液" value="BLOOD" />
            <el-option label="尿液" value="URINE" />
            <el-option label="影像" value="IMAGING" />
            <el-option label="体格" value="PHYSICAL" />
          </el-select>
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
import * as api from '@/api/modules/assessmentAdmin'
import EmptyState from '@/components/EmptyState.vue'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({})
const page = ref(1)
const size = ref(20)
const total = ref(0)
const categoryFilter = ref('')

const categoryMap = { BLOOD: '血液', URINE: '尿液', IMAGING: '影像', PHYSICAL: '体格' }
const categoryTagMap = { BLOOD: '', URINE: 'success', IMAGING: 'warning', PHYSICAL: 'info' }
function categoryLabel(v) { return categoryMap[v] || v }
function categoryTagType(v) { return categoryTagMap[v] || '' }

onMounted(() => fetch())
async function fetch() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (categoryFilter.value) params.indicatorType = categoryFilter.value
    const res = await api.getIndicators(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function openDialog(row) {
  form.value = row ? { ...row } : { indicatorName: '', unit: '', indicatorType: 'BLOOD', minValue: undefined, maxValue: undefined, riskLevel: '低' }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await api.updateIndicator(form.value.id, form.value)
    else await api.createIndicator(form.value)
    dialogVisible.value = false
    ElMessage.success('保存成功')
    fetch()
  } catch { /* handled by interceptor */ }
  saving.value = false
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm('确认删除该指标？', '提示', { type: 'warning' }) } catch { return }
  try {
    await api.deleteIndicator(row.id); ElMessage.success('已删除'); fetch()
  } catch (e) { ElMessage.error(e?.message || '删除失败') }
}
</script>
