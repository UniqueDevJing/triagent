<template>
  <div class="page-container">
    <div class="page-header">
      <h2>人群方案</h2>
      <div class="header-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索方案名称"
          clearable
          style="width:200px;margin-right:12px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="primary" @click="openDialog()">新增方案</el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe v-loading="loading">
      <template #empty>
        <EmptyState title="暂无人群方案" description="针对不同人群制定健康干预方案，实现精准管理" icon="Share" action-text="新增方案" @action="openDialog()" />
      </template>
      <el-table-column prop="programName" label="方案名称" min-width="160" />
      <el-table-column prop="targetCrowd" label="目标人群" min-width="160" show-overflow-tooltip />
      <el-table-column prop="programContent" label="方案内容" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 'ACTIVE'"
            :loading="row._switching"
            @change="(val) => handleStatusChange(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑方案' : '新增方案'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="方案名称">
          <el-input v-model="form.programName" placeholder="请输入方案名称" />
        </el-form-item>
        <el-form-item label="目标人群">
          <el-input v-model="form.targetCrowd" type="textarea" :rows="3" placeholder="例如：老年人、高血压患者" />
        </el-form-item>
        <el-form-item label="方案内容">
          <el-input v-model="form.programContent" type="textarea" :rows="5" placeholder="请输入方案内容" />
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
import EmptyState from '@/components/EmptyState.vue'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({})
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

onMounted(() => fetch())

async function fetch() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    const res = await api.getCrowdPrograms(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function handleSearch() {
  page.value = 1
  fetch()
}

function openDialog(row) {
  form.value = row ? { ...row } : { programName: '', targetCrowd: '', programContent: '' }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await api.updateCrowdProgram(form.value.id, form.value)
    else await api.createCrowdProgram(form.value)
    dialogVisible.value = false
    ElMessage.success('保存成功')
    fetch()
  } catch { /* handled by interceptor */ }
  saving.value = false
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该方案？', '提示', { type: 'warning' })
  await api.deleteCrowdProgram(row.id)
  ElMessage.success('已删除')
  fetch()
}

async function handleStatusChange(row, val) {
  row._switching = true
  try {
    await api.updateCrowdProgram(row.id, { status: val ? 'ACTIVE' : 'INACTIVE' })
    row.status = val ? 'ACTIVE' : 'INACTIVE'
    ElMessage.success(val ? '已激活' : '已停用')
  } catch { /* handled by interceptor */ }
  row._switching = false
}
</script>
