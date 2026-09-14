<template>
  <div class="profile-page">
    <!-- 顶部返回 -->
    <div class="profile-header">
      <el-card class="header-card" shadow="never">
        <div class="header-inner">
          <el-button type="primary" text @click="goBack">
            <el-icon style="margin-right:4px"><ArrowLeft /></el-icon>返回大厅
          </el-button>
          <h2 class="page-title">个人中心</h2>
          <div>
            <el-button @click="goMyOrders">
              <el-icon style="margin-right:4px"><List /></el-icon>我的订单
            </el-button>
            <el-button type="primary" plain @click="goResetPwd" style="margin-left:8px">
              <el-icon style="margin-right:4px"><Lock /></el-icon>修改密码
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <div class="profile-body">
      <el-row :gutter="24">
        <!-- 左侧头像区 -->
        <el-col :xs="24" :md="8">
          <el-card shadow="hover" class="avatar-card">
            <div class="avatar-wrapper">
              <el-upload
                class="avatar-uploader"
                action=""
                :show-file-list="false"
                :auto-upload="false"
                :on-change="handleAvatarChange"
                accept="image/*"
              >
                <img v-if="avatarPreview" :src="avatarPreview" class="avatar-img" />
                <el-avatar v-else :size="120" :src="DEFAULT_AVATAR" />
              </el-upload>
              <p class="upload-hint">点击头像更换</p>
            </div>
            <div class="user-summary">
              <p class="summary-name">{{ form.username || '用户' }}</p>
              <p class="summary-role" v-if="userStore.role">{{ userStore.role }}</p>
              <p class="summary-phone">{{ form.phone }}</p>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧信息表单 -->
        <el-col :xs="24" :md="16">
          <el-card shadow="hover" class="info-card">
            <template #header>
              <div class="card-header">
                <span class="section-title">基本信息</span>
                <el-tag size="small" :type="editing ? 'warning' : 'info'">{{ editing ? '编辑中' : '只读模式' }}</el-tag>
              </div>
            </template>

            <el-form :model="form" label-width="90px" class="info-form">
              <el-form-item label="用户名">
                <el-input v-model="form.username" :disabled="!editing" clearable />
              </el-form-item>

              <el-form-item label="昵称">
                <el-input v-model="form.nickname" :disabled="!editing" clearable />
              </el-form-item>

              <el-form-item label="手机号">
                <el-input v-model="form.phone" disabled />
              </el-form-item>

              <el-row :gutter="10">
                <el-col :span="12">
                  <el-form-item label="性别">
                    <el-select v-model="form.sex" :disabled="!editing" placeholder="请选择性别" style="width: 100%">
                      <el-option label="男" value="男" />
                      <el-option label="女" value="女" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="年龄">
                    <el-input-number v-model="form.age" :min="0" :max="150" :disabled="!editing" controls-position="right" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="邮箱">
                <el-input v-model="form.email" :disabled="!editing" placeholder="请输入邮箱" clearable />
              </el-form-item>

              <el-form-item label="地址">
                <el-input v-model="form.address" :disabled="!editing" placeholder="请输入地址" clearable />
              </el-form-item>

              <el-form-item label="注册时间">
                <el-input :model-value="formatDate(form.registerDatetime)" disabled />
              </el-form-item>

              <el-form-item>
                <el-button v-if="!editing" type="primary" @click="startEdit">
                  <el-icon style="margin-right:4px"><Edit /></el-icon>修改信息
                </el-button>
                <template v-else>
                  <el-button type="primary" :loading="saving" @click="doSave">
                    <el-icon style="margin-right:4px"><Check /></el-icon>保存修改
                  </el-button>
                  <el-button @click="cancelEdit">取消</el-button>
                </template>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfo, updateUser } from '../../api/user'
