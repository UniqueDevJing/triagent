<template>
  <div class="page-container">
    <div class="page-header">
      <h2>评估记录</h2>
      <div class="header-actions">
        <el-select v-model="typeFilter" placeholder="评估类型" clearable style="width:140px;margin-right:12px" @change="search">
          <el-option label="风险评估" value="RISK" />
          <el-option label="中医体质" value="TCM" />
          <el-option label="心理评测" value="PSYCHOLOGY" />
        </el-select>
        <el-input v-model="memberSearch" placeholder="搜索会员名" clearable style="width:180px;margin-right:12px" @clear="search" @keyup.enter="search" />
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button type="primary" @click="openDialog()">新增记录</el-button>
      </div>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="memberName" label="会员名" width="120" />
      <el-table-column label="评估类型" width="120">
        <template #default="{ row }">
          <el-tag :type="typeTagType(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalScore" label="分数" width="80" />
      <el-table-column label="风险等级" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.riskLevel" :type="riskTagType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="assessDate" label="评估日期" width="120" />
      <el-table-column prop="conclusion" label="结论" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <EmptyState
          title="暂无评估记录"
          description="完成体检后可为会员生成健康评估报告"
          icon="DataAnalysis"
          action-text="发起评估"
          @action="openDialog()"
        />
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

    <el-dialog v-model="dialogVisible" title="新增评估记录" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="会员">
          <el-select v-model="form.memberId" filterable remote :remote-method="searchMember" :loading="memberLoading" placeholder="搜索并选择会员" style="width:100%">
            <el-option v-for="m in memberOptions" :key="m.id" :label="m.realName || m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="风险评估" value="RISK" />
            <el-option label="中医体质" value="TCM" />
            <el-option label="心理评测" value="PSYCHOLOGY" />
          </el-select>
        </el-form-item>
        <el-form-item label="分数"><el-input-number v-model="form.totalScore" :min="0" :max="999" style="width:100%" /></el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="form.riskLevel" clearable style="width:100%">
            <el-option label="低风险" value="低风险" />
            <el-option label="中风险" value="中风险" />
            <el-option label="高风险" value="高风险" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估日期"><el-date-picker v-model="form.assessDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="结论"><el-input v-model="form.conclusion" type="textarea" :rows="3" /></el-form-item>
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
const typeFilter = ref('')
const memberSearch = ref('')
const memberOptions = ref([])
const memberLoading = ref(false)

const typeMap = { RISK: '风险评估', TCM: '中医体质', PSYCHOLOGY: '心理评测' }
const typeTagMap = { RISK: '', TCM: 'success', PSYCHOLOGY: 'warning' }
const riskTagMap = { '低风险': 'success', '中风险': 'warning', '高风险': 'danger' }
function typeLabel(v) { return typeMap[v] || v }
function typeTagType(v) { return typeTagMap[v] || '' }
function riskTagType(v) { return riskTagMap[v] || '' }

onMounted(() => fetch())
async function fetch() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (typeFilter.value) params.type = typeFilter.value
    if (memberSearch.value) params.memberId = memberSearch.value
    const res = await api.getAssessmentRecords(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function search() { page.value = 1; fetch() }

async function searchMember(query) {
  if (!query) { memberOptions.value = []; return }
  memberLoading.value = true
  try {
    const res = await getMembers({ page: 1, size: 20, keyword: query })
    memberOptions.value = res.data.records || []
  } catch { memberOptions.value = [] }
  memberLoading.value = false
}

function openDialog() {
  form.value = { memberId: '', type: 'RISK', totalScore: undefined, riskLevel: '', assessDate: '', conclusion: '' }
  dialogVisible.value = true
  memberOptions.value = []
}

async function save() {
  saving.value = true
  try {
    await api.createAssessmentRecord(form.value)
    dialogVisible.value = false
    ElMessage.success('创建成功')
    fetch()
  } catch { /* handled by interceptor */ }
  saving.value = false
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm('确认删除该记录？', '提示', { type: 'warning' }) } catch { return }
  try {
    await api.deleteAssessmentRecord(row.id); ElMessage.success('已删除'); fetch()
  } catch (e) { ElMessage.error(e?.message || '删除失败') }
}
</script>
