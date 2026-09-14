<template>
  <el-container style="height: 100vh; border: 1px solid #eee">
    <!-- 侧边栏 -->
    <el-aside width="220px" style="background-color: #001529">
      <div class="logo-box">后台管理系统</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/adminMain">
          <template #icon><House /></template>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/adminUser">
          <template #icon><User /></template>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/adminFeedback">
          <template #icon><Message /></template>
          <span>问题反馈</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header style="background: #fff; display: flex; justify-content: flex-end; align-items: center; padding: 0 20px; border-bottom: 1px solid #e4e7ed; gap: 16px">
        <NotificationBell />
        <span>{{ userStore.adminName }}</span>
        <el-button type="danger" icon="Logout" @click="handleLogout">退出登录</el-button>
      </el-header>

      <!-- 页面内容区域 -->
      <el-main style="background: #f5f7fa; padding: 20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import router from '../router'
import NotificationBell from '../components/NotificationBell.vue'

const userStore = useUserStore()
const handleLogout = () => {
  ElMessage.success('已退出登录')
  userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.logo-box {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}
</style>