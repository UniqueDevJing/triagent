<template>
  <div class="page-container">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 class="page-title" style="margin:0">膳食记录</h2>
      <van-button size="small" type="primary" @click="addVisible = true">+ 新增</van-button>
    </div>
    <van-pull-refresh v-model="refreshing" @refresh="fetch">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="fetch">
        <van-cell-group inset v-for="d in list" :key="d.id" style="margin-bottom:12px">
          <van-cell :title="mealMap[d.mealType]" :label="d.recordedDate">
            <template #value><span>{{ d.calories }} kcal</span></template>
          </van-cell>
          <van-cell v-if="d.foodItems" title="食物" :value="formatFood(d.foodItems)" />
        </van-cell-group>
      </van-list>
    </van-pull-refresh>

    <van-popup v-model:show="addVisible" position="bottom" :style="{ padding: '16px' }" round>
      <h3 style="margin-bottom:16px">记录膳食</h3>
      <van-form @submit="saveLog">
        <van-field name="mealType" label="餐次">
          <template #input><van-radio-group v-model="form.mealType" direction="horizontal"><van-radio v-for="(v,k) in mealMap" :key="k" :name="k">{{ v }}</van-radio></van-radio-group></template>
        </van-field>
        <van-field v-model="form.foodItems" name="foodItems" label="食物" placeholder="如: 米饭1碗, 青菜100g" />
        <van-field v-model="form.calories" name="calories" label="热量(kcal)" type="number" />
        <van-field v-model="form.recordedDate" name="recordedDate" label="日期" type="date" />
        <div style="margin:16px 0"><van-button round block type="primary" native-type="submit">保存</van-button></div>
      </van-form>
    </van-popup>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { showToast } from 'vant'
import { getDietLogs, createDietLog } from '@/api/modules/member'

const mealMap = { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }
const list = ref([])
const loading = ref(false), finished = ref(false), refreshing = ref(false)
const addVisible = ref(false)
const form = ref({ mealType: 'LUNCH', calories: 0, foodItems: '', recordedDate: new Date().toISOString().slice(0, 10) })

async function fetch() {
  if (refreshing.value) { list.value = []; refreshing.value = false }
  loading.value = true
  try {
    const res = await getDietLogs({ page: list.value.length / 20 + 1, size: 20 })
    list.value = [...list.value, ...res.data.records]
    finished.value = res.data.records.length < 20
  } catch {} finally { loading.value = false }
}

function formatFood(items) {
  try { return typeof items === 'string' ? JSON.parse(items).map(f => f.name || f).join(', ') : JSON.stringify(items) } catch { return items }
}

async function saveLog() {
  try {
    await createDietLog(form.value)
    addVisible.value = false
    showToast('已记录')
    list.value = []; fetch()
  } catch { showToast('保存失败') }
}
</script>
