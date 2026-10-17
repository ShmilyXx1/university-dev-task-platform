<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside class="layout-aside" width="220px">
      <div class="logo-box">审核管理系统</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,.72)"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/auditMain">
          <template #icon><House /></template>
          <span>订单管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="layout-header">
        <div class="header-right">
          <NotificationBell />
          <span class="header-username">{{ userStore.adminName }}</span>
          <el-button type="danger" icon="Logout" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <!-- 页面内容区域 -->
      <el-main class="layout-main">
        <div class="page-banner">
          <div>
            <div class="banner-badge">审核工作台 · AUDIT CONSOLE</div>
            <div class="banner-title">{{ pageTitle }}</div>
            <div class="banner-sub">{{ pageSub }}</div>
          </div>
          <div class="banner-actions">
            <span class="banner-chip">{{ userStore.adminName || '审核员' }}</span>
          </div>
        </div>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import router from '../router'
import NotificationBell from '../components/NotificationBell.vue'

const userStore = useUserStore()
const route = useRoute()

const PAGE_META = {
  '/auditMain': { title: '成果审核', sub: '核对接单人提交的交付成果，通过后自动完成资金结算' }
}
const pageTitle = computed(() => PAGE_META[route.path]?.title || '审核工作台')
const pageSub = computed(() => PAGE_META[route.path]?.sub || '交付成果审核与结算')
const handleLogout = () => {
  ElMessage.success('已退出登录')
  userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background: var(--sidebar-gradient);
  box-shadow: 2px 0 14px rgba(8, 22, 41, .22);
  transition: width 0.3s;
}

.logo-box {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(255, 255, 255, .07);
}
.logo-box::before {
  content: '';
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: var(--gradient-primary);
  box-shadow: 0 4px 12px rgba(79, 107, 254, .5);
}

.layout-header {
  background: rgba(255, 255, 255, .85);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-light);
  box-shadow: 0 2px 10px rgba(16, 24, 40, .04);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-username {
  font-size: 14px;
  color: var(--text-regular);
  font-weight: 500;
}

.layout-main {
  background: var(--content-bg);
  padding: 20px;
}
</style>
