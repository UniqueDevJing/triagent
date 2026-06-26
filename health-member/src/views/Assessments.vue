<template>
  <div class="page-container">
    <h2 class="page-title">我的评估</h2>
    <van-pull-refresh v-model="refreshing" @refresh="fetch">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="fetch">
        <van-cell-group inset v-for="a in list" :key="a.id" style="margin-bottom:12px">
          <van-cell :title="a.templateName || '健康评估'" :label="'评估日期: ' + (a.assessedAt || a.createdAt)">
            <template #value>
              <van-tag :type="a.riskLevel === 'HIGH' ? 'danger' : a.riskLevel === 'MEDIUM' ? 'warning' : 'success'" size="small">
                {{ riskMap[a.riskLevel] || a.riskLevel }}
              </van-tag>
            </template>
          </van-cell>
          <van-cell v-if="a.conclusion" title="结论" :value="a.conclusion" />
        </van-cell-group>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getMyAssessments } from '@/api/modules/member'

const riskMap = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }
const list = ref([])
const loading = ref(false), finished = ref(false), refreshing = ref(false)

async function fetch() {
  if (refreshing.value) { list.value = []; refreshing.value = false }
  loading.value = true
  try {
    const res = await getMyAssessments({ page: list.value.length / 20 + 1, size: 20 })
    list.value = [...list.value, ...res.data.records]
    finished.value = res.data.records.length < 20
  } catch {} finally { loading.value = false }
}
</script>
