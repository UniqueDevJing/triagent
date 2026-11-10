<template>
  <!-- 生命体征背景层：固定视口、不参与布局、不接收指针事件 -->
  <div class="clinical-backdrop" aria-hidden="true">
    <span class="cb-grid"></span>
    <span class="cb-glow"></span>
    <span class="cb-wave"></span>
    <span class="cb-sweep"></span>
    <span class="cb-noise"></span>
    <span class="cb-corner cb-corner--tl"></span>
    <span class="cb-corner cb-corner--br"></span>
    <span class="cb-readout">TriAgent / Vital Console · Signal Nominal · 3 Agents Online</span>
  </div>
  <div v-if="isLoginPage" class="app-plain">
    <router-view />
  </div>
  <div v-else class="app-shell">
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>

    <aside class="sidebar" :class="{ 'sidebar-mobile': sidebarOpen }">
      <div class="brand">
        <div class="brand-mark">
          <svg viewBox="0 0 32 32" fill="none" aria-hidden="true">
            <defs>
              <linearGradient id="triBrand" x1="0" y1="0" x2="1" y2="1">
                <stop stop-color="#14B39B" />
                <stop offset="1" stop-color="#0A7D6D" />
              </linearGradient>
            </defs>
            <rect width="32" height="32" rx="9" fill="url(#triBrand)" />
            <path d="M9.5 9.5 16 16l6.5-6.5M9.5 22.5 16 16l6.5 6.5" stroke="#fff" stroke-width="1.7" stroke-linecap="round" opacity=".85" />
            <circle cx="9.5" cy="9.5" r="2.3" fill="#fff" />
            <circle cx="22.5" cy="9.5" r="2.3" fill="#fff" />
            <circle cx="9.5" cy="22.5" r="2.3" fill="#fff" />
            <circle cx="22.5" cy="22.5" r="2.3" fill="#fff" />
            <circle cx="16" cy="16" r="3.4" fill="#0C2237" stroke="#fff" stroke-width="1.5" />
          </svg>
        </div>
        <div class="brand-text">
          <div class="brand-name">TriAgent</div>
          <div class="brand-sub">多 AGENT 分诊平台</div>
        </div>
      </div>

      <nav class="nav-scroll">
        <div class="nav-section">
          <div class="nav-caption">工作区</div>
          <el-menu :default-active="activeMenu" router background-color="transparent" text-color="rgba(255,255,255,.62)" active-text-color="#fff">
            <el-menu-item index="/dashboard">
              <div class="menu-item-content"><el-icon :size="17"><DataAnalysis /></el-icon><span>工作台</span></div>
            </el-menu-item>
          </el-menu>
        </div>

        <div class="nav-section">
          <div class="nav-caption">临床服务</div>
          <el-menu :default-active="activeMenu" router background-color="transparent" text-color="rgba(255,255,255,.62)" active-text-color="#fff">
            <el-menu-item index="/assistant">
              <div class="menu-item-content"><el-icon :size="17"><ChatDotRound /></el-icon><span>智能分诊</span><span class="ai-badge">AI</span></div>
            </el-menu-item>
            <el-menu-item index="/members/list">
              <div class="menu-item-content"><el-icon :size="17"><User /></el-icon><span>会员管理</span></div>
            </el-menu-item>
            <el-menu-item index="/appointments/list">
              <div class="menu-item-content"><el-icon :size="17"><Calendar /></el-icon><span>预约管理</span></div>
            </el-menu-item>
            <el-menu-item index="/health-records">
              <div class="menu-item-content"><el-icon :size="17"><Notebook /></el-icon><span>健康档案</span></div>
            </el-menu-item>
          </el-menu>
        </div>

        <div class="nav-section">
          <div class="nav-caption">健康数据</div>
          <el-menu :default-active="activeMenu" router background-color="transparent" text-color="rgba(255,255,255,.62)" active-text-color="#fff">
            <el-sub-menu index="/assessments">
              <template #title><div class="menu-item-content"><el-icon :size="17"><DocumentChecked /></el-icon><span>健康评估</span></div></template>
              <el-menu-item index="/assessments/indicators"><span>评估指标</span></el-menu-item>
              <el-menu-item index="/assessments/constitutions"><span>中医体质</span></el-menu-item>
              <el-menu-item index="/assessments/psychology"><span>心理评测</span></el-menu-item>
              <el-menu-item index="/assessments/records"><span>评估记录</span></el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="/interventions">
              <template #title><div class="menu-item-content"><el-icon :size="17"><SetUp /></el-icon><span>健康干预</span></div></template>
              <el-menu-item index="/interventions/chronic"><span>慢病管理</span></el-menu-item>
              <el-menu-item index="/interventions/diet-logs"><span>膳食日志</span></el-menu-item>
              <el-menu-item index="/interventions/crowd-programs"><span>人群方案</span></el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="/knowledge">
              <template #title><div class="menu-item-content"><el-icon :size="17"><Reading /></el-icon><span>知识库</span></div></template>
              <el-menu-item index="/knowledge/contents"><span>宣教内容</span></el-menu-item>
              <el-menu-item index="/knowledge/words"><span>宣教词</span></el-menu-item>
              <el-menu-item index="/knowledge/exercises"><span>运动项目库</span></el-menu-item>
              <el-menu-item index="/knowledge/diseases"><span>疾病知识库</span></el-menu-item>
              <el-menu-item index="/knowledge/recipes"><span>健康食谱库</span></el-menu-item>
            </el-sub-menu>
          </el-menu>
        </div>

        <div class="nav-section" v-if="authStore.role === 'ADMIN'">
          <div class="nav-caption">系统</div>
          <el-menu :default-active="activeMenu" router background-color="transparent" text-color="rgba(255,255,255,.62)" active-text-color="#fff">
            <el-sub-menu index="/system">
              <template #title><div class="menu-item-content"><el-icon :size="17"><Setting /></el-icon><span>系统设置</span></div></template>
              <el-menu-item index="/system/users"><span>用户管理</span></el-menu-item>
              <el-menu-item index="/system/roles"><span>角色设置</span></el-menu-item>
              <el-menu-item index="/system/departments"><span>科室管理</span></el-menu-item>
            </el-sub-menu>
          </el-menu>
        </div>
      </nav>

      <div class="sidebar-foot">
        <div class="foot-status"><span class="status-dot"></span>三 Agent 编排就绪</div>
        <div class="foot-version">TriAgent v1.0-rc</div>
      </div>
    </aside>

    <div class="main-col">
      <header class="app-header">
        <div class="header-left">
          <el-button class="hamburger-btn" text @click="sidebarOpen = !sidebarOpen">
            <el-icon :size="20"><component :is="sidebarOpen ? 'Close' : 'Menu'" /></el-icon>
          </el-button>
          <div class="breadcrumb">
            <span class="crumb-root">控制台</span>
            <span class="crumb-sep">/</span>
            <span class="crumb-current">{{ currentTitle }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="health-pill"><span class="health-dot"></span>服务正常</div>
          <el-popover placement="bottom-end" :width="340" trigger="click" @show="fetchNotifications">
            <template #reference>
              <el-badge :value="unreadCount" class="notice-badge" :max="99" :hidden="unreadCount === 0">
                <el-icon :size="18" class="notice-icon"><Bell /></el-icon>
              </el-badge>
            </template>
            <div class="notification-panel">
              <div class="notification-header">
                <span>通知中心</span>
                <el-button text size="small" @click="markAllRead" :disabled="unreadCount === 0">全部已读</el-button>
              </div>
              <div class="notification-list" v-loading="notifLoading">
                <div v-if="notifications.length === 0" class="notif-empty">暂无通知</div>
                <div v-for="n in notifications.slice(0, 10)" :key="n.id" class="notif-item" :class="{ unread: n.isRead === 0 }" @click="readNotification(n)">
                  <div class="notif-dot" v-if="n.isRead === 0"></div>
                  <div class="notif-body">
                    <div class="notif-title">{{ n.title }}</div>
                    <div class="notif-content">{{ n.content }}</div>
                    <div class="notif-time">{{ n.createdAt?.slice(0, 16) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>
          <div class="header-divider"></div>
          <div class="user-info">
            <el-avatar :size="32" icon="UserFilled" class="user-avatar" />
            <div class="user-meta">
              <div class="user-name">{{ authStore.userName || '管理员' }}</div>
              <div class="user-role">{{ authStore.role || '系统管理员' }}</div>
            </div>
          </div>
          <el-button text :icon="SwitchButton" @click="handleLogout" class="logout-btn">退出</el-button>
        </div>
      </header>
      <main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>


<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getNotifications, getUnreadCount, markAsRead, markAllRead as markAllReadApi } from '@/api/modules/notifications'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const sidebarOpen = ref(window.innerWidth > 768)
const unreadCount = ref(0)
const notifications = ref([])
const notifLoading = ref(false)
let notifTimer = null

function onResize() {
  sidebarOpen.value = window.innerWidth > 768
}
onMounted(() => {
  window.addEventListener('resize', onResize)
  if (authStore.isLoggedIn) fetchUnreadCount()
  notifTimer = setInterval(() => {
    if (authStore.isLoggedIn) fetchUnreadCount()
  }, 30000)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (notifTimer) clearInterval(notifTimer)
})

async function fetchUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.count || 0
  } catch {}
}

