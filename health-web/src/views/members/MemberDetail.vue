<template>
  <div class="detail-container" v-loading="loading">
    <div class="detail-header">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回会员列表
      </el-button>
      <h2>会员详情</h2>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="info">
        <el-descriptions :column="2" border class="info-card" v-if="member">
          <el-descriptions-item label="姓名" :span="1">{{ member.name }}</el-descriptions-item>
          <el-descriptions-item label="性别" :span="1">{{ { 0: '未知', 1: '男', 2: '女' }[member.gender] || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="年龄" :span="1">{{ member.age }}</el-descriptions-item>
          <el-descriptions-item label="手机号" :span="1">{{ member.phone }}</el-descriptions-item>
          <el-descriptions-item label="身份证号" :span="2">{{ member.idCard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="紧急联系人" :span="1">{{ member.emergencyContact || '-' }}</el-descriptions-item>
          <el-descriptions-item label="紧急电话" :span="1">{{ member.emergencyPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="血型" :span="1">{{ member.bloodType ? member.bloodType + '型' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="身高/体重" :span="1">{{ member.height ? member.height + 'cm' : '-' }} / {{ member.weight ? member.weight + 'kg' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="既往病史" :span="2">{{ member.medicalHistory || '无' }}</el-descriptions-item>
          <el-descriptions-item label="过敏史" :span="2">{{ member.allergies || '无' }}</el-descriptions-item>
          <el-descriptions-item label="会员等级" :span="1">
            <el-tag :type="levelTagType(member.memberLevel)">{{ levelMap[member.memberLevel] || member.memberLevel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态" :span="1">
            <el-tag :type="member.status === 1 ? 'success' : 'info'">{{ member.status === 1 ? '启用' : '禁用' }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="体检计划" name="examPlans">
        <div class="tab-toolbar">
          <span class="tab-title">体检计划列表</span>
          <el-button type="primary" size="small" @click="showCreatePlan = true">新增计划</el-button>
        </div>
        <el-table :data="examPlans" v-loading="planLoading" stripe empty-text="暂无体检计划">
          <el-table-column prop="planName" label="计划名称" />
          <el-table-column prop="startDate" label="开始日期" width="120" />
          <el-table-column prop="endDate" label="结束日期" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="planStatusType(row.status)">{{ planStatusMap[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        </el-table>

        <el-dialog v-model="showCreatePlan" title="新增体检计划" width="500px">
          <el-form :model="planForm" label-width="90px">
            <el-form-item label="计划名称" required>
              <el-input v-model="planForm.planName" />
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker v-model="planForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker v-model="planForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="planForm.description" type="textarea" :rows="3" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showCreatePlan = false">取消</el-button>
            <el-button type="primary" @click="savePlan">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="评估记录" name="assessments">
        <div class="placeholder-content">
          <el-icon :size="48" color="#C0C4CC"><Folder /></el-icon>
          <p>评估功能开发中，敬请期待</p>
        </div>
      </el-tab-pane>

      <el-tab-pane label="干预方案" name="interventions">
        <div class="placeholder-content">
          <el-icon :size="48" color="#C0C4CC"><Folder /></el-icon>
          <p>干预功能开发中，敬请期待</p>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as memberApi from '@/api/modules/members'

const route = useRoute()
const router = useRouter()
const memberId = route.params.id

const activeTab = ref('info')
const loading = ref(false)
const member = ref(null)

const levelMap = { NORMAL: '普通会员', VIP: 'VIP', SVIP: 'SVIP' }
const levelTagType = (level) => ({ NORMAL: 'info', VIP: 'warning', SVIP: 'danger' }[level] || 'info')

const examPlans = ref([])
const planLoading = ref(false)
const showCreatePlan = ref(false)
const planForm = ref({})
const planStatusMap = { PENDING: '待开始', IN_PROGRESS: '进行中', COMPLETED: '已完成' }
const planStatusType = (s) => ({ PENDING: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success' }[s] || 'info')

onMounted(() => {
  fetchMember()
})

async function fetchMember() {
  loading.value = true
  const res = await memberApi.getMember(memberId)
  member.value = res.data
  loading.value = false
  fetchExamPlans()
}

async function fetchExamPlans() {
  planLoading.value = true
  const res = await memberApi.getExamPlans(memberId, { page: 1, size: 100 })
  examPlans.value = res.data.records || []
  planLoading.value = false
}

async function savePlan() {
  await memberApi.createExamPlan(memberId, planForm.value)
  showCreatePlan.value = false
  planForm.value = {}
  ElMessage.success('创建成功')
  fetchExamPlans()
}

function goBack() {
  router.push('/members/list')
}
</script>

<style scoped>
.detail-container {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.detail-header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}
.info-card {
  margin-top: 16px;
}
.tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.tab-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #909399;
}
.placeholder-content p {
  margin-top: 16px;
  font-size: 14px;
}
</style>
