<template>
  <div class="profile-page">
    <el-container class="profile-container">
      <el-header class="page-header">
        <el-button @click="goBack" :icon="ArrowLeft" circle />
        <span class="header-title">用户信息</span>
      </el-header>

      <el-main class="page-main">
        <el-card v-loading="loading" class="profile-card" shadow="hover">
          <div v-if="user" class="profile-content">
            <!-- 头像 + 基本信息 -->
            <div class="user-header">
              <el-avatar :size="100" :src="resolveImagePath(user.imagePath)">
                {{ user.username ? user.username.charAt(0).toUpperCase() : 'U' }}
              </el-avatar>
              <div class="user-basic">
                <h2 class="username">{{ user.username || '用户' }}</h2>
                <p class="nickname" v-if="user.nickname">{{ user.nickname }}</p>
                <div class="user-actions">
                  <el-button type="primary" :icon="ChatDotRound" size="small" @click="sendPrivateMsg">
                    私信
                  </el-button>
                  <el-tag v-if="user.position" size="small" type="success" effect="plain">{{ user.position }}</el-tag>
                </div>
              </div>
            </div>

            <el-divider />

            <!-- 详情列表 -->
            <el-descriptions :column="2" border>
              <el-descriptions-item label="性别">{{ user.sex || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="年龄">{{ user.age || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ maskPhone(user.phone) }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ user.email || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="地址" :span="2">{{ user.address || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="注册时间" :span="2">{{ formatDate(user.registerDatetime) }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div v-else-if="!loading" class="empty-state">
            用户不存在
          </div>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatDotRound } from '@element-plus/icons-vue'
import { getProfileUser } from '../../api/user'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const user = ref(null)

const sendPrivateMsg = () => {
  if (!user.value) return
  if (!user.value.userId) {
    ElMessage.warning('该用户信息不完整，无法发起私信')
    return
  }
  router.push({
    path: '/clientP2PChat',
    query: {
      peerId: user.value.userId,
      peerName: user.value.username
    }
  })
}

const goBack = () => {
  router.back()
}

const formatDate = (d) => {
  if (!d) return '-'
  const dt = new Date(d)
  if (isNaN(dt.getTime())) return String(d)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

const maskPhone = (phone) => {
  if (!phone || phone.length < 11) return phone || '未填写'
  return phone.slice(0, 3) + '****' + phone.slice(7)
}

const resolveImagePath = (p) => {
  if (!p) return ''
  if (p.startsWith('http://') || p.startsWith('https://')) return p
  return `http://localhost:8888${p.startsWith('/') ? '' : '/'}${p}`
}

const loadUser = async () => {
  const username = route.params.username
  if (!username) {
    ElMessage.error('用户名缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    const res = await getProfileUser({ username })
    user.value = res.data || null
  } catch (err) {
    ElMessage.error(err.message || '加载用户信息失败')
    user.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadUser)
</script>

<style scoped>
.profile-container {
  height: 100vh;
  background: var(--content-bg);
}
.page-header {
  background: rgba(255, 255, 255, .85);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-light);
  box-shadow: 0 2px 10px rgba(16, 24, 40, .04);
  position: sticky;
  top: 0;
  z-index: 10;
}
.page-main {
  background: var(--content-bg);
  padding: 20px;
}
.profile-card {
  width: 680px;
  max-width: 100%;
  margin: 0 auto;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}
.header-title {
  margin-left: 12px;
  font-size: 18px;
  font-weight: bold;
}
.user-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
.empty-state {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.profile-content {
  padding: 10px 0;
}
.user-header {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 26px 24px;
  background: var(--gradient-primary);
  border-radius: var(--radius);
  box-shadow: 0 12px 28px -14px rgba(64, 128, 255, .6);
}
.user-header::after {
  content: '';
  position: absolute;
  right: -50px;
  top: -70px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .12);
  pointer-events: none;
}
.user-header :deep(.el-avatar) {
  position: relative;
  z-index: 1;
  border: 3px solid rgba(255, 255, 255, .9);
  box-shadow: 0 8px 20px rgba(8, 22, 41, .22);
}
.user-basic {
  position: relative;
  z-index: 1;
  flex: 1;
}
.username {
  margin: 0 0 4px;
  font-size: 22px;
  color: #fff;
}
.nickname {
  margin: 0 0 10px;
  color: rgba(255, 255, 255, .8);
  font-size: 14px;
}
.user-header :deep(.el-button--primary) {
  background: #fff !important;
  color: var(--primary) !important;
  box-shadow: 0 4px 12px rgba(8, 22, 41, .18);
}
</style>
