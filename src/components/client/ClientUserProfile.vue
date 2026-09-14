<template>
  <div class="profile-page">
    <el-container style="height: 100vh; background: #f5f7fa">
      <el-header style="background: #fff; display: flex; align-items: center; padding: 0 20px; border-bottom: 1px solid #e4e7ed">
        <el-button @click="goBack" :icon="ArrowLeft" circle />
        <span style="margin-left: 12px; font-size: 18px; font-weight: bold">用户信息</span>
      </el-header>

      <el-main style="padding: 20px; display: flex; justify-content: center">
        <el-card v-loading="loading" style="width: 680px" shadow="hover">
          <div v-if="user" class="profile-content">
            <!-- 头像 + 基本信息 -->
            <div class="user-header">
              <el-avatar :size="100" :src="resolveImagePath(user.imagePath)">
                {{ user.username ? user.username.charAt(0).toUpperCase() : 'U' }}
              </el-avatar>
              <div class="user-basic">
                <h2 class="username">{{ user.username || '用户' }}</h2>
                <p class="nickname" v-if="user.nickname">{{ user.nickname }}</p>
                <div style="margin-top: 8px; display: flex; gap: 8px">
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
          <div v-else-if="!loading" style="text-align: center; padding: 40px; color: #909399">
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
.profile-content {
  padding: 10px 0;
}
.user-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 20px;
}
.user-basic {
  flex: 1;
}
.username {
  margin: 0 0 4px;
  font-size: 22px;
  color: #303133;
}
.nickname {
  margin: 0 0 8px;
  color: #909399;
  font-size: 14px;
}
</style>
