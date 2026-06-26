<template>
  <div v-if="isLoginPage" class="app-plain">
    <router-view />
  </div>
  <el-container v-else class="app-container">
    <!-- 移动端遮罩 -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>
    <!-- 侧边栏 -->
    <el-aside :width="sidebarOpen ? '230px' : '0'" class="sidebar" :class="{ 'sidebar-mobile': sidebarOpen }">
      <div class="logo-area">
        <div class="logo-icon">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="40" height="40" rx="12" fill="rgba(255,255,255,0.18)"/>
            <path d="M12 20h16M20 12v16" stroke="#fff" stroke-width="3" stroke-linecap="round"/>
            <circle cx="20" cy="20" r="6" stroke="#fff" stroke-width="2" fill="none"/>
          </svg>
        </div>
        <div class="logo-text">
          <div class="logo-title">传智健康</div>
          <div class="logo-subtitle">HEALTH MANAGEMENT</div>
        </div>
      </div>
      <div class="menu-wrapper">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="transparent"
          text-color="rgba(255,255,255,0.7)"
          active-text-color="#fff"
        >
          <el-menu-item index="/dashboard">
            <template #title>
              <div class="menu-item-content">
                <el-icon :size="18"><DataAnalysis /></el-icon>
                <span>工作台</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/members/list">
            <template #title>
              <div class="menu-item-content">
                <el-icon :size="18"><User /></el-icon>
                <span>会员管理</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/appointments/list">
            <template #title>
              <div class="menu-item-content">
                <el-icon :size="18"><Calendar /></el-icon>
                <span>预约管理</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/users">
            <template #title>
              <div class="menu-item-content">
                <el-icon :size="18"><User /></el-icon>
                <span>用户管理</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/health-records">
            <template #title>
              <div class="menu-item-content">
                <el-icon :size="18"><Notebook /></el-icon>
                <span>健康档案</span>
              </div>
            </template>
          </el-menu-item>
          <el-sub-menu index="/assessments">
            <template #title>
              <div class="menu-item-content">
                <el-icon :size="18"><DocumentChecked /></el-icon>
                <span>健康评估</span>
              </div>
            </template>
            <el-menu-item index="/assessments/indicators">评估指标</el-menu-item>
            <el-menu-item index="/assessments/constitutions">中医体质</el-menu-item>
            <el-menu-item index="/assessments/psychology">心理评测</el-menu-item>
            <el-menu-item index="/assessments/records">评估记录</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/intervention">
            <template #title>
              <div class="menu-item-content">
                <el-icon :size="18"><SetUp /></el-icon>
                <span>健康干预</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <template #title>
              <div class="menu-item-content">
                <el-icon :size="18"><Reading /></el-icon>
                <span>知识库</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/ai-agent">
            <template #title>
              <div class="menu-item-content ai-menu">
                <el-icon :size="18"><ChatDotRound /></el-icon>
                <span>AI 健康助手</span>
                <el-tag size="small" effect="dark" class="ai-badge">AI</el-tag>
              </div>
            </template>
          </el-menu-item>
          <el-sub-menu index="/system" v-if="authStore.role === 'ADMIN'">
            <template #title>
              <div class="menu-item-content">
                <el-icon :size="18"><Setting /></el-icon>
                <span>系统设置</span>
              </div>
            </template>
            <el-menu-item index="/system/users">
              <span>用户管理</span>
            </el-menu-item>
            <el-menu-item index="/system/roles">
              <span>角色设置</span>
            </el-menu-item>
            <el-menu-item index="/system/departments">
              <span>科室管理</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
      <div class="sidebar-footer">
        <div class="footer-tip">
          <el-icon><Sunny /></el-icon>
          <span>关爱健康每一天</span>
        </div>
      </div>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-button class="hamburger-btn" text @click="sidebarOpen = !sidebarOpen">
            <el-icon :size="22"><component :is="sidebarOpen ? 'Close' : 'Menu'" /></el-icon>
          </el-button>
          <div class="breadcrumb">
            <el-icon class="breadcrumb-icon" :size="16"><HomeFilled /></el-icon>
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-current">{{ currentTitle }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="header-search">
            <el-icon :size="18" color="#909399"><Search /></el-icon>
          </div>
          <el-popover placement="bottom-end" :width="360" trigger="click" @show="fetchNotifications">
            <template #reference>
              <el-badge :value="unreadCount" class="notice-badge" :max="99" :hidden="unreadCount === 0">
                <el-icon :size="20" color="#606266"><Bell /></el-icon>
              </el-badge>
            </template>
            <div class="notification-panel">
              <div class="notification-header">
                <span>通知中心</span>
                <el-button text size="small" @click="markAllRead" :disabled="unreadCount === 0">全部已读</el-button>
              </div>
              <div class="notification-list" v-loading="notifLoading">
                <div v-if="notifications.length === 0" class="notif-empty">暂无通知</div>
                <div
                  v-for="n in notifications.slice(0, 10)"
                  :key="n.id"
                  class="notif-item"
                  :class="{ unread: n.isRead === 0 }"
                  @click="readNotification(n)"
                >
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
          <div class="user-info">
            <el-avatar :size="34" icon="UserFilled" class="user-avatar" />
            <div class="user-meta">
              <div class="user-name">{{ authStore.userName || '管理员' }}</div>
              <div class="user-role">{{ authStore.role || '系统管理员' }}</div>
            </div>
          </div>
          <el-button text :icon="SwitchButton" @click="handleLogout" class="logout-btn">
            退出
          </el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
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
    const uid = authStore.user?.userId || authStore.user?.id
    if (!uid) return
    const res = await getUnreadCount(uid)
    unreadCount.value = res.data?.count || 0
  } catch {}
}

