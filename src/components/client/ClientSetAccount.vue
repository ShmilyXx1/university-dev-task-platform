<template>
  <div class="reset-page">
    <transition name="fade">
      <div v-show="isShow" class="error-tip">
        <el-icon class="tip-icon"><CircleCloseFilled /></el-icon>
        <span>{{ errMessage }}</span>
      </div>
    </transition>

    <el-card class="login-card" shadow="hover">
      <div class="card-header">
        <el-icon color="#409EFF" size="42"><Key /></el-icon>
        <h1 class="login-title">设置账号</h1>
      </div>

      <el-form label-width="0px" class="login-form">
        <el-input type="hidden" v-model="phone" />

        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" prefix-icon="User" clearable />
        </el-form-item>

        <el-form-item>
          <el-input v-model="form.nickname" placeholder="请输入昵称" size="large" clearable />
        </el-form-item>

        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" prefix-icon="Lock" show-password clearable />
        </el-form-item>

        <el-form-item>
          <el-input v-model="form.againPassword" type="password" placeholder="请输入确认密码" size="large" prefix-icon="Lock" show-password clearable />
        </el-form-item>

        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item>
              <el-select v-model="form.sex" placeholder="性别" size="large" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item>
              <el-input-number v-model="form.age" :min="0" :max="150" size="large" placeholder="年龄" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large" clearable />
        </el-form-item>

        <el-form-item>
          <el-input v-model="form.address" placeholder="请输入地址" size="large" clearable />
        </el-form-item>

        <el-form-item>
          <el-upload
            class="avatar-uploader"
            action=""
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleAvatarChange"
            accept="image/*"
          >
            <img v-if="avatarUrl" :src="avatarUrl" class="avatar-image" />
            <img v-else :src="DEFAULT_AVATAR" class="avatar-image" />
          </el-upload>
          <div class="upload-tip">点击上传头像(可选)</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="submitting" @click="doRegister">注 册</el-button>
        </el-form-item>
      </el-form>

      <div class="link-row">
        <router-link to="/clientRegister" class="back-link">← 返回验证码页</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../../api/user'
import { CircleCloseFilled, Key, User, Lock, Plus } from '@element-plus/icons-vue'
import { DEFAULT_AVATAR } from '../../stores/user'

const router = useRouter()
const route = useRoute()

const phone = ref(route.query.phone)
const avatarUrl = ref('')
const avatarFile = ref(null)
const submitting = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  againPassword: '',
  sex: '',
  age: null,
  email: '',
  address: ''
})

const errMessage = ref('')
const isShow = ref(false)

const showError = (msg) => {
  errMessage.value = msg
  isShow.value = true
  setTimeout(() => { isShow.value = false }, 2000)
}

const handleAvatarChange = (uploadFile) => {
  avatarFile.value = uploadFile.raw
  avatarUrl.value = URL.createObjectURL(uploadFile.raw)
}

const doRegister = async () => {
  if (!form.username) { showError('请输入用户名'); return }
  if (!form.password) { showError('请输入密码'); return }
  if (!form.againPassword) { showError('请输入确认密码'); return }
  if (form.password !== form.againPassword) { showError('两次密码输入不一致'); return }

  submitting.value = true
  try {
    const userObj = {
      phone: phone.value,
      username: form.username,
      nickname: form.nickname,
      password: form.password,
      sex: form.sex || null,
      age: form.age || 0,
      email: form.email || null,
      address: form.address || null
    }
    await register(userObj)
    ElMessage.success('注册成功,请登录')
    router.push('/clientLogin')
  } catch (err) {
    showError(err.message || '注册失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.reset-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4ff 0%, #e6edff 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
}

.error-tip {
  position: fixed;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
  padding: 10px 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 999;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.login-card {
  width: 520px;
  border-radius: 16px;
  padding: 35px 30px;
  background-color: #ffffff;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 30px;
}
.login-title {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.login-form {
  margin-bottom: 20px;
}
:deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  border-radius: 8px;
}

.link-row {
  text-align: right;
}
.back-link {
  color: #409EFF;
  font-size: 14px;
  text-decoration: none;
}
.back-link:hover {
  text-decoration: underline;
}

.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
}
.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409EFF;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}
.avatar-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  display: block;
}
.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
  text-align: left;
}
</style>
