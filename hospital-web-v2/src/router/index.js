import { createRouter, createWebHistory } from 'vue-router'

import Tools from '../views/admin/Tools.vue'
import Knowledge from '../views/admin/Knowledge.vue'
import Dashboard from '../views/admin/Dashboard.vue'
import Usage from '../views/admin/Usage.vue'
import BaseData from '../views/admin/BaseData.vue'
import AiConfig from '../views/admin/AiConfig.vue'
import SymptomLibrary from '../views/admin/SymptomLibrary.vue'
import PreTriage from '../views/patient/PreTriage.vue'
import Records from '../views/patient/Records.vue'
import DoctorConsole from '../views/doctor/DoctorConsole.vue'

const routes = [
  { path: '/', redirect: '/admin/tools' },
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

export default createRouter({
  history: createWebHistory('/v2-web/'),
  routes
})
