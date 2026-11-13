<template>
  <div class="page-container">
    <div class="page-header">
      <h2>预约管理</h2>
      <div class="header-right">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="table">表格</el-radio-button>
          <el-radio-button value="calendar">日历</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="$router.push('/appointments/create')">新建预约</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="search-bar">
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="handleFilter">
        <el-option value="PENDING" label="待确认" />
        <el-option value="CONFIRMED" label="已确认" />
        <el-option value="DONE" label="已完成" />
        <el-option value="CANCELLED" label="已取消" />
      </el-select>
      <el-input
        v-model="keyword" placeholder="搜索会员名" clearable style="width: 200px"
        @keyup.enter="handleFilter" @clear="handleFilter"
      />
      <el-button type="primary" @click="handleFilter">搜索</el-button>
    </div>

    <!-- 表格视图 -->
    <template v-if="viewMode === 'table'">
      <EmptyState
        v-if="tableData.length === 0 && !loading"
        title="暂无预约记录"
        description="为会员安排体检预约，开启健康评估流程"
        icon="Calendar"
        action-text="新建预约"
        @action="$router.push('/appointments/create')"
      />
      <template v-else>
      <el-table :data="tableData" v-loading="loading" stripe @row-click="openDetail">
        <el-table-column prop="memberName" label="会员姓名" width="120" />
        <el-table-column prop="packageName" label="套餐名称" show-overflow-tooltip />
        <el-table-column prop="appointmentDate" label="预约日期" width="120" />
        <el-table-column prop="timeSlot" label="时间段" width="80">
          <template #default="{ row }">
            {{ row.timeSlot === 'MORNING' ? '上午' : '下午' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="notes" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'" text size="small" type="primary"
              @click.stop="handleStatusChange(row, 'CONFIRMED')"
            >确认</el-button>
            <el-button
              v-if="row.status === 'CONFIRMED'" text size="small" type="success"
              @click.stop="handleStatusChange(row, 'DONE')"
            >完成</el-button>
            <el-button
              v-if="row.status !== 'CANCELLED' && row.status !== 'DONE'" text size="small"
              @click.stop="handleStatusChange(row, 'CANCELLED')"
            >取消</el-button>
            <el-button
              text size="small" type="danger"
              @click.stop="handleDelete(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page" :total="total" :page-size="size"
        @current-change="fetch" layout="prev,pager,next" style="margin-top: 16px"
      />
      </template>
    </template>

    <!-- 日历视图 -->
    <template v-if="viewMode === 'calendar'">
      <div class="calendar-header">
        <el-button text @click="prevMonth">&lt; 上月</el-button>
        <span class="calendar-title">{{ calendarTitle }}</span>
        <el-button text @click="nextMonth">下月 &gt;</el-button>
        <el-button text size="small" @click="todayMonth" style="margin-left: 8px">今天</el-button>
      </div>
      <table class="calendar-table">
        <thead>
          <tr>
            <th v-for="d in weekDays" :key="d">{{ d }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(week, wi) in calendarGrid" :key="wi">
            <td
              v-for="(day, di) in week" :key="di"
              class="calendar-cell"
              :class="{
                'is-today': day.isToday,
                'is-other': !day.inMonth,
                'has-appointment': day.count > 0,
              }"
              @click="day.inMonth && clickDay(day)"
            >
              <div class="cell-top">
                <span class="cell-date">{{ day.date }}</span>
              </div>
              <div v-if="day.count > 0" class="cell-count">{{ day.count }} 个预约</div>
            </td>
          </tr>
        </tbody>
      </table>
    </template>

    <!-- 预约详情弹窗 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="550px">
      <template v-if="detailData">
        <div class="detail-section">
          <h4>会员信息</h4>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="姓名">{{ detailData.memberName }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ detailData.memberPhone || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="detail-section">
          <h4>套餐信息</h4>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="套餐名称">{{ detailData.packageName }}</el-descriptions-item>
            <el-descriptions-item label="价格">{{ detailData.packagePrice != null ? '¥' + Number(detailData.packagePrice).toFixed(2) : '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="detail-section">
          <h4>预约信息</h4>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="预约日期">{{ detailData.appointmentDate }}</el-descriptions-item>
            <el-descriptions-item label="时间段">{{ detailData.timeSlot === 'MORNING' ? '上午' : '下午' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType(detailData.status)" size="small">{{ statusLabel(detailData.status) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-if="detailData.notes" class="detail-section">
          <h4>备注</h4>
          <p class="detail-notes">{{ detailData.notes }}</p>
        </div>
        <div class="detail-actions" v-if="detailData.status !== 'CANCELLED' && detailData.status !== 'DONE'">
          <el-button
            v-if="detailData.status === 'PENDING'" type="primary"
            @click="handleDetailStatusChange('CONFIRMED')"
          >确认预约</el-button>
          <el-button
            v-if="detailData.status === 'CONFIRMED'" type="success"
            @click="handleDetailStatusChange('DONE')"
          >标记完成</el-button>
          <el-button @click="handleDetailStatusChange('CANCELLED')">取消预约</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as appointmentApi from '@/api/modules/appointments'
import EmptyState from '@/components/EmptyState.vue'
import dayjs from 'dayjs'

const viewMode = ref('table')

// 表格数据
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref('')

// 日历数据
const calendarDate = ref(dayjs())
const filteredAppointments = ref([])
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref(null)

const calendarTitle = computed(() => {
  return calendarDate.value.format('YYYY年M月')
})

const calendarGrid = computed(() => {
  const year = calendarDate.value.year()
  const month = calendarDate.value.month()
  const firstDay = dayjs(new Date(year, month, 1))
  const daysInMonth = firstDay.daysInMonth()
  const startDayOfWeek = firstDay.day()

  // last month padding
  const prevMonthDays = dayjs(new Date(year, month, 0)).date()

  const grid = []
  let week = []

  // fill previous month days
  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    const d = prevMonthDays - i
    const dateStr = dayjs(new Date(year, month - 1, d)).format('YYYY-MM-DD')
    week.push({ date: d, inMonth: false, count: 0, isToday: false, dateStr })
  }

  // current month days
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = dayjs(new Date(year, month, d)).format('YYYY-MM-DD')
    const count = filteredAppointments.value.filter(a => a.appointmentDate === dateStr).length
    week.push({
      date: d,
      inMonth: true,
      count,
      isToday: dayjs().format('YYYY-MM-DD') === dateStr,
      dateStr,
    })
    if (week.length === 7) {
      grid.push(week)
      week = []
    }
  }

  // fill remaining days
  if (week.length > 0) {
    let nextD = 1
    while (week.length < 7) {
      const dateStr = dayjs(new Date(year, month + 1, nextD)).format('YYYY-MM-DD')
      week.push({ date: nextD, inMonth: false, count: 0, isToday: false, dateStr })
      nextD++
    }
    grid.push(week)
  }

  return grid
})

onMounted(() => { fetch(); fetchAllForCalendar() })

// 切换标签页时刷新日历数据
watch(viewMode, (val) => {
  if (val === 'calendar') fetchAllForCalendar()
})

async function fetch() {
  loading.value = true
  const params = { page: page.value, size: size.value }
  if (keyword.value) params.keyword = keyword.value
  if (statusFilter.value) params.status = statusFilter.value
  const res = await appointmentApi.getAppointments(params)
  tableData.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

async function fetchAllForCalendar() {
  const params = { page: 1, size: 999 }
  if (statusFilter.value) params.status = statusFilter.value
  try {
    const res = await appointmentApi.getAppointments(params)
    filteredAppointments.value = res.data.records || []
  } catch {}
}

function handleFilter() {
  page.value = 1
  fetch()
  if (viewMode.value === 'calendar') fetchAllForCalendar()
}

function prevMonth() {
  calendarDate.value = calendarDate.value.subtract(1, 'month')
}

function nextMonth() {
  calendarDate.value = calendarDate.value.add(1, 'month')
}

function todayMonth() {
  calendarDate.value = dayjs()
}

function clickDay(day) {
  viewMode.value = 'table'
  keyword.value = ''
  statusFilter.value = ''
  page.value = 1
  fetchByDate(day.dateStr)
}

async function fetchByDate(dateStr) {
  loading.value = true
  try {
    // 客户端过滤：加载当月全部预约（size: 500），按日期匹配
    const res = await appointmentApi.getAppointments({ page: 1, size: 500 })
    const allRecords = res.data.records || []
    tableData.value = allRecords.filter(r => r.appointmentDate === dateStr)
    total.value = tableData.value.length
  } catch (e) {
    ElMessage.error('加载预约数据失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function statusType(status) {
  const map = { PENDING: 'warning', CONFIRMED: 'primary', DONE: 'success', CANCELLED: 'info' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { PENDING: '待确认', CONFIRMED: '已确认', DONE: '已完成', CANCELLED: '已取消' }
  return map[status] || status
}

function openDetail(row) {
  detailData.value = row
  detailVisible.value = true
}

async function handleStatusChange(row, newStatus) {
  const label = statusLabel(newStatus)
  try {
    await ElMessageBox.confirm(`确认将状态变更为"${label}"？`, '提示', { type: 'warning' })
  } catch { return }
  try {
    await appointmentApi.updateAppointmentStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(`已${label}`)
    fetch()
  } catch (e) {
    ElMessage.error('状态更新失败: ' + (e.message || '请重试'))
  }
}

async function handleDetailStatusChange(newStatus) {
  const label = statusLabel(newStatus)
  try {
    await ElMessageBox.confirm(`确认${label}？`, '提示', { type: 'warning' })
  } catch { return }
  try {
    await appointmentApi.updateAppointmentStatus(detailData.value.id, newStatus)
    detailData.value.status = newStatus
    detailVisible.value = false
    ElMessage.success(`已${label}`)
    fetch()
  } catch (e) {
    ElMessage.error('状态更新失败: ' + (e.message || '请重试'))
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该预约？', '提示', { type: 'warning' })
  } catch { return }
  try {
    await appointmentApi.deleteAppointment(row.id)
    ElMessage.success('已删除')
    fetch()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.message || '请重试'))
  }
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
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

/* 日历视图 */
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
}
.calendar-title {
  min-width: 140px;
  text-align: center;
}
.calendar-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}
.calendar-table th {
  padding: 8px;
  text-align: center;
  font-weight: 600;
  color: #606266;
  background: #F5F7FA;
  border: 1px solid #EBEEF5;
}
.calendar-cell {
  height: 80px;
  vertical-align: top;
  padding: 6px;
  border: 1px solid #EBEEF5;
  cursor: pointer;
  transition: background 0.2s;
}
.calendar-cell:hover {
  background: #F0F9FF;
}
.calendar-cell.is-today .cell-date {
  background: #409EFF;
  color: #fff;
  border-radius: 50%;
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
}
.calendar-cell.is-other {
  color: #C0C4CC;
}
.calendar-cell.has-appointment {
  background: #F0F9FF;
}
.cell-date {
  font-size: 14px;
  font-weight: 500;
}
.cell-count {
  font-size: 11px;
  color: #409EFF;
  margin-top: 4px;
}

/* 详情弹窗 */
.detail-section {
  margin-bottom: 16px;
}
.detail-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #303133;
}
.detail-notes {
  color: #606266;
  font-size: 14px;
  margin: 0;
  padding: 8px 12px;
  background: #F9FAFC;
  border-radius: 4px;
}
.detail-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #EBEEF5;
}
</style>
