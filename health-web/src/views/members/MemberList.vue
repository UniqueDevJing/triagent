<template>
  <div class="page-container">
    <div class="page-header">
      <h2>会员管理</h2>
      <el-button type="primary" @click="openDialog()">新增会员</el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索姓名/手机号"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="60">
        <template #default="{ row }">
          {{ { 0: '未知', 1: '男', 2: '女' }[row.gender] || '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="60" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="idCard" label="身份证号" width="180" />
      <el-table-column prop="memberLevel" label="会员等级" width="100">
        <template #default="{ row }">
          <el-tag :type="levelType(row.memberLevel)">
            {{ levelMap[row.memberLevel] || row.memberLevel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button text size="small" @click="handleDetail(row)">详情</el-button>
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :total="total"
      :page-size="size"
      @current-change="fetch"
      layout="prev,pager,next"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑会员' : '新增会员'" width="700px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名" required>
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option :value="0" label="未知" />
                <el-option :value="1" label="男" />
                <el-option :value="2" label="女" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="0" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" required>
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="form.emergencyContact" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急电话">
              <el-input v-model="form.emergencyPhone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" style="width: 100%">
                <el-option value="A" label="A型" />
                <el-option value="B" label="B型" />
                <el-option value="AB" label="AB型" />
                <el-option value="O" label="O型" />
                <el-option value="" label="未知" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="身高(cm)">
              <el-input-number v-model="form.height" :min="0" :max="250" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体重(kg)">
              <el-input-number v-model="form.weight" :min="0" :max="500" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="既往病史">
          <el-input v-model="form.medicalHistory" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="过敏史">
          <el-input v-model="form.allergies" type="textarea" :rows="2" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会员等级">
              <el-select v-model="form.memberLevel" style="width: 100%">
                <el-option value="NORMAL" label="普通会员" />
                <el-option value="VIP" label="VIP" />
                <el-option value="SVIP" label="SVIP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as memberApi from '@/api/modules/members'

const router = useRouter()

const levelMap = { NORMAL: '普通会员', VIP: 'VIP', SVIP: 'SVIP' }
const levelType = (level) => {
  return { NORMAL: 'info', VIP: 'warning', SVIP: 'danger' }[level] || 'info'
}

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const dialogVisible = ref(false)
const form = ref({})

onMounted(() => { fetch() })

async function fetch() {
  loading.value = true
  const params = { page: page.value, size: size.value }
  if (keyword.value) params.keyword = keyword.value
  const res = await memberApi.getMembers(params)
  tableData.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

function handleSearch() {
  page.value = 1
  fetch()
}

function openDialog(row) {
  form.value = row ? { ...row } : { gender: 0, status: 1, memberLevel: 'NORMAL', bloodType: '' }
  dialogVisible.value = true
}

async function save() {
  if (form.value.id) {
    await memberApi.updateMember(form.value.id, form.value)
  } else {
    await memberApi.createMember(form.value)
  }
  dialogVisible.value = false
  ElMessage.success('保存成功')
  fetch()
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await memberApi.updateMember(row.id, { ...row, status: newStatus })
  row.status = newStatus
  ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
}

function handleDetail(row) {
  router.push(`/members/${row.id}`)
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该会员？', '提示', { type: 'warning' })
  await memberApi.deleteMember(row.id)
  ElMessage.success('已删除')
  fetch()
}
</script>

<style scoped>
.page-container {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
