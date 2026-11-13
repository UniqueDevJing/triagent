<template>
  <div class="page-container">
    <div class="profile-card">
      <van-image round width="64" height="64" :src="info.avatar || 'https://img.yzcdn.cn/vant/cat.jpeg'" />
      <div class="profile-info">
        <h3>{{ info.name || '未设置昵称' }}</h3>
        <p>ID: {{ info.id }}</p>
      </div>
    </div>
    <van-cell-group inset style="margin-bottom:16px">
      <van-cell title="身高 (cm)" :value="info.height || '未设置'" is-link @click="editField('height')" />
      <van-cell title="体重 (kg)" :value="info.weight || '未设置'" is-link @click="editField('weight')" />
      <van-cell title="血型" :value="info.bloodType || '未知'" />
      <van-cell title="过敏史" :value="info.allergies || '无'" is-link />
    </van-cell-group>
    <van-cell-group inset style="margin-bottom:16px">
      <van-cell title="我的预约" to="/appointments" is-link icon="calendar-o" />
      <van-cell title="评估结果" to="/assessments" is-link icon="balance-list-o" />
      <van-cell title="干预方案" to="/interventions" is-link icon="other-pay" />
      <van-cell title="膳食记录" to="/diet-logs" is-link icon="bookmark-o" />
    </van-cell-group>
    <div style="padding: 16px">
      <van-button round block type="danger" @click="handleLogout">退出登录</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getMemberInfo } from '@/api/modules/member'

const router = useRouter()
const authStore = useAuthStore()
const info = ref({})

onMounted(async () => {
  try { const res = await getMemberInfo(); info.value = res.data } catch { info.value = authStore.memberInfo || {} }
})

function editField(field) { /* TODO: inline edit */ }

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.profile-card { background: linear-gradient(135deg, #07C160, #10AEFF); color: #fff; padding: 32px 16px; border-radius: 12px; display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.profile-info h3 { font-size: 18px; margin-bottom: 4px; }
.profile-info p { font-size: 13px; opacity: 0.8; }
</style>