async function fetchNotifications() {
  notifLoading.value = true
  try {
    const uid = authStore.user?.userId || authStore.user?.id
    if (!uid) return
    const res = await getNotifications({ userId: uid, page: 1, size: 10 })
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
    const uid = authStore.user?.userId || authStore.user?.id
    if (!uid) return
    await markAllReadApi(uid)
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
/* 侧边栏 */
.sidebar {
  background: linear-gradient(180deg, #4B6CF7 0%, #3651D5 60%, #2B45B8 100%);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  border-radius: 0 20px 20px 0;
  box-shadow: 4px 0 24px rgba(75, 108, 247, 0.15);
  position: relative;
}

/* Logo 区域 */
.logo-area {
  padding: 24px 20px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  margin-bottom: 8px;
}
.logo-icon svg {
  display: block;
}
.logo-text {
  display: flex;
  flex-direction: column;
}
.logo-title {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}
.logo-subtitle {
  color: rgba(255,255,255,0.5);
  font-size: 10px;
  letter-spacing: 2px;
  font-weight: 500;
}

/* 菜单 */
.menu-wrapper {
  flex: 1;
  padding: 8px 0;
}
.el-menu {
  border-right: none;
  padding: 0 12px;
}
.el-menu-item {
  border-radius: 10px;
  margin-bottom: 2px;
  height: 46px;
  line-height: 46px;
  transition: all 0.25s ease;
  position: relative;
}
.el-menu-item:hover {
  background: rgba(255,255,255,0.1) !important;
}
.el-menu-item.is-active {
  background: rgba(255,255,255,0.18) !important;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.el-menu-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: #fff;
  border-radius: 3px;
}
.menu-item-content {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 500;
}
.ai-menu {
  position: relative;
}
.ai-badge {
  position: absolute;
  right: -4px;
  top: 50%;
  transform: translateY(-50%);
  background: linear-gradient(135deg, #FF6B6B, #EE5A24) !important;
  border: none;
  font-size: 10px;
  padding: 2px 6px;
  height: 18px;
  line-height: 14px;
  border-radius: 9px;
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.footer-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255,255,255,0.5);
  font-size: 12px;
}

/* 头部 */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0,0,0,0.05);
  padding: 0 28px;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
}
.header-left {
  display: flex;
  align-items: center;
}
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.breadcrumb-icon {
  color: #5B8DEF;
}
.breadcrumb-sep {
  color: #c0c4cc;
  font-size: 12px;
}
.breadcrumb-current {
  color: #303133;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.header-search {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #F5F7FA;
  cursor: pointer;
  transition: all 0.25s;
}
.header-search:hover {
  background: #EBEEF5;
}
.notice-badge {
  cursor: pointer;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-avatar {
  background: linear-gradient(135deg, #5B8DEF, #7BA8F7);
}
.user-meta {
  display: flex;
  flex-direction: column;
}
.user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}
.user-role {
  font-size: 12px;
  color: #909399;
}
.logout-btn {
  margin-left: 4px;
  color: #909399;
}

/* 主内容区 */
.app-main {
  background: #F3F6FB;
  min-height: calc(100vh - 60px);
  padding: 24px;
}

/* 登录页纯布局 */
.app-plain {
  width: 100%;
  min-height: 100vh;
}

/* 汉堡菜单按钮 */
.hamburger-btn {
  display: none;
  margin-right: 12px;
  padding: 6px;
}
.sidebar-overlay {
  display: none;
}

/* 通知面板 */
.notification-panel { max-height: 420px; display: flex; flex-direction: column; }
.notification-header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 10px; border-bottom: 1px solid #EBEEF5; margin-bottom: 8px; font-weight: 600; font-size: 14px; }
.notification-list { flex: 1; overflow-y: auto; max-height: 340px; }
.notif-empty { text-align: center; color: #909399; padding: 32px 0; font-size: 13px; }
.notif-item { display: flex; align-items: flex-start; gap: 10px; padding: 10px 8px; border-radius: 8px; cursor: pointer; transition: background 0.2s; border-bottom: 1px solid #F5F7FA; }
.notif-item:hover { background: #F5F7FA; }
.notif-item.unread { background: #F0F5FF; }
.notif-dot { width: 8px; height: 8px; border-radius: 50%; background: #409EFF; margin-top: 6px; flex-shrink: 0; }
.notif-body { flex: 1; min-width: 0; }
.notif-title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 3px; }
.notif-content { font-size: 12px; color: #606266; margin-bottom: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notif-time { font-size: 11px; color: #C0C4CC; }

/* 响应式布局 */
@media (max-width: 768px) {
  .hamburger-btn {
    display: flex;
  }
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    transition: width 0.3s ease;
    border-radius: 0;
  }
  .sidebar:not(.sidebar-mobile) {
    width: 0 !important;
    overflow: hidden;
  }
  .sidebar-mobile {
    width: 230px !important;
    box-shadow: 4px 0 24px rgba(0,0,0,0.3);
  }
  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.4);
    z-index: 999;
  }
  .app-header {
    padding: 0 16px;
  }
  .app-main {
    padding: 16px;
  }
  .user-meta {
    display: none;
  }
  .header-right {
    gap: 12px;
  }
  .header-search {
    display: none;
  }
}
</style>
