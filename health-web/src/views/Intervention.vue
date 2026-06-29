<template>
  <div class="intervention-page">
    <div class="page-header">
      <h2 class="page-title">健康干预</h2>
      <p class="page-subtitle">制定和执行个性化健康干预计划</p>
    </div>

    <!-- 干预计划列表 -->
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <span>干预计划</span>
          <el-button type="primary" @click="showPlanDialog(null)">新建计划</el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8" v-for="plan in plans" :key="plan.id">
          <el-card shadow="hover" class="plan-card" @click="selectPlan(plan)">
            <div class="plan-header">
              <h4>{{ plan.title }}</h4>
              <el-tag :type="plan.status === 'ACTIVE' ? 'success' : plan.status === 'COMPLETED' ? 'info' : 'danger'" size="small">
                {{ plan.status === 'ACTIVE' ? '进行中' : plan.status === 'COMPLETED' ? '已完成' : '已取消' }}
              </el-tag>
            </div>
            <p class="plan-goal">{{ plan.goal }}</p>
            <div class="plan-meta">
              <span>{{ plan.startDate?.slice(0, 10) }} ~ {{ plan.endDate?.slice(0, 10) }}</span>
              <span style="margin-left: 8px;">创建人: {{ plan.createdBy || '管理员' }}</span>
            </div>
            <el-progress :percentage="planProgress(plan.id)" :stroke-width="6" style="margin-top: 12px;" />
          </el-card>
        </el-col>
        <el-col :span="8" v-if="plans.length === 0 && !loading">
          <el-empty description="暂无干预计划" />
        </el-col>
      </el-row>
    </el-card>

    <!-- 任务看板 -->
    <el-card v-if="selectedPlan" style="margin-top: 20px;" class="kanban-card">
      <template #header>
        <div class="card-header">
          <span>任务看板 - {{ selectedPlan.title }}</span>
          <el-button type="primary" size="small" @click="showTaskDialog(null)">添加任务</el-button>
        </div>
      </template>

      <div class="kanban" v-loading="tasksLoading">
        <div class="kanban-col">
          <div class="kanban-title pending">
            <span>待执行</span>
            <el-tag size="small" type="info" round>{{ pendingTasks.length }}</el-tag>
          </div>
          <el-card v-for="task in pendingTasks" :key="task.id" shadow="hover" class="task-card">
            <h5>{{ task.title }}</h5>
            <p>{{ task.description }}</p>
            <div class="task-footer">
              <el-tag size="small">截止: {{ task.dueDate?.slice(0, 10) }}</el-tag>
              <el-button size="small" type="primary" @click="moveTask(task, 'IN_PROGRESS')">开始</el-button>
            </div>
          </el-card>
        </div>
        <div class="kanban-col">
          <div class="kanban-title in-progress">
            <span>进行中</span>
            <el-tag size="small" type="primary" round>{{ inProgressTasks.length }}</el-tag>
          </div>
          <el-card v-for="task in inProgressTasks" :key="task.id" shadow="hover" class="task-card">
            <h5>{{ task.title }}</h5>
            <p>{{ task.description }}</p>
            <div class="task-footer">
              <el-tag size="small">截止: {{ task.dueDate?.slice(0, 10) }}</el-tag>
              <el-button size="small" type="success" @click="moveTask(task, 'COMPLETED')">完成</el-button>
            </div>
          </el-card>
        </div>
        <div class="kanban-col">
          <div class="kanban-title done">
            <span>已完成</span>
            <el-tag size="small" type="success" round>{{ doneTasks.length }}</el-tag>
          </div>
          <el-card v-for="task in doneTasks" :key="task.id" shadow="hover" class="task-card done-card">
            <h5>{{ task.title }}</h5>
            <p>{{ task.description }}</p>
            <div class="task-footer">
              <el-tag size="small" type="success">已完成</el-tag>
              <span class="done-time">{{ task.completedAt?.slice(0, 16) }}</span>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>

    <!-- 计划弹窗 -->
    <el-dialog v-model="planDialogVisible" :title="editPlanId ? '编辑计划' : '新建计划'" width="500px">
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="100px">
        <el-form-item label="计划标题" prop="title"><el-input v-model="planForm.title" placeholder="请输入计划标题" /></el-form-item>
        <el-form-item label="目标" prop="goal"><el-input v-model="planForm.goal" type="textarea" :rows="3" placeholder="请输入干预目标" /></el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="planForm.startDate" type="date" style="width: 100%" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="planForm.endDate" type="date" style="width: 100%" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePlan" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 任务弹窗 -->
    <el-dialog v-model="taskDialogVisible" title="添加任务" width="500px">
      <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-width="80px">
        <el-form-item label="任务标题" prop="title"><el-input v-model="taskForm.title" placeholder="请输入任务标题" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="taskForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="截止日期"><el-date-picker v-model="taskForm.dueDate" type="date" style="width: 100%" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTask" :loading="taskSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getPlans, getPlanTasks, createPlan, updatePlan, createTask, updateTaskStatus } from '@/api/modules/interventions'
