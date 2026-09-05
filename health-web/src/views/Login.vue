<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">
        <svg viewBox="0 0 80 80" fill="none">
          <defs>
            <linearGradient id="leaf1" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#66BB6A"/>
              <stop offset="100%" stop-color="#2E7D32"/>
            </linearGradient>
            <linearGradient id="leaf2" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#81C784"/>
              <stop offset="100%" stop-color="#388E3C"/>
            </linearGradient>
            <linearGradient id="leaf3" x1="0" y1="1" x2="1" y2="0">
              <stop offset="0%" stop-color="#43A047"/>
              <stop offset="100%" stop-color="#1B5E20"/>
            </linearGradient>
            <linearGradient id="vineGrad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#81C784"/>
              <stop offset="50%" stop-color="#4CAF50"/>
              <stop offset="100%" stop-color="#2E7D32"/>
            </linearGradient>
            <filter id="leafShadow">
              <feDropShadow dx="0" dy="1.5" stdDeviation="2.5" flood-color="#1B5E20" flood-opacity="0.22"/>
            </filter>
          </defs>
          <path d="M40,62 C22,46 16,28 28,22 C34,19 38,24 40,28 C42,24 46,19 52,22 C64,28 58,46 40,62Z"
            fill="none" stroke="url(#vineGrad)" stroke-width="0.9" opacity="0.45" stroke-dasharray="3,1.5" class="heart-vine"/>
          <g transform="translate(26,28) rotate(-35) scale(0.9)" filter="url(#leafShadow)">
            <path d="M0,-15 C9,-9 9,5 0,10 C-9,5 -9,-9 0,-15Z" fill="url(#leaf1)" opacity="0.9"/>
            <line x1="0" y1="-13" x2="0" y2="8" stroke="rgba(255,255,255,0.3)" stroke-width="0.8"/>
          </g>
          <g transform="translate(54,28) rotate(35) scale(0.9)" filter="url(#leafShadow)">
            <path d="M0,-15 C9,-9 9,5 0,10 C-9,5 -9,-9 0,-15Z" fill="url(#leaf1)" opacity="0.9"/>
            <line x1="0" y1="-13" x2="0" y2="8" stroke="rgba(255,255,255,0.3)" stroke-width="0.8"/>
          </g>
          <g transform="translate(40,54) rotate(180) scale(0.8)" filter="url(#leafShadow)">
            <path d="M0,-14 C8,-8 8,4 0,9 C-8,4 -8,-8 0,-14Z" fill="url(#leaf3)" opacity="0.88"/>
            <line x1="0" y1="-12" x2="0" y2="7" stroke="rgba(255,255,255,0.25)" stroke-width="0.7"/>
          </g>
          <g transform="translate(30,20) rotate(-60) scale(0.55)" filter="url(#leafShadow)">
            <path d="M0,-13 C7,-7 7,4 0,8 C-7,4 -7,-7 0,-13Z" fill="url(#leaf2)" opacity="0.82"/>
          </g>
          <g transform="translate(50,20) rotate(60) scale(0.55)" filter="url(#leafShadow)">
            <path d="M0,-13 C7,-7 7,4 0,8 C-7,4 -7,-7 0,-13Z" fill="url(#leaf2)" opacity="0.82"/>
          </g>
          <g transform="translate(28,42) rotate(-70) scale(0.5)" filter="url(#leafShadow)">
            <path d="M0,-12 C6,-6 6,3 0,7 C-6,3 -6,-6 0,-12Z" fill="url(#leaf2)" opacity="0.75"/>
          </g>
          <g transform="translate(52,42) rotate(70) scale(0.5)" filter="url(#leafShadow)">
            <path d="M0,-12 C6,-6 6,3 0,7 C-6,3 -6,-6 0,-12Z" fill="url(#leaf2)" opacity="0.75"/>
          </g>
          <circle cx="40" cy="36" r="5" fill="rgba(255,255,255,0.55)"/>
          <circle cx="40" cy="36" r="2.5" fill="#C8E6C9"/>
          <circle cx="40" cy="36" r="1.2" fill="#FFF9C4"/>
          <circle cx="40" cy="40" r="35" fill="none" stroke="#66BB6A" stroke-width="1" opacity="0.35"/>
          <circle cx="40" cy="40" r="30" fill="none" stroke="#81C784" stroke-width="0.5" opacity="0.22" stroke-dasharray="5,4"/>
        </svg>
      </div>
      <div class="login-clock">
        <span class="clock-time">{{ currentTime }}</span>
        <span class="clock-date">{{ currentDate }}</span>
      </div>
      <h2 class="login-title">智能医疗系统</h2>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="验证码" size="large" :prefix-icon="Key" class="captcha-input" maxlength="4"/>
            <div class="captcha-img-wrap" @click="refreshCaptcha" title="点击刷新验证码">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-img" />
              <el-icon v-else class="captcha-loading"><Loading /></el-icon>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, Loading } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getCaptchaImage } from '@/api/modules/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