import { useUserStore, DEFAULT_AVATAR } from '../../stores/user'
import { ArrowLeft, Edit, Check, List, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const avatarFile = ref(null)
const avatarPreview = ref('')

const originalData = ref({})
const form = reactive({
  username: '',
  nickname: '',
  phone: '',
  sex: '',
  age: null,
  email: '',
  address: '',
  imagePath: '',
  registerDatetime: null
})

const goBack = () => {
  router.push('/clientMain')
}

const goMyOrders = () => {
  router.push('/clientMyOrders')
}

const goResetPwd = () => {
  router.push('/clientResetPassword')
}

const formatDate = (d) => {
  if (!d) return '-'
  const dt = new Date(d)
  if (isNaN(dt.getTime())) return String(d)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

const loadUserInfo = async () => {
  loading.value = true
  try {
    const res = await getUserInfo()
    const data = res.data || {}
    Object.assign(form, {
      username: data.username || '',
      nickname: data.nickname || '',
      phone: data.phone || '',
      sex: data.sex || '',
      age: data.age ?? null,
      email: data.email || '',
      address: data.address || '',
      imagePath: data.imagePath || '',
      registerDatetime: data.registerDatetime || null
    })
    // 回退:如果store里有username但form里没有,用store的
    if (!form.username && userStore.username) form.username = userStore.username
    // 头像预览
    avatarPreview.value = form.imagePath ? resolveImagePath(form.imagePath) : ''
    // 同步store
    if (form.username) userStore.setUsername(form.username)
    if (form.imagePath) userStore.setImagePath(form.imagePath)
    // 保存原始数据供取消时还原
    originalData.value = JSON.parse(JSON.stringify(form))
  } catch (err) {
    ElMessage.error(err.message || '加载用户信息失败')
  } finally {
    loading.value = false
  }
}

// 处理后端返回的相对路径
const resolveImagePath = (p) => {
  if (!p) return ''
  if (p.startsWith('http://') || p.startsWith('https://')) return p
  // Vite代理 /user /order已经配了,图片一般走静态资源或后端/static,所以加完整后端地址
  return `http://localhost:8888${p.startsWith('/') ? '' : '/'}${p}`
}

const handleAvatarChange = (upFile) => {
  avatarFile.value = upFile.raw
  avatarPreview.value = URL.createObjectURL(upFile.raw)
}

const startEdit = () => {
  editing.value = true
}

const cancelEdit = () => {
  Object.assign(form, JSON.parse(JSON.stringify(originalData.value)))
  avatarFile.value = null
  avatarPreview.value = form.imagePath ? resolveImagePath(form.imagePath) : ''
  editing.value = false
}

const doSave = async () => {
  if (!form.username) { ElMessage.error('用户名不能为空'); return }

  saving.value = true
  try {
    const fd = new FormData()
    // User字段平铺到FormData(Spring可直接从multipart表单绑定到@RequestParam User属性)
    fd.append('username', form.username)
    fd.append('nickname', form.nickname)
    if (form.sex) fd.append('sex', form.sex)
    fd.append('age', form.age ?? 0)
    if (form.email) fd.append('email', form.email)
    if (form.address) fd.append('address', form.address)
    if (form.imagePath) fd.append('imagePath', form.imagePath)
    // 客户端调用:adminUserId传0(或不传),后端用Integer才能不报错,这里传0兼容后端int primitive声明
    fd.append('adminUserId', 0)
    if (avatarFile.value) {
      fd.append('avatar', avatarFile.value)
    }
    const res = await updateUser(fd)
    // 更新store
    if (form.username) userStore.setUsername(form.username)
    const updatedUser = res.data
    if (updatedUser && updatedUser.imagePath) {
      userStore.setImagePath(updatedUser.imagePath)
      form.imagePath = updatedUser.imagePath
      avatarPreview.value = resolveImagePath(updatedUser.imagePath)
    }
    avatarFile.value = null
    originalData.value = JSON.parse(JSON.stringify(form))
    editing.value = false
    ElMessage.success('个人信息修改成功')
  } catch (err) {
    ElMessage.error(err.message || '修改失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.profile-header {
  padding: 16px 24px 0;
}
.header-card {
  border-radius: 12px;
}
.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.profile-body {
  padding: 20px 24px 40px;
}

.avatar-card {
  border-radius: 12px;
  margin-bottom: 24px;
}
.avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0 20px;
}
.avatar-uploader {
  cursor: pointer;
}
.avatar-img {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  border: 2px solid #f0f2f5;
}
.upload-hint {
  color: #909399;
  font-size: 12px;
  margin-top: 10px;
}
.user-summary {
  text-align: center;
  margin-top: 8px;
}
.summary-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 8px 0 4px;
}
.summary-role {
  display: inline-block;
  margin: 0 auto 4px;
  color: #409EFF;
  font-size: 13px;
}
.summary-phone {
  color: #909399;
  font-size: 13px;
  margin: 0;
}

.info-card {
  border-radius: 12px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.info-form {
  padding: 10px 10px 0;
}
:deep(.el-form-item) {
  margin-bottom: 20px;
}
</style>
