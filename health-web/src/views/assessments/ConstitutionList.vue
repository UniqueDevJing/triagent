<template>
  <div class="page-container">
    <div class="page-header">
      <h2>中医体质</h2>
      <el-button type="primary" @click="openDialog()">新增体质</el-button>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="name" label="名称" width="140" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="特征" min-width="180">
        <template #default="{ row }">
          <span v-if="row.characteristics && row.characteristics.length <= 20">{{ row.characteristics }}</span>
          <span v-else>
            {{ row.characteristics ? row.characteristics.slice(0, 20) + '...' : '-' }}
            <el-popover trigger="click" placement="bottom" :width="360">
              <template #reference>
                <el-button text size="small" type="primary" style="margin-left:4px">详情</el-button>
              </template>
              <div style="max-height:300px;overflow-y:auto;white-space:pre-wrap;line-height:1.6">{{ row.characteristics || '暂无' }}</div>
            </el-popover>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="调养建议" min-width="180">
        <template #default="{ row }">
          <span v-if="row.dietAdvice && row.dietAdvice.length <= 20">{{ row.dietAdvice }}</span>
          <span v-else>
            {{ row.dietAdvice ? row.dietAdvice.slice(0, 20) + '...' : '-' }}
            <el-popover trigger="click" placement="bottom" :width="360">
              <template #reference>
                <el-button text size="small" type="primary" style="margin-left:4px">详情</el-button>
              </template>
              <div style="max-height:300px;overflow-y:auto;white-space:pre-wrap;line-height:1.6">{{ row.dietAdvice || '暂无' }}</div>
            </el-popover>
          </span>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑体质' : '新增体质'" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="特征"><el-input v-model="form.characteristics" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="调养建议"><el-input v-model="form.dietAdvice" type="textarea" :rows="3" /></el-form-item>
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

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({})
const page = ref(1)
const size = ref(20)
const total = ref(0)

onMounted(() => fetch())
async function fetch() {
  loading.value = true
  try {
    const res = await api.getConstitutions({ page: page.value, size: size.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function openDialog(row) {
  form.value = row ? { ...row } : { name: '', description: '', characteristics: '', dietAdvice: '' }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await api.updateConstitution(form.value.id, form.value)
    else await api.createConstitution(form.value)
    dialogVisible.value = false
    ElMessage.success('保存成功')
    fetch()
  } catch { /* handled by interceptor */ }
  saving.value = false
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该体质？', '提示', { type: 'warning' })
  await api.deleteConstitution(row.id)
  ElMessage.success('已删除')
  fetch()
}
</script>
