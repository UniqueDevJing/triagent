<template>
  <div class="login-page">
    <div class="login-header">
      <div class="login-logo">
        <svg viewBox="0 0 60 60" fill="none" width="60" height="60">
          <rect width="60" height="60" rx="16" fill="#07C160"/>
          <path d="M18 30h24M30 18v24" stroke="#fff" stroke-width="4" stroke-linecap="round"/>
          <circle cx="30" cy="30" r="10" stroke="#fff" stroke-width="3" fill="none"/>
        </svg>
      </div>
      <h1>传智健康</h1>
      <p>您的健康管理专家</p>
    </div>
    <div class="login-form">
      <van-form @submit="handleLogin">
        <van-cell-group inset>
          <van-field v-model="phone" name="phone" label="手机号" placeholder="请输入手机号" type="tel" maxlength="11"
            :rules="[{ required: true, message: '请输入手机号' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]" />
          <van-field v-model="code" name="code" label="验证码" placeholder="请输入验证码" maxlength="6"
            :rules="[{ required: true, message: '请输入验证码' }]">
            <template #button>
              <van-button size="small" type="primary" :disabled="countdown > 0" @click="sendCode" native-type="button">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </van-button>
            </template>
          </van-field>
        </van-cell-group>
        <div style="margin: 24px 16px">
          <van-button round block type="primary" native-type="submit" :loading="loading">登录</van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { login, sendSmsCode } from '@/api/modules/member'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const phone = ref('')
const code = ref('')
const countdown = ref(0)
const loading = ref(false)

function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    showToast('请输入正确的手机号')
    return
  }
  sendSmsCode(phone.value).then(() => {
    showToast('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  }).catch(() => showToast('发送失败'))
}

async function handleLogin() {
  loading.value = true
  try {
    const res = await login(phone.value, code.value)
    authStore.setToken(res.data.token)
    authStore.setMemberInfo(res.data.member)
    router.push('/home')
  } catch {
    showToast('登录失败')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page { min-height: 100vh; background: linear-gradient(135deg, #07C160 0%, #10AEFF 100%); display: flex; flex-direction: column; justify-content: center; }
.login-header { text-align: center; color: #fff; margin-bottom: 40px; }
.login-header h1 { font-size: 28px; margin: 16px 0 8px; }
.login-header p { font-size: 14px; opacity: 0.85; }
.login-form { padding: 0 16px; }
</style>
