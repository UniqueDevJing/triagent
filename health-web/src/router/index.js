import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', noAuth: true },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '工作台' },
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('@/views/Users.vue'),
    meta: { title: '用户管理' },
  },
  {
    path: '/assessment',
    name: 'Assessment',
    component: () => import('@/views/Assessment.vue'),
    meta: { title: '健康评估' },
  },
  {
    path: '/health-records',
    name: 'HealthRecords',
    component: () => import('@/views/HealthRecords.vue'),
    meta: { title: '健康档案' },
  },
  {
    path: '/intervention',
    name: 'Intervention',
    component: () => import('@/views/Intervention.vue'),
    meta: { title: '健康干预' },
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/Knowledge.vue'),
    meta: { title: '知识库' },
  },
  {
    path: '/ai-agent',
    name: 'AiAgent',
    component: () => import('@/views/AiAgent.vue'),
    meta: { title: 'AI 健康助手' },
  },
  {
    path: '/system',
    redirect: '/system/users',
    meta: { title: '系统设置', role: 'ADMIN' },
    children: [
      { path: 'users', name: 'SystemUsers', component: () => import('@/views/system/Users.vue'), meta: { title: '用户管理', role: 'ADMIN' } },
      { path: 'roles', name: 'SystemRoles', component: () => import('@/views/system/Roles.vue'), meta: { title: '角色设置', role: 'ADMIN' } },
      { path: 'departments', name: 'SystemDepartments', component: () => import('@/views/system/Departments.vue'), meta: { title: '科室管理', role: 'ADMIN' } },
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在', noAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const whiteList = ['/login']

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (authStore.isLoggedIn) {
    if (to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (whiteList.includes(to.path) || to.meta.noAuth) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