const currentTime = ref('')
const currentDate = ref('')
let timer = null

function updateClock() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short' })
}

const form = reactive({ username: 'admin', password: 'admin123', captchaCode: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function refreshCaptcha() {
  form.captchaCode = ''
  try {
    const res = await getCaptchaImage()
    captchaImage.value = res.data.captchaImage
    captchaKey.value = res.data.captchaKey
  } catch {
    ElMessage.error('验证码加载失败')
  }
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login(form.username, form.password, captchaKey.value, form.captchaCode)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  updateClock()
  timer = setInterval(updateClock, 1000)
  refreshCaptcha()
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: url('/login-bg.png') center/cover no-repeat;
  position: relative;
  overflow: hidden;
}

.login-card {
  position: relative; z-index: 1;
  background: rgba(255,255,255,0.1);
  backdrop-filter: blur(1px);
  -webkit-backdrop-filter: blur(1px);
  border-radius: 18px;
  padding: 38px 42px;
  width: 400px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 2px 8px rgba(0,0,0,0.04);
  border: 1px solid rgba(255,255,255,0.6);
}

.login-logo {
  display: flex; justify-content: center;
  margin-bottom: 10px;
}
.login-logo svg {
  width: 52px; height: 52px;
  filter: drop-shadow(0 3px 14px rgba(46,125,50,0.25));
  animation: heartbeat 3s ease-in-out infinite;
}
@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  8% { transform: scale(1.08); }
  16% { transform: scale(1); }
  24% { transform: scale(1.05); }
  32% { transform: scale(1); }
}

.login-clock {
  text-align: center;
  margin-bottom: 8px;
}
.clock-time {
  display: block;
  font-size: 26px;
  font-weight: 600;
  color: #1B5E20;
  letter-spacing: 1px;
  font-variant-numeric: tabular-nums;
}
.clock-date {
  display: block;
  font-size: 12px;
  color: #6d8a71;
  margin-top: 2px;
}

.heart-vine {
  animation: vinePulse 3s ease-in-out infinite;
}
@keyframes vinePulse {
  0%, 100% { opacity: 0.35; }
  50% { opacity: 0.6; }
}

.login-title {
  text-align: center;
  font-size: 21px;
  font-weight: 700;
  color: #1B5E20;
  margin: 0;
  letter-spacing: 2px;
  text-shadow: 0 1px 2px rgba(255,255,255,0.5);
}

.login-form { margin-top: 4px; }
.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(176,190,181,0.35) inset;
  transition: all 0.3s;
  background: rgba(255,255,255,0.6);
}
.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1.5px #43A047 inset;
}
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #2E7D32 inset;
  background: rgba(255,255,255,0.85);
}
.captcha-row { display: flex; gap: 12px; }
.captcha-input { flex: 1; }
.captcha-img-wrap {
  width: 120px; height: 42px;
  border-radius: 8px; overflow: hidden; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  background: rgba(240,242,245,0.6); border: 1px solid #b0beb5;
  transition: border-color 0.3s, transform 0.2s;
  flex-shrink: 0;
}
.captcha-img-wrap:hover { border-color: #43A047; transform: scale(1.03); }
.captcha-img { width: 100%; height: 100%; object-fit: cover; }
.captcha-loading { font-size: 22px; color: #78909C; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.login-btn {
  width: 100%; height: 46px;
  font-size: 15px; letter-spacing: 4px;
  border-radius: 8px;
  background: linear-gradient(135deg, #43A047, #2E7D32);
  border: none;
  font-weight: 500;
  transition: all 0.3s;
}
.login-btn:hover {
  background: linear-gradient(135deg, #4CAF50, #1B5E20);
  box-shadow: 0 6px 20px rgba(46,125,50,0.35);
}

.login-hint {
  text-align: center;
  font-size: 12px;
  color: #8d9b90;
  margin-top: 12px;
}

@media (max-width: 500px) {
  .login-card { width: 92vw; padding: 30px 24px; }
}
</style>
