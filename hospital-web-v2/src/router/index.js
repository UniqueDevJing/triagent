import { createRouter, createWebHistory } from 'vue-router'

const Tools = () => import('../views/admin/Tools.vue')
const Knowledge = () => import('../views/admin/Knowledge.vue')
const Dashboard = () => import('../views/admin/Dashboard.vue')
const Usage = () => import('../views/admin/Usage.vue')
const BaseData = () => import('../views/admin/BaseData.vue')
const AiConfig = () => import('../views/admin/AiConfig.vue')
const SymptomLibrary = () => import('../views/admin/SymptomLibrary.vue')
const PreTriage = () => import('../views/patient/PreTriage.vue')
const Records = () => import('../views/patient/Records.vue')
const DoctorConsole = () => import('../views/doctor/DoctorConsole.vue')
const Login = () => import('../views/Login.vue')

const routes = [
  { path: '/', redirect: '/patient/pre' },
  { path: '/login', name: 'login', component: Login, meta: { public: true, title: '登录' } },
  { path: '/admin/tools', name: 'tools', component: Tools, meta: { group: 'admin', title: '工具注册表', tag: 'MCP' } },
  { path: '/admin/knowledge', name: 'knowledge', component: Knowledge, meta: { group: 'admin', title: '医学知识库', tag: 'RAG' } },
  { path: '/admin/aiconfig', name: 'aiconfig', component: AiConfig, meta: { group: 'admin', title: 'AI 模型配置', tag: 'LLM' } },
  { path: '/admin/dashboard', name: 'dashboard', component: Dashboard, meta: { group: 'admin', title: '运营看板', tag: 'KPI' } },
  { path: '/admin/usage', name: 'usage', component: Usage, meta: { group: 'admin', title: 'AI 观测', tag: 'Token' } },
  { path: '/admin/basedata', name: 'basedata', component: BaseData, meta: { group: 'admin', title: '基础数据', tag: 'CRUD' } },
  { path: '/admin/symptoms', name: 'symptoms', component: SymptomLibrary, meta: { group: 'admin', title: '症状标签库', tag: 'TRIAGE' } },
  { path: '/doctor/console', name: 'doctorConsole', component: DoctorConsole, meta: { group: 'doctor', title: '门诊接诊台', tag: 'EMR' } },
  { path: '/patient/pre', name: 'pre', component: PreTriage, meta: { group: 'patient', title: '智能预问诊', tag: 'SSE' } },
  { path: '/patient/records', name: 'records', component: Records, meta: { group: 'patient', title: '我的就诊', tag: 'EMR' } }
]

const router = createRouter({
  history: createWebHistory('/v2-web/'),
  routes,
  scrollBehavior() {
    return { top: 0, left: 0 }
  }
})

router.beforeEach(to => {
  if (to.meta.public) return true
  const token = localStorage.getItem('hospital_token')
  const user = JSON.parse(localStorage.getItem('hospital_user') || 'null')
  if (!token || !user) return '/login'
  const group = to.meta.group
  if (group && group !== user.role && user.role !== 'admin') {
    return user.role === 'doctor' ? '/doctor/console' : '/patient/pre'
  }
  return true
})

export default router
