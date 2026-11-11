<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="pulse"></span>
        <div>
          <b>SMART HOSPITAL</b>
          <small>AI Triage Console · v2</small>
        </div>
      </div>

      <div class="section-title" style="color:#6f93a0;font-size:12px;">管理端 · ADMIN</div>
      <router-link v-for="r in adminRoutes" :key="r.path" :to="r.path" class="nav-item"
                   :class="{ active: $route.path === r.path }">
        <span>{{ r.meta.title }}</span>
        <span class="tag">{{ r.meta.tag }}</span>
      </router-link>

      <div class="section-title" style="color:#6f93a0;font-size:12px;margin-top:18px;">医生端 · DOCTOR</div>
      <router-link v-for="r in doctorRoutes" :key="r.path" :to="r.path" class="nav-item"
                   :class="{ active: $route.path === r.path }">
        <span>{{ r.meta.title }}</span>
        <span class="tag">{{ r.meta.tag }}</span>
      </router-link>

      <div class="section-title" style="color:#6f93a0;font-size:12px;margin-top:18px;">患者端 · PATIENT</div>
      <router-link v-for="r in patientRoutes" :key="r.path" :to="r.path" class="nav-item"
                   :class="{ active: $route.path === r.path }">
        <span>{{ r.meta.title }}</span>
        <span class="tag">{{ r.meta.tag }}</span>
      </router-link>
    </aside>

    <main class="main">
      <div class="page-head">
        <h1>{{ currentTitle }}</h1>
        <span class="sub">{{ currentSub }}</span>
      </div>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import router from './router'

const routes = router.options.routes
const adminRoutes = routes.filter(r => r.meta && r.meta.group === 'admin')
const doctorRoutes = routes.filter(r => r.meta && r.meta.group === 'doctor')
const patientRoutes = routes.filter(r => r.meta && r.meta.group === 'patient')

const route = useRoute()
const currentTitle = computed(() => (route.meta && route.meta.title) || '智慧医院')
const currentSub = computed(() => route.path)
</script>
