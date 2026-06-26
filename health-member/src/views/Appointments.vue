<template>
  <div class="page-container">
    <h2 class="page-title">我的预约</h2>
    <van-tabs v-model:active="active">
      <van-tab title="进行中">
        <van-pull-refresh v-model="refreshing" @refresh="fetch">
          <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="fetch">
            <van-cell-group inset v-for="a in appointments" :key="a.id" style="margin-bottom:12px">
              <van-cell :title="'预约 #' + a.id" :label="'日期: ' + a.appointmentDate">
                <template #value>
                  <van-tag :type="a.status === 'CONFIRMED' ? 'success' : a.status === 'DONE' ? '' : 'warning'" size="small">
                    {{ statusMap[a.status] }}
                  </van-tag>
                </template>
              </van-cell>
              <van-cell title="套餐" :value="a.packageName || '-'" />
              <van-cell title="时段" :value="a.timeSlot === 'MORNING' ? '上午' : '下午'" />
            </van-cell-group>
          </van-list>
        </van-pull-refresh>
      </van-tab>
      <van-tab title="预约体检">
        <div style="padding:24px 16px">
          <van-form @submit="createAppt">
            <van-field v-model="form.packageId" name="packageId" label="选择套餐" />
            <van-field v-model="form.appointmentDate" name="appointmentDate" label="预约日期" type="date" />
            <van-field name="timeSlot" label="时段">
              <template #input><van-radio-group v-model="form.timeSlot" direction="horizontal"><van-radio name="MORNING">上午</van-radio><van-radio name="AFTERNOON">下午</van-radio></van-radio-group></template>
            </van-field>
            <div style="margin:24px 16px"><van-button round block type="primary" native-type="submit">提交预约</van-button></div>
          </van-form>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { showToast } from 'vant'
import { getMyAppointments, createAppointment } from '@/api/modules/member'

const statusMap = { PENDING: '待确认', CONFIRMED: '已确认', DONE: '已完成', CANCELLED: '已取消' }
const active = ref(0)
const appointments = ref([])
const loading = ref(false), finished = ref(false), refreshing = ref(false)
const form = ref({ packageId: 1, appointmentDate: '', timeSlot: 'MORNING' })

async function fetch() {
  if (refreshing.value) { appointments.value = []; refreshing.value = false }
  loading.value = true
  try {
    const res = await getMyAppointments({ page: appointments.value.length / 20 + 1, size: 20 })
    appointments.value = [...appointments.value, ...res.data.records]
    finished.value = res.data.records.length < 20
  } catch {} finally { loading.value = false }
}

async function createAppt() {
  try {
    await createAppointment(form.value)
    showToast('预约成功')
    active.value = 0
    fetch()
  } catch { showToast('预约失败') }
}
</script>
