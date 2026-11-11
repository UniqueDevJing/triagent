<template>
  <router-view v-if="$route.meta.public" />
  <div v-else ref="shell" class="app-shell" :class="{ 'nav-open': navOpen }">
    <button v-if="navOpen" class="nav-scrim" aria-label="关闭导航" @click="navOpen = false"></button>
    <aside class="sidebar" aria-label="主导航">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true"><i></i></span>
        <div>
          <b>TriAgent Hospital</b>
          <small>AI triage workspace</small>
        </div>
      </div>

      <div class="nav-context">
        <span>{{ roleLabel }}</span>
        <strong>{{ user.name || user.username }}</strong>
      </div>
      <div v-if="role==='admin'" class="nav-section">管理工作区</div>
      <router-link v-for="r in adminRoutes" :key="r.path" :to="r.path" class="nav-item"
                   v-show="role==='admin'"
                   :class="{ active: $route.path === r.path }" @click="navOpen = false">
        <span>{{ r.meta.title }}</span>
        <span class="tag">{{ r.meta.tag }}</span>
      </router-link>

      <div v-if="role==='doctor'" class="nav-section">医生工作区</div>
      <router-link v-for="r in doctorRoutes" :key="r.path" :to="r.path" class="nav-item"
                   v-show="role==='doctor'"
                   :class="{ active: $route.path === r.path }" @click="navOpen = false">
        <span>{{ r.meta.title }}</span>
        <span class="tag">{{ r.meta.tag }}</span>
      </router-link>

      <div v-if="role==='patient'" class="nav-section">患者服务</div>
      <router-link v-for="r in patientRoutes" :key="r.path" :to="r.path" class="nav-item"
                   v-show="role==='patient'"
                   :class="{ active: $route.path === r.path }" @click="navOpen = false">
        <span>{{ r.meta.title }}</span>
        <span class="tag">{{ r.meta.tag }}</span>
      </router-link>
      <div class="sidebar-foot">
        <span>Secure session</span>
        <button class="logout-button" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <button class="menu-button" :aria-expanded="navOpen" aria-label="打开导航" @click="navOpen = !navOpen">
          <span></span><span></span>
        </button>
        <div class="page-head">
          <span class="page-kicker">{{ roleLabel }}</span>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="user-chip">
          <span class="user-avatar">{{ userInitial }}</span>
          <div><strong>{{ user.name || user.username }}</strong><small>{{ roleLabel }}</small></div>
        </div>
      </header>
      <div ref="routeStage" class="route-stage">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
import router from './router'

const routes = router.options.routes
const adminRoutes = routes.filter(r => r.meta && r.meta.group === 'admin')
const doctorRoutes = routes.filter(r => r.meta && r.meta.group === 'doctor')
const patientRoutes = routes.filter(r => r.meta && r.meta.group === 'patient')

const route = useRoute()
const nav = useRouter()
const shell = ref(null)
const routeStage = ref(null)
const navOpen = ref(false)
const user = computed(() => {
  route.fullPath
  return JSON.parse(localStorage.getItem('hospital_user') || '{}')
})
const role = computed(() => user.value.role)
const roleLabel = computed(() => ({ admin: '运营管理', doctor: '医生接诊', patient: '患者服务' }[role.value] || '工作台'))
const userInitial = computed(() => (user.value.name || user.value.username || 'U').slice(0, 1).toUpperCase())
const currentTitle = computed(() => (route.meta && route.meta.title) || '智慧医院')
let motion

function animateRoute() {
  motion?.revert()
  nextTick(() => {
    if (!routeStage.value) return
    motion = gsap.matchMedia()
    motion.add('(prefers-reduced-motion: no-preference)', () => {
      const targets = Array.from(routeStage.value.children)
      if (!targets.length) return
      gsap.fromTo(targets,
        { autoAlpha: 0, y: 14 },
        { autoAlpha: 1, y: 0, duration: 0.48, stagger: 0.05, ease: 'power2.out', clearProps: 'all' })
    }, routeStage.value)
  })
}

onMounted(animateRoute)
watch(() => route.fullPath, animateRoute)
onUnmounted(() => motion?.revert())
function logout() {
  localStorage.removeItem('hospital_token'); localStorage.removeItem('hospital_user'); nav.replace('/login')
}
</script>