async function fetchNotifications() {
  notifLoading.value = true
  try {
    const res = await getNotifications({ page: 1, size: 10 })
    notifications.value = res.data?.records || []
  } catch {}
  notifLoading.value = false
}

async function readNotification(n) {
  if (n.isRead === 0) {
    try {
      await markAsRead(n.id)
      n.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {}
  }
}

async function markAllRead() {
  try {
    await markAllReadApi()
    notifications.value.forEach(n => n.isRead = 1)
    unreadCount.value = 0
  } catch {}
}

const isLoginPage = computed(() => route.path === '/login' || route.name === 'NotFound')
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '工作台')

function handleLogout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style>
.app-shell { display: flex; min-height: 100vh; background: var(--bg); }
.app-plain { width: 100%; min-height: 100vh; }

/* ---------- 侧边栏 ---------- */
.sidebar {
  width: 232px; flex-shrink: 0;
  background: var(--ink-900);
  border-right: 1px solid rgba(255,255,255,.06);
  display: flex; flex-direction: column;
  position: sticky; top: 0; height: 100vh; z-index: 20;
}
.brand {
  display: flex; align-items: center; gap: 11px;
  padding: 22px 20px 20px;
  border-bottom: 1px solid rgba(255,255,255,.07);
}
.brand-mark svg { display: block; width: 34px; height: 34px; }
.brand-name { color: #fff; font-size: 17px; font-weight: 700; letter-spacing: .02em; line-height: 1.2; }
.brand-sub { color: rgba(255,255,255,.42); font-size: 10px; letter-spacing: .14em; margin-top: 3px; }

.nav-scroll { flex: 1; overflow-y: auto; padding: 14px 10px 20px; }
.nav-scroll::-webkit-scrollbar { width: 5px; }
.nav-scroll::-webkit-scrollbar-thumb { background: rgba(255,255,255,.14); border-radius: 4px; }
.nav-section { margin-bottom: 16px; }
.nav-caption {
  padding: 0 12px 8px; color: rgba(255,255,255,.32);
  font-size: 10px; font-weight: 650; letter-spacing: .16em; text-transform: uppercase;
}
.nav-section .el-menu { border-right: none; background: transparent; padding: 0; }
.nav-section .el-menu-item,
.nav-section .el-sub-menu__title {
  height: 40px; line-height: 40px; border-radius: 8px;
  margin-bottom: 2px; padding: 0 12px !important;
  color: rgba(255,255,255,.66) !important; font-size: 13px;
  background: transparent !important;
  transition: background .18s ease, color .18s ease;
}
.nav-section .el-menu-item:hover,
.nav-section .el-sub-menu__title:hover { background: rgba(255,255,255,.07) !important; color: #fff !important; }
.nav-section .el-menu-item.is-active {
  background: rgba(14,159,138,.18) !important; color: #fff !important; font-weight: 600;
  box-shadow: inset 3px 0 0 var(--brand);
}
.nav-section .el-sub-menu .el-menu-item { height: 34px; line-height: 34px; padding-left: 38px !important; font-size: 12.5px; }
.nav-section .el-sub-menu.is-active > .el-sub-menu__title { color: #fff !important; }
.nav-section .el-sub-menu__icon-arrow { color: rgba(255,255,255,.45); }
.menu-item-content { display: flex; align-items: center; gap: 10px; width: 100%; }
.menu-item-content .el-icon { flex-shrink: 0; }
.ai-badge {
  margin-left: auto; font-size: 9px; font-weight: 700; letter-spacing: .08em;
  color: #7FE3D2; background: rgba(14,159,138,.22);
  border: 1px solid rgba(20,179,155,.35); border-radius: 4px; padding: 1px 5px;
}

.sidebar-foot { padding: 14px 20px 18px; border-top: 1px solid rgba(255,255,255,.07); }
.foot-status { display: flex; align-items: center; gap: 7px; color: rgba(255,255,255,.55); font-size: 11.5px; }
.status-dot, .health-dot { width: 6px; height: 6px; border-radius: 50%; background: #2FD3A4; box-shadow: 0 0 0 3px rgba(47,211,164,.15); flex-shrink: 0; }
.foot-version { color: rgba(255,255,255,.25); font-size: 10.5px; margin-top: 6px; letter-spacing: .04em; }

/* ---------- 顶栏 ---------- */
.main-col { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.app-header {
  height: 60px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(255,255,255,.92); backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line); padding: 0 26px;
  position: sticky; top: 0; z-index: 10;
}
.header-left, .header-right { display: flex; align-items: center; }
.header-right { gap: 16px; }
.breadcrumb { display: flex; align-items: center; gap: 9px; font-size: 13px; }
.crumb-root { color: var(--text-4); }
.crumb-sep { color: #C9D3DC; }
.crumb-current { color: var(--text-1); font-weight: 650; }
.health-pill {
  display: flex; align-items: center; gap: 7px;
  font-size: 11.5px; color: var(--text-3);
  background: var(--surface-alt); border: 1px solid var(--line);
  border-radius: 999px; padding: 5px 11px;
}
.health-pill .health-dot { width: 5px; height: 5px; box-shadow: 0 0 0 2px rgba(47,211,164,.16); }
.notice-badge { cursor: pointer; display: flex; }
.notice-icon { color: var(--text-3); }
.header-divider { width: 1px; height: 22px; background: var(--line); }
.user-info { display: flex; align-items: center; gap: 9px; }
.user-avatar { background: #E8F0F4; color: var(--ink-700) !important; font-size: 14px; }
.user-name { font-size: 12.5px; font-weight: 650; color: var(--text-1); line-height: 1.3; }
.user-role { font-size: 10.5px; color: var(--text-4); margin-top: 2px; }
.logout-btn { color: var(--text-3); font-size: 12.5px; margin-left: 2px; }
.hamburger-btn { display: none; margin-right: 8px; padding: 6px; }
.sidebar-overlay { display: none; }

.app-main { flex: 1; min-height: calc(100vh - 60px); padding: 24px; }

/* ---------- 通知面板 ---------- */
.notification-panel { max-height: 400px; display: flex; flex-direction: column; }
.notification-header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 10px; border-bottom: 1px solid var(--line); margin-bottom: 6px; font-weight: 650; font-size: 13px; }
.notification-list { flex: 1; overflow-y: auto; max-height: 320px; }
.notif-empty { text-align: center; color: var(--text-4); padding: 30px 0; font-size: 12px; }
.notif-item { display: flex; align-items: flex-start; gap: 9px; padding: 9px 8px; border-radius: 7px; cursor: pointer; border-bottom: 1px solid var(--line-soft); }
.notif-item:hover { background: var(--surface-alt); }
.notif-item.unread { background: var(--brand-soft); }
.notif-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--brand); margin-top: 6px; flex-shrink: 0; }
.notif-body { flex: 1; min-width: 0; }
.notif-title { font-size: 12.5px; font-weight: 650; color: var(--text-1); margin-bottom: 3px; }
.notif-content { font-size: 11.5px; color: var(--text-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notif-time { font-size: 10.5px; color: var(--text-4); margin-top: 3px; }

@media (max-width: 768px) {
  .sidebar { position: fixed; left: 0; top: 0; bottom: 0; width: 0; overflow: hidden; transition: width .25s ease; }
  .sidebar.sidebar-mobile { width: 232px !important; overflow-y: auto; box-shadow: 8px 0 32px rgba(12,34,55,.28); }
  .sidebar-overlay { display: block; position: fixed; inset: 0; background: rgba(12,34,55,.45); z-index: 19; }
  .hamburger-btn { display: flex; }
  .app-header { padding: 0 14px; }
  .app-main { padding: 16px; }
  .user-meta, .health-pill { display: none; }
  .header-right { gap: 10px; }
}
</style>
