<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="grid-overlay" aria-hidden="true"></div>
      <div class="glow" aria-hidden="true"></div>
      <div class="brand-inner">
        <div class="bp-head">
          <svg viewBox="0 0 32 32" fill="none" class="bp-mark" aria-hidden="true">
            <rect width="32" height="32" rx="9" fill="#14B39B" />
            <path d="M9.5 9.5 16 16l6.5-6.5M9.5 22.5 16 16l6.5 6.5" stroke="#fff" stroke-width="1.7" stroke-linecap="round" opacity=".85" />
            <circle cx="9.5" cy="9.5" r="2.3" fill="#fff" />
            <circle cx="22.5" cy="9.5" r="2.3" fill="#fff" />
            <circle cx="9.5" cy="22.5" r="2.3" fill="#fff" />
            <circle cx="22.5" cy="22.5" r="2.3" fill="#fff" />
            <circle cx="16" cy="16" r="3.4" fill="#0C2237" stroke="#fff" stroke-width="1.5" />
          </svg>
          <span class="bp-wordmark">TriAgent</span>
          <span class="bp-chip">v1.0-rc</span>
        </div>

        <div class="bp-body">
          <div class="bp-eyebrow">MULTI-AGENT CLINICAL TRIAGE</div>
          <h1 class="bp-title">让每一次分诊<br /><em>有据可循</em></h1>
          <p class="bp-desc">
            多个专职 Agent 协作完成意图识别、红旗筛查、科室路由与预约编排；
            决策过程、工具调用与知识溯源全部可见、可复现。
          </p>
          <div class="bp-features">
            <div class="bp-feature">
              <span class="bp-num">01</span>
              <div><div class="bp-f-title">多 Agent 协作编排</div><div class="bp-f-sub">Supervisor 路由 · 状态机 · 预算熔断</div></div>
            </div>
            <div class="bp-feature">
              <span class="bp-num">02</span>
              <div><div class="bp-f-title">透明推理过程</div><div class="bp-f-sub">编排决策 · 工具调用 · 标准化 Trace</div></div>
            </div>
            <div class="bp-feature">
              <span class="bp-num">03</span>
              <div><div class="bp-f-title">可溯源的分诊建议</div><div class="bp-f-sub">RAG 知识库 · 置信度 · 安全免责</div></div>
            </div>
          </div>
        </div>

        <div class="bp-foot">
          <span>基于 LLM 的多 Agent 分诊就医系统</span>
          <span class="bp-clock">{{ currentTime }} · {{ currentDate }}</span>
        </div>
      </div>
    </section>

    <section class="form-panel">
      <div class="form-inner">
        <div class="form-eyebrow">控制台登录</div>
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-sub">请使用管理员账号登录工作台</p>

        <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <label class="field-label">用户名</label>
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <label class="field-label">密码</label>
            <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item prop="captchaCode">
            <label class="field-label">验证码</label>
            <div class="captcha-row">
              <el-input v-model="form.captchaCode" placeholder="4 位验证码" size="large" :prefix-icon="Key" class="captcha-input" maxlength="4" />
              <div class="captcha-img-wrap" @click="refreshCaptcha" title="点击刷新验证码">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-img" />
                <el-icon v-else class="captcha-loading"><Loading /></el-icon>
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
              {{ loading ? '登录中…' : '登录工作台' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-hint"><span class="hint-dot"></span>演示环境：账号已预填，输入验证码即可登录</div>
      </div>
      <div class="form-foot">© 2026 TriAgent · 仅用于技术演示，不构成医疗建议</div>
    </section>
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
.login-page { min-height: 100vh; display: flex; background: #fff; }

/* ---------- 左侧品牌叙事 ---------- */
.brand-panel {
  position: relative; width: 54%; min-height: 100vh; overflow: hidden;
  background: linear-gradient(155deg, #0C2237 0%, #102B43 58%, #0B3A44 100%);
  color: #fff;
}
.grid-overlay {
  position: absolute; inset: 0; opacity: .5;
  background-image: linear-gradient(rgba(255,255,255,.045) 1px, transparent 1px),
                    linear-gradient(90deg, rgba(255,255,255,.045) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(120deg, #000 10%, transparent 85%);
}
.glow {
  position: absolute; width: 560px; height: 560px; right: -180px; top: -160px;
  background: radial-gradient(circle, rgba(20,179,155,.20), transparent 62%); pointer-events: none;
}
.brand-inner { position: relative; z-index: 1; height: 100%; min-height: 100vh; display: flex; flex-direction: column; padding: 40px 56px 32px; }
.bp-head { display: flex; align-items: center; gap: 11px; }
.bp-mark { width: 32px; height: 32px; display: block; }
.bp-wordmark { font-size: 18px; font-weight: 700; letter-spacing: .02em; }
.bp-chip { font-size: 10px; letter-spacing: .08em; color: #7FE3D2; border: 1px solid rgba(20,179,155,.4); background: rgba(20,179,155,.12); border-radius: 5px; padding: 2px 7px; }

.bp-body { flex: 1; display: flex; flex-direction: column; justify-content: center; max-width: 540px; padding: 40px 0; }
.bp-eyebrow { font-size: 11px; letter-spacing: .22em; color: #4FD1B5; font-weight: 650; margin-bottom: 20px; }
.bp-title { font-size: clamp(38px, 4vw, 56px); line-height: 1.18; font-weight: 700; letter-spacing: -.02em; margin: 0 0 20px; }
.bp-title em { font-style: normal; color: #3FD0B0; }
.bp-desc { font-size: 14px; line-height: 1.9; color: rgba(255,255,255,.62); max-width: 440px; margin: 0 0 38px; }
.bp-features { display: flex; flex-direction: column; gap: 0; border-top: 1px solid rgba(255,255,255,.1); }
.bp-feature { display: flex; gap: 18px; align-items: flex-start; padding: 16px 0; border-bottom: 1px solid rgba(255,255,255,.1); }
.bp-num { font-size: 11px; font-weight: 700; letter-spacing: .08em; color: #3FD0B0; padding-top: 3px; font-variant-numeric: tabular-nums; }
.bp-f-title { font-size: 14px; font-weight: 650; margin-bottom: 5px; }
.bp-f-sub { font-size: 12px; color: rgba(255,255,255,.45); }
.bp-foot { display: flex; justify-content: space-between; gap: 16px; font-size: 11px; color: rgba(255,255,255,.35); border-top: 1px solid rgba(255,255,255,.08); padding-top: 18px; }
.bp-clock { font-variant-numeric: tabular-nums; }

/* ---------- 右侧表单 ---------- */
.form-panel { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 32px 24px; }
.form-inner { width: 100%; max-width: 372px; }
.form-eyebrow { font-size: 11px; font-weight: 650; letter-spacing: .16em; color: var(--brand); text-transform: uppercase; margin-bottom: 12px; }
.form-title { font-size: 30px; font-weight: 680; color: var(--text-1); letter-spacing: -.02em; margin: 0 0 8px; }
.form-sub { font-size: 13px; color: var(--text-3); margin: 0 0 34px; }
.login-form .el-form-item { margin-bottom: 22px; }
.field-label { display: block; font-size: 12px; font-weight: 600; color: var(--text-2); margin-bottom: 8px; }
.login-form :deep(.el-input__wrapper) {
  height: 44px; border-radius: 9px; background: var(--surface-alt);
  box-shadow: 0 0 0 1px var(--line) inset;
}
.login-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #A9D9D1 inset; }
.login-form :deep(.el-input__wrapper.is-focus) { background: #fff; box-shadow: 0 0 0 1.5px var(--brand) inset; }
.login-form :deep(.el-input__prefix-inner) { color: var(--text-4); }
.captcha-row { display: flex; gap: 10px; width: 100%; }
.captcha-input { flex: 1; min-width: 0; }
.captcha-img-wrap {
  width: 116px; height: 44px; flex-shrink: 0;
  border: 1px solid var(--line); border-radius: 9px; overflow: hidden; cursor: pointer;
  display: flex; align-items: center; justify-content: center; background: var(--surface-alt);
  transition: border-color .18s ease;
}
.captcha-img-wrap:hover { border-color: var(--brand); }
.captcha-img { width: 100%; height: 100%; object-fit: cover; }
.captcha-loading { font-size: 20px; color: var(--text-4); animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.login-btn { width: 100%; height: 46px; font-size: 14px; font-weight: 650; letter-spacing: .04em; border-radius: 9px; margin-top: 4px; }
.form-hint { display: flex; align-items: center; gap: 8px; margin-top: 20px; font-size: 11.5px; color: var(--text-3); background: var(--brand-soft); border: 1px solid #D3EDE7; border-radius: 8px; padding: 10px 12px; }
.hint-dot { width: 5px; height: 5px; border-radius: 50%; background: var(--brand); flex-shrink: 0; }
.form-foot { margin-top: 56px; font-size: 11px; color: var(--text-4); text-align: center; }

@media (max-width: 960px) {
  .brand-panel { display: none; }
  .form-panel { min-height: 100vh; }
}
@media (max-width: 500px) {
  .form-panel { padding: 32px 20px 20px; }
  .form-title { font-size: 26px; }
  .form-foot { margin-top: 36px; }
}
</style>
