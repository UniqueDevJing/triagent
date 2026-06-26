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
    path: '/assessments',
    redirect: '/assessments/indicators',
    meta: { title: '健康评估' },
    children: [
      { path: 'indicators', name: 'AssessmentIndicators', component: () => import('@/views/assessments/IndicatorList.vue'), meta: { title: '评估指标' } },
      { path: 'constitutions', name: 'TcmConstitutions', component: () => import('@/views/assessments/ConstitutionList.vue'), meta: { title: '中医体质' } },
      { path: 'psychology', name: 'PsychologyAssessments', component: () => import('@/views/assessments/PsychologyList.vue'), meta: { title: '心理评测' } },
      { path: 'records', name: 'AssessmentRecords', component: () => import('@/views/assessments/AssessmentRecordList.vue'), meta: { title: '评估记录' } },
    ]
  },
  {
    path: '/health-records',
    name: 'HealthRecords',
    component: () => import('@/views/HealthRecords.vue'),
    meta: { title: '健康档案' },
  },
  {
    path: '/interventions',
    redirect: '/interventions/chronic',
    meta: { title: '健康干预' },
    children: [
      { path: 'chronic', name: 'ChronicDiseaseList', component: () => import('@/views/interventions/ChronicDiseaseList.vue'), meta: { title: '慢病管理' } },
      { path: 'diet-logs', name: 'DietLogList', component: () => import('@/views/interventions/DietLogList.vue'), meta: { title: '膳食日志' } },
      { path: 'crowd-programs', name: 'CrowdProgramList', component: () => import('@/views/interventions/CrowdProgramList.vue'), meta: { title: '人群方案' } },
    ]
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
    path: '/members',
    redirect: '/members/list',
    meta: { title: '会员管理' },
    children: [
      { path: 'list', name: 'MemberList', component: () => import('@/views/members/MemberList.vue'), meta: { title: '会员列表' } },
      { path: ':id', name: 'MemberDetail', component: () => import('@/views/members/MemberDetail.vue'), meta: { title: '会员详情' } },
    ]
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
    path: '/appointments',
    redirect: '/appointments/list',
    meta: { title: '预约管理' },
    children: [
      { path: 'list', name: 'AppointmentList', component: () => import('@/views/appointments/AppointmentList.vue'), meta: { title: '预约列表' } },
      { path: 'create', name: 'AppointmentCreate', component: () => import('@/views/appointments/AppointmentCreate.vue'), meta: { title: '新增预约' } },
      { path: 'packages', name: 'PackageList', component: () => import('@/views/appointments/PackageList.vue'), meta: { title: '套餐管理' } },
      { path: 'exam-items', name: 'ExamItemList', component: () => import('@/views/appointments/ExamItemList.vue'), meta: { title: '检测项管理' } },
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