import { useRealtime } from '@/composables/useRealtime'
import { useFormDraft } from '@/composables/useFormDraft'

const plans = ref([])
const tasks = ref([])
const loading = ref(false)
const tasksLoading = ref(false)
const saving = ref(false)
const taskSaving = ref(false)
const selectedPlan = ref(null)

const planDialogVisible = ref(false)
const taskDialogVisible = ref(false)
const planFormRef = ref(null)
const taskFormRef = ref(null)
const editPlanId = ref(null)
const planForm = reactive({ title: '', goal: '', startDate: '', endDate: '' })
const taskForm = reactive({ title: '', description: '', dueDate: '' })

const planRules = {
  title: [{ required: true, message: '请输入计划标题', trigger: 'blur' }],
  goal: [{ required: true, message: '请输入计划目标', trigger: 'blur' }],
}
const taskRules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
}

const { hasDraft: hasPlanDraft, restoreDraft: restorePlanDraft, clearDraft: clearPlanDraft } = useFormDraft('intervention-plan-form', { form: planForm })
const { hasDraft: hasTaskDraft, restoreDraft: restoreTaskDraft, clearDraft: clearTaskDraft } = useFormDraft('intervention-task-form', { form: taskForm })

onMounted(() => {
  fetchPlans()
  useRealtime('interventions', (eventName) => {
    if (eventName !== 'connected') {
      fetchPlans()
      if (selectedPlan.value) selectPlan(selectedPlan.value)
    }
  }).connect()
})

async function fetchPlans() {
  loading.value = true
  try {
    const res = await getPlans({ page: 1, size: 50 })
    plans.value = res.data.records || []
    // 加载所有计划的任务以计算正确的进度条
    if (plans.value.length > 0) {
      const taskResults = await Promise.allSettled(
        plans.value.map(p => getPlanTasks(p.id))
      )
      taskResults.forEach((result, i) => {
        if (result.status === 'fulfilled') {
          const data = Array.isArray(result.value.data) ? result.value.data : (result.value.data?.records || [])
          tasks.value.push(...data)
        }
      })
    }
  } catch { plans.value = [] }
  loading.value = false
}

async function selectPlan(plan) {
  selectedPlan.value = plan
  tasksLoading.value = true
  try {
    const res = await getPlanTasks(plan.id)
    const newTasks = Array.isArray(res.data) ? res.data : (res.data?.records || [])
    // 替换该计划的任务，避免重复
    tasks.value = [...tasks.value.filter(t => t.planId !== plan.id), ...newTasks]
  } catch { /* keep existing tasks */ }
  tasksLoading.value = false
}

function planTasks(planId) {
  return tasks.value.filter(t => t.planId === planId)
}

const pendingTasks = computed(() => selectedPlan.value ? planTasks(selectedPlan.value.id).filter(t => t.status === 'PENDING') : [])
const inProgressTasks = computed(() => selectedPlan.value ? planTasks(selectedPlan.value.id).filter(t => t.status === 'IN_PROGRESS') : [])
const doneTasks = computed(() => selectedPlan.value ? planTasks(selectedPlan.value.id).filter(t => t.status === 'COMPLETED') : [])

function planProgress(planId) {
  const all = planTasks(planId)
  if (all.length === 0) return 0
  return Math.round((all.filter(t => t.status === 'COMPLETED').length / all.length) * 100)
}

