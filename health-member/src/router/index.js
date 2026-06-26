import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { noAuth: true },
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人档案' },
  },
  {
    path: '/appointments',
    name: 'Appointments',
    component: () => import('@/views/Appointments.vue'),
    meta: { title: '我的预约' },
  },
  {
    path: '/assessments',
    name: 'Assessments',
    component: () => import('@/views/Assessments.vue'),
    meta: { title: '评估结果' },
  },
  {
    path: '/interventions',
    name: 'Interventions',
    component: () => import('@/views/Interventions.vue'),
    meta: { title: '干预方案' },
  },
  {
    path: '/diet-logs',
    name: 'DietLogs',
    component: () => import('@/views/DietLogs.vue'),
    meta: { title: '膳食记录' },
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/Knowledge.vue'),
    meta: { title: '知识库' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const whiteList = ['/login']

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (authStore.isLoggedIn()) {
    if (to.path === '/login') next('/home')
    else next()
  } else {
    if (whiteList.includes(to.path) || to.meta.noAuth) next()
    else next('/login')
  }
})

export default router
