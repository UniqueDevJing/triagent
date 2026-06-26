<template>
  <div class="page-container">
    <div class="page-header">
      <h2>新增预约</h2>
    </div>

    <el-steps :active="step" align-center style="margin-bottom: 32px">
      <el-step title="选择会员" />
      <el-step title="选择套餐" />
      <el-step title="确认信息" />
    </el-steps>

    <!-- Step 1: 选择会员 -->
    <div v-show="step === 0">
      <div class="search-bar">
        <el-input
          v-model="memberKeyword" placeholder="搜索姓名/手机号" clearable style="width: 260px"
          @keyup.enter="searchMember" @clear="searchMember"
        />
        <el-button type="primary" @click="searchMember">搜索</el-button>
      </div>
      <el-table
        :data="members" v-loading="memberLoading" stripe
        highlight-current-row
        @current-change="handleMemberSelect"
      >
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60">
          <template #default="{ row }">
            {{ { 0: '未知', 1: '男', 2: '女' }[row.gender] || '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="idCard" label="身份证号" />
      </el-table>
      <el-pagination
        v-model:current-page="memberPage" :total="memberTotal" :page-size="memberSize"
        @current-change="fetchMembers" layout="prev,pager,next"
      />
      <div style="margin-top: 16px; text-align: right">
        <el-button type="primary" :disabled="!selectedMember" @click="step = 1">下一步</el-button>
      </div>
    </div>

    <!-- Step 2: 选择套餐 -->
    <div v-show="step === 1">
      <div class="search-bar">
        <el-input
          v-model="pkgKeyword" placeholder="搜索套餐" clearable style="width: 260px"
          @keyup.enter="searchPackage" @clear="searchPackage"
        />
        <el-button type="primary" @click="searchPackage">搜索</el-button>
      </div>
      <div class="package-grid">
        <div
          v-for="pkg in packages" :key="pkg.id"
          class="package-card"
          :class="{ active: selectedPackage?.id === pkg.id }"
          @click="selectedPackage = pkg"
        >
          <div class="pkg-name">{{ pkg.name }}</div>
          <div class="pkg-desc">{{ pkg.description || '暂无描述' }}</div>
          <div class="pkg-price">{{ formatPrice(pkg.price) }}</div>
        </div>
      </div>
      <el-pagination
        v-model:current-page="pkgPage" :total="pkgTotal" :page-size="pkgSize"
        @current-change="fetchPackages" layout="prev,pager,next"
      />
      <div style="margin-top: 16px; display: flex; justify-content: space-between">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" :disabled="!selectedPackage" @click="step = 2">下一步</el-button>
      </div>
    </div>

    <!-- Step 3: 确认信息 -->
    <div v-show="step === 2">
      <div class="confirm-section">
        <div class="confirm-card">
          <h4>会员信息</h4>
          <div class="info-row"><span class="label">姓名：</span>{{ selectedMember?.name }}</div>
          <div class="info-row"><span class="label">手机号：</span>{{ selectedMember?.phone }}</div>
          <div class="info-row"><span class="label">性别：</span>{{ { 0: '未知', 1: '男', 2: '女' }[selectedMember?.gender] || '未知' }}</div>
        </div>
        <div class="confirm-card">
          <h4>套餐信息</h4>
          <div class="info-row"><span class="label">套餐：</span>{{ selectedPackage?.name }}</div>
          <div class="info-row"><span class="label">价格：</span>{{ formatPrice(selectedPackage?.price) }}</div>
          <div class="info-row"><span class="label">描述：</span>{{ selectedPackage?.description || '无' }}</div>
        </div>
        <el-form label-width="100px" style="margin-top: 24px">
          <el-form-item label="预约日期" required>
            <el-date-picker v-model="form.date" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="时间段" required>
            <el-select v-model="form.timeSlot" placeholder="选择时间段" style="width: 100%">
              <el-option value="MORNING" label="上午" />
              <el-option value="AFTERNOON" label="下午" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.notes" type="textarea" :rows="3" placeholder="选填备注信息" />
          </el-form-item>
        </el-form>
      </div>
      <div style="margin-top: 16px; display: flex; justify-content: space-between">
        <el-button @click="step = 1">上一步</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMembers } from '@/api/modules/members'
import * as packageApi from '@/api/modules/packages'
import * as appointmentApi from '@/api/modules/appointments'

const router = useRouter()
const step = ref(0)

// Members
const members = ref([])
const memberLoading = ref(false)
const memberPage = ref(1)
const memberSize = ref(20)
const memberTotal = ref(0)
const memberKeyword = ref('')
const selectedMember = ref(null)

// Packages
const packages = ref([])
const pkgLoading = ref(false)
const pkgPage = ref(1)
const pkgSize = ref(20)
const pkgTotal = ref(0)
const pkgKeyword = ref('')
const selectedPackage = ref(null)

// Form
const form = ref({
  date: '',
  timeSlot: '',
  notes: '',
})
const submitting = ref(false)

onMounted(() => {
  fetchMembers()
  fetchPackages()
})

function formatPrice(val) {
  if (val == null) return '¥0.00'
  return '¥' + Number(val).toFixed(2)
}

async function fetchMembers() {
  memberLoading.value = true
  const params = { page: memberPage.value, size: memberSize.value }
  if (memberKeyword.value) params.keyword = memberKeyword.value
  const res = await getMembers(params)
  members.value = res.data.records
  memberTotal.value = res.data.total
  memberLoading.value = false
}

function searchMember() {
  memberPage.value = 1
  fetchMembers()
}

function handleMemberSelect(row) {
  selectedMember.value = row
}

async function fetchPackages() {
  pkgLoading.value = true
  const params = { page: pkgPage.value, size: pkgSize.value }
  if (pkgKeyword.value) params.keyword = pkgKeyword.value
  const res = await packageApi.getPackages(params)
  packages.value = res.data.records
  pkgTotal.value = res.data.total
  pkgLoading.value = false
}

function searchPackage() {
  pkgPage.value = 1
  fetchPackages()
}

async function handleSubmit() {
  if (!form.value.date || !form.value.timeSlot) {
    ElMessage.warning('请选择预约日期和时间段')
    return
  }
  submitting.value = true
  try {
    await appointmentApi.createAppointment({
      memberId: selectedMember.value.id,
      packageId: selectedPackage.value.id,
      appointmentDate: form.value.date,
      timeSlot: form.value.timeSlot,
      notes: form.value.notes || '',
    })
    ElMessage.success('预约创建成功')
    router.push('/appointments/list')
  } catch (e) {
    submitting.value = false
    ElMessage.error('创建预约失败: ' + (e.message || '请重试'))
  }
}
</script>

<style scoped>
.page-container {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}
.page-header {
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

/* 套餐卡片网格 */
.package-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.package-card {
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s;
}
.package-card:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}
.package-card.active {
  border-color: #409EFF;
  background: #F0F9FF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}
.pkg-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.pkg-desc {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pkg-price {
  font-size: 20px;
  font-weight: 700;
  color: #F56C6C;
}

/* 确认信息 */
.confirm-section {
  max-width: 600px;
  margin: 0 auto;
}
.confirm-card {
  background: #F9FAFC;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}
.confirm-card h4 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #303133;
}
.info-row {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}
.info-row .label {
  color: #909399;
  display: inline-block;
  width: 70px;
}
</style>
