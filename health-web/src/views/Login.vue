<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-card">
      <div class="login-logo">
        <svg viewBox="0 0 48 48" fill="none">
          <rect width="48" height="48" rx="14" fill="url(#logoGrad)"/>
          <path d="M14 24h20M24 14v20" stroke="#fff" stroke-width="3" stroke-linecap="round"/>
          <circle cx="24" cy="24" r="7" stroke="#fff" stroke-width="2.5" fill="none"/>
          <defs><linearGradient id="logoGrad" x1="0" y1="0" x2="48" y2="48"><stop stop-color="#5B8DEF"/><stop offset="1" stop-color="#3651D5"/></linearGradient></defs>
        </svg>
      </div>
      <h2 class="login-title">传智健康管理系统</h2>
      <p class="login-subtitle">HEALTH MANAGEMENT SYSTEM</p>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p class="login-hint">演示账号: admin / admin123</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: 'admin', password: 'admin123' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.login-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #4B6CF7 30%, #3651D5 70%, #2B45B8 100%);
  z-index: 0;
}
.login-bg::before {
  content: '';
  position: absolute;
  width: 600px; height: 600px;
  background: rgba(255,255,255,0.05);
  border-radius: 50%;
  top: -200px; right: -100px;
}
.login-bg::after {
  content: '';
  position: absolute;
  width: 400px; height: 400px;
  background: rgba(255,255,255,0.04);
  border-radius: 50%;
  bottom: -100px; left: -80px;
}
.login-card {
  position: relative; z-index: 1;
  background: #fff;
  border-radius: 20px;
  padding: 48px 44px;
  width: 420px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15), 0 0 0 1px rgba(255,255,255,0.1);
}
.login-logo { display: flex; justify-content: center; margin-bottom: 20px; }
.login-title { text-align: center; font-size: 22px; font-weight: 700; color: #303133; margin: 0; }
.login-subtitle { text-align: center; font-size: 11px; color: #909399; letter-spacing: 3px; margin: 8px 0 32px; }
.login-form { margin-top: 8px; }
.login-btn { width: 100%; height: 46px; font-size: 16px; border-radius: 10px; }
.login-hint { text-align: center; font-size: 12px; color: #c0c4cc; margin-top: 16px; }
</style>
