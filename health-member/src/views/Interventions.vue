<template>
  <div class="page-container">
    <h2 class="page-title">干预方案</h2>
    <van-pull-refresh v-model="refreshing" @refresh="fetch">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="fetch">
        <van-cell-group inset v-for="i in list" :key="i.id" style="margin-bottom:12px">
          <van-cell :title="i.title || '干预方案'" :label="'创建: ' + i.createdAt">
            <template #value>
              <van-tag :type="i.status === 1 ? 'success' : ''" size="small">{{ i.status === 1 ? '进行中' : '已完成' }}</van-tag>
            </template>
          </van-cell>
          <van-cell v-if="i.description" title="描述" :value="i.description" />
        </van-cell-group>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getMyInterventions } from '@/api/modules/member'

const list = ref([])
const loading = ref(false), finished = ref(false), refreshing = ref(false)

async function fetch() {
  if (refreshing.value) { list.value = []; refreshing.value = false }
  loading.value = true
  try {
    const res = await getMyInterventions({ page: list.value.length / 20 + 1, size: 20 })
    list.value = [...list.value, ...res.data.records]
    finished.value = res.data.records.length < 20
  } catch {} finally { loading.value = false }
}
</script>
