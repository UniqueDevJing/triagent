<template>
  <div class="page-container">
    <div class="page-header">
      <h2>慢病管理</h2>
      <div class="header-actions">
        <el-select
          v-model="memberFilter"
          placeholder="筛选会员"
          clearable
          filterable
          remote
          :remote-method="searchMembers"
          style="width:200px;margin-right:12px"
          @change="handleMemberFilterChange"
        >
          <el-option
            v-for="m in memberOptions"
            :key="m.id"
            :label="m.name"
            :value="m.id"
          />
        </el-select>
        <el-button type="primary" @click="openDialog()">新增记录</el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column label="会员" min-width="120">
        <template #default="{ row }">
          {{ getMemberName(row.memberId) }}
        </template>
      </el-table-column>
      <el-table-column prop="diseaseName" label="疾病名称" min-width="120" />
      <el-table-column prop="severity" label="严重程度" width="100" />
      <el-table-column prop="diagnosisDate" label="确诊日期" width="120" />
      <el-table-column prop="controlStatus" label="控制状况" min-width="140" show-overflow-tooltip />
      <el-table-column prop="medication" label="用药" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <EmptyState title="暂无慢病记录" description="记录会员慢性病信息，制定个性化慢病管理方案" icon="Collection" action-text="新增记录" @action="openDialog()" />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑慢病记录' : '新增慢病记录'" width="560px">
      <el-form :model="form" label-width="100px">
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
        <el-form-item label="疾病名称">
          <el-select v-model="form.diseaseName" style="width:100%" filterable allow-create>
            <el-option label="高血压" value="高血压" />
            <el-option label="2型糖尿病" value="2型糖尿病" />
            <el-option label="冠心病" value="冠心病" />
            <el-option label="慢性阻塞性肺疾病" value="慢性阻塞性肺疾病" />
            <el-option label="高脂血症" value="高脂血症" />
            <el-option label="脑卒中" value="脑卒中" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="form.severity" style="width:100%">
            <el-option label="轻度" value="轻度" />
            <el-option label="中度" value="中度" />
            <el-option label="重度" value="重度" />
          </el-select>
        </el-form-item>
        <el-form-item label="确诊日期">
          <el-date-picker v-model="form.diagnosisDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="控制状况">
          <el-input v-model="form.controlStatus" placeholder="例如：血压控制良好，空腹血糖偏高" />
        </el-form-item>
        <el-form-item label="用药记录">
          <el-input v-model="form.medication" type="textarea" :rows="3" placeholder="请输入用药信息" />
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
const memberOptions = ref([])
const memberMap = ref({})

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

function handleMemberFilterChange(val) {
  page.value = 1
  fetch()
}

async function fetch() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (memberFilter.value) params.memberId = memberFilter.value
    const res = await api.getChronicDiseases(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function openDialog(row) {
  form.value = row ? { ...row } : { memberId: '', diseaseName: '', severity: '', diagnosisDate: '', controlStatus: '', medication: '' }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await api.updateChronicDisease(form.value.id, form.value)
    else await api.createChronicDisease(form.value)
    dialogVisible.value = false
    ElMessage.success('保存成功')
    fetch()
  } catch { /* handled by interceptor */ }
  saving.value = false
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm('确认删除该慢病记录？', '提示', { type: 'warning' }) } catch { return }
  try {
    await api.deleteChronicDisease(row.id); ElMessage.success('已删除'); fetch()
  } catch (e) { ElMessage.error(e?.message || '删除失败') }
}
</script>
