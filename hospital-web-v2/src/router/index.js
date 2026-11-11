import { createRouter, createWebHistory } from 'vue-router'

import Tools from '../views/admin/Tools.vue'
import Knowledge from '../views/admin/Knowledge.vue'
import Dashboard from '../views/admin/Dashboard.vue'
import Usage from '../views/admin/Usage.vue'
import BaseData from '../views/admin/BaseData.vue'
import PreTriage from '../views/patient/PreTriage.vue'
import Records from '../views/patient/Records.vue'

const routes = [
  { path: '/', redirect: '/admin/tools' },
  { path: '/admin/tools', name: 'tools', component: Tools, meta: { group: 'admin', title: '工具注册表' } },
  { path: '/admin/knowledge', name: 'knowledge', component: Knowledge, meta: { group: 'admin', title: '医学知识库' } },
  { path: '/admin/dashboard', name: 'dashboard', component: Dashboard, meta: { group: 'admin', title: '运营看板' } },
  { path: '/admin/usage', name: 'usage', component: Usage, meta: { group: 'admin', title: 'AI 观测' } },
  { path: '/admin/basedata', name: 'basedata', component: BaseData, meta: { group: 'admin', title: '基础数据' } },
  { path: '/patient/pre', name: 'pre', component: PreTriage, meta: { group: 'patient', title: '智能预问诊' } },
  { path: '/patient/records', name: 'records', component: Records, meta: { group: 'patient', title: '我的就诊' } }
]

export default createRouter({
  history: createWebHistory('/v2-web/'),
  routes
})
