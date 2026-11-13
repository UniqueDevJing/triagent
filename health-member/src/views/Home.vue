<template>
  <div class="page-container">
    <div class="home-banner">
      <div class="banner-info">
        <h2>{{ greeting }}，{{ memberInfo?.name || '会员' }}</h2>
        <p>关注健康，从今天开始</p>
      </div>
      <van-image round width="50" height="50" :src="memberInfo?.avatar || 'https://img.yzcdn.cn/vant/cat.jpeg'" />
    </div>
    <van-grid :column-num="4" :border="false">
      <van-grid-item icon="calendar-o" text="我的预约" to="/appointments" />
      <van-grid-item icon="balance-list-o" text="健康评估" to="/assessments" />
      <van-grid-item icon="other-pay" text="干预方案" to="/interventions" />
      <van-grid-item icon="bookmark-o" text="膳食记录" to="/diet-logs" />
    </van-grid>
    <div class="home-section">
      <h3>健康小贴士</h3>
      <van-cell-group inset>
        <van-cell title="每日饮水" label="建议每天饮水 1500-2000ml" icon="water-o" is-link to="/knowledge" />
        <van-cell title="均衡饮食" label="每天摄入12种以上食物" icon="balance-list-o" is-link to="/knowledge" />
        <van-cell title="规律运动" label="每周至少150分钟中等强度运动" icon="flag-o" is-link to="/knowledge" />
        <van-cell title="充足睡眠" label="每天保证7-8小时睡眠" icon="clock-o" is-link to="/knowledge" />
      </van-cell-group>
    </div>
    <div class="home-section">
      <h3>健康知识</h3>
      <van-cell-group inset>
        <van-cell v-for="a in articles" :key="a.id" :title="a.title" :label="a.summary" is-link @click="readArticle(a)" />
      </van-cell-group>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getKnowledgeArticles } from '@/api/modules/member'

const authStore = useAuthStore()
const memberInfo = ref(authStore.memberInfo)
const articles = ref([])
const greeting = ref(new Date().getHours() < 12 ? '早上好' : new Date().getHours() < 18 ? '下午好' : '晚上好')

onMounted(async () => {
  try { const res = await getKnowledgeArticles({ page: 1, size: 4 }); articles.value = res.data.records } catch {}
})

function readArticle(a) { /* TODO: open article detail */ }
</script>

<style scoped>
.home-banner { background: linear-gradient(135deg, #07C160, #10AEFF); color: #fff; padding: 24px 16px; border-radius: 12px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.home-banner h2 { font-size: 20px; margin-bottom: 4px; }
.home-banner p { font-size: 13px; opacity: 0.85; }
.home-section { margin-bottom: 16px; }
.home-section h3 { font-size: 16px; margin: 0 16px 8px; color: #323233; }
</style>