async function showPlanDialog(plan) {
  if (plan) {
    editPlanId.value = plan.id
    planForm.title = plan.title || ''
    planForm.goal = plan.goal || ''
    planForm.startDate = plan.startDate || ''
    planForm.endDate = plan.endDate || ''
  } else {
    editPlanId.value = null
    if (hasPlanDraft.value) {
      try {
        await ElMessageBox.confirm('检测到未保存的草稿，是否恢复？', '提示', {
          confirmButtonText: '恢复草稿',
          cancelButtonText: '重新填写',
          type: 'info',
        })
        restorePlanDraft()
        planDialogVisible.value = true
        return
      } catch {}
    }
    Object.keys(planForm).forEach(k => planForm[k] = '')
  }
  planDialogVisible.value = true
}

async function savePlan() {
  const valid = await planFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const auth = useAuthStore()
    const payload = {
      ...planForm,
      userId: auth.user?.userId || auth.user?.id || 1,
      createdBy: '管理员',
    }
    if (editPlanId.value) {
      await updatePlan(editPlanId.value, payload)
    } else {
      await createPlan(payload)
      clearPlanDraft()
    }
    ElMessage.success('保存成功')
    planDialogVisible.value = false
    fetchPlans()
  } catch { ElMessage.warning('操作失败，请检查后端服务') }
  saving.value = false
}

async function showTaskDialog() {
  if (hasTaskDraft.value) {
    try {
      await ElMessageBox.confirm('检测到未保存的草稿，是否恢复？', '提示', {
        confirmButtonText: '恢复草稿',
        cancelButtonText: '重新填写',
        type: 'info',
      })
      restoreTaskDraft()
      taskDialogVisible.value = true
      return
    } catch {}
  }
  Object.keys(taskForm).forEach(k => taskForm[k] = '')
  taskDialogVisible.value = true
}

async function saveTask() {
  const valid = await taskFormRef.value?.validate().catch(() => false)
  if (!valid) return
  taskSaving.value = true
  try {
    await createTask({
      ...taskForm,
      planId: selectedPlan.value.id,
    })
    clearTaskDraft()
    ElMessage.success('任务添加成功')
    taskDialogVisible.value = false
    selectPlan(selectedPlan.value)
  } catch { ElMessage.warning('操作失败') }
  taskSaving.value = false
}

async function moveTask(task, newStatus) {
  try {
    await updateTaskStatus(task.id, newStatus)
    task.status = newStatus
    if (newStatus === 'DONE') task.completedAt = new Date().toISOString()
    ElMessage.success('任务状态已更新')
  } catch { ElMessage.warning('更新失败') }
}
</script>

<style scoped>
.intervention-page { animation: fadeIn 0.4s ease; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.content-card, .kanban-card { border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.plan-card {
  cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px; margin-bottom: 12px; border: 1px solid #EBEEF5;
}
.plan-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); border-color: #5B8DEF; }
.plan-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.plan-goal { color: #606266; font-size: 13px; margin-bottom: 8px; }
.plan-meta { color: #909399; font-size: 12px; }
.kanban { display: flex; gap: 16px; overflow-x: auto; padding: 4px 0; }
.kanban-col { flex: 1; min-width: 280px; background: #F8FAFC; border-radius: 12px; padding: 16px; }
.kanban-title {
  padding: 10px 14px; border-radius: 10px; margin-bottom: 16px;
  font-weight: 600; font-size: 14px; display: flex; align-items: center; justify-content: space-between;
}
.kanban-title.pending { background: #F0F2F5; color: #606266; }
.kanban-title.in-progress { background: #ECF5FF; color: #409EFF; }
.kanban-title.done { background: #F0F9EB; color: #67C23A; }
.task-card { margin-bottom: 12px; border-radius: 10px; border: 1px solid #EBEEF5; transition: all 0.25s; }
.task-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.task-card h5 { margin-bottom: 6px; font-size: 14px; }
.task-card p { color: #909399; font-size: 12px; margin-bottom: 10px; }
.task-footer { display: flex; justify-content: space-between; align-items: center; }
.done-time { color: #909399; font-size: 12px; }
.done-card { opacity: 0.7; background: #FAFAFA; }
</style>
