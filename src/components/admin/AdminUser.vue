<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span>用户管理</span>
        <div>
          <el-button type="primary" size="small" @click="openAddDialog">
            <el-icon class="btn-icon"><Plus /></el-icon>新增用户
          </el-button>
          <el-button type="primary" link size="small" @click="loadUsers">
            <el-icon class="btn-icon"><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </div>
    </template>

    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="用户名 / 昵称 / 手机号 / 邮箱"
        clearable
        class="keyword-input"
        :prefix-icon="Search"
      />
      <el-select v-model="stateFilter" placeholder="账号状态" clearable class="state-select">
        <el-option label="正常" value="0" />
        <el-option label="冻结" value="1" />
      </el-select>
    </div>

    <!-- 用户表格 -->
    <el-table :data="filteredUsers" border stripe v-loading="loading" class="full-width">
      <el-table-column label="用户ID" prop="userId" width="80" align="center" />
      <el-table-column label="用户名" prop="username" width="120" />
      <el-table-column label="昵称" prop="nickname" width="120">
        <template #default="{ row }">{{ row.nickname || '—' }}</template>
      </el-table-column>
      <el-table-column label="性别" width="70" align="center">
        <template #default="{ row }">{{ row.sex || '—' }}</template>
      </el-table-column>
      <el-table-column label="年龄" width="65" align="center">
        <template #default="{ row }">{{ row.age > 0 ? row.age : '—' }}</template>
      </el-table-column>
      <el-table-column label="手机号" prop="phone" width="120" />
      <el-table-column label="邮箱" prop="email" min-width="160">
        <template #default="{ row }">{{ row.email || '—' }}</template>
      </el-table-column>
      <el-table-column label="地址" prop="address" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.address || '—' }}</template>
      </el-table-column>
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag
            v-for="r in (roleMap[row.userId] || [])"
            :key="r"
            :type="roleTagType(r)"
            size="small"
            class="role-tag"
          >{{ r }}</el-tag>
          <el-button link type="primary" size="small" @click="openRoleDialog(row)">管理角色</el-button>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="155" align="center">
        <template #default="{ row }">{{ formatDate(row.registerDatetime) }}</template>
      </el-table-column>
      <el-table-column label="账号状态" width="110" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.state !== '1'"
            active-text="正常"
            inactive-text="冻结"
            @change="(val) => handleStateChange(row, val)"
          />
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && filteredUsers.length === 0" description="暂无用户" />

    <!-- 角色管理弹窗 -->
    <el-dialog v-model="roleDialogVisible" title="角色管理" width="460px" destroy-on-close>
      <div v-if="currentUser" class="dialog-tip">
        用户：<b>{{ currentUser.username }}</b>（{{ currentUser.phone }}）
      </div>
      <div v-loading="roleLoading">
        <div class="role-list">
          <el-tag
            v-for="r in currentRoles"
            :key="r"
            :type="roleTagType(r)"
            closable
            class="role-list-tag"
            @close="handleRemoveRole(r)"
          >{{ r }}</el-tag>
          <span v-if="currentRoles.length === 0" class="muted-text">暂无角色</span>
        </div>
        <el-divider class="role-divider" />
        <div class="role-add-row">
          <el-select v-model="selectedRoleId" placeholder="选择要添加的角色" class="flex-1">
            <el-option
              v-for="opt in availableRoleOptions"
              :key="opt.roleId"
              :label="opt.name"
              :value="opt.roleId"
            />
          </el-select>
          <el-button type="primary" :disabled="!selectedRoleId" @click="handleAddRole">添加</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="addDialogVisible" title="新增用户" width="620px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="addForm.phone" placeholder="登录账号（唯一）" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="addForm.username" placeholder="显示名称" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="addForm.password" show-password placeholder="至少 6 位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认角色" prop="defaultRoleId">
              <el-select v-model="addForm.defaultRoleId" class="full-width">
                <el-option v-for="r in ROLE_OPTIONS" :key="r.roleId" :label="r.name" :value="r.roleId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称">
              <el-input v-model="addForm.nickname" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="性别">
              <el-select v-model="addForm.sex" placeholder="请选择" class="full-width">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="年龄">
              <el-input v-model="addForm.age" placeholder="0-120" class="full-width" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="addForm.email" placeholder="xxx@xx.com" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="addForm.address" placeholder="可留空" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-radio-group v-model="addForm.state">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">冻结</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAddUser">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  getAllUsers, updateUserState, getUserRoles,
  addUserRole, deleteUserRole, addUser
} from '../../api/admin'

// 角色字典：roleId → 角色名（与 t_role 一致）
const ROLE_OPTIONS = [
  { roleId: 1, name: '管理员' },
  { roleId: 2, name: '审核员' },
  { roleId: 3, name: '客服' },
  { roleId: 4, name: '普通用户' }
]

const loading = ref(false)
const users = ref([])
const keyword = ref('')
const stateFilter = ref('')
const roleMap = ref({}) // userId -> [角色名]

const roleDialogVisible = ref(false)
const roleLoading = ref(false)
const currentUser = ref(null)
const currentRoles = ref([])
const selectedRoleId = ref(null)

const addDialogVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref(null)
const addForm = ref({
  phone: '', username: '', password: '', defaultRoleId: 4,
  nickname: '', sex: '', age: '', email: '', address: '', state: '0'
})
const addRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^\d{11}$/, message: '手机号应为 11 位数字', trigger: 'blur' }
  ],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  defaultRoleId: [{ required: true, message: '请选择默认角色', trigger: 'change' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const formatDate = (d) => {
  if (!d) return '—'
  const dt = new Date(d)
  if (isNaN(dt.getTime())) return String(d)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

const roleTagType = (roleName) => {
  if (roleName.includes('管理员')) return 'danger'
  if (roleName.includes('审核员')) return 'warning'
  if (roleName.includes('客服')) return 'success'
  return 'info'
}

const filteredUsers = computed(() => {
  let list = users.value
  const kw = keyword.value.trim()
  if (kw) {
    list = list.filter(u =>
      (u.username || '').includes(kw) ||
      (u.nickname || '').includes(kw) ||
      (u.phone || '').includes(kw) ||
      (u.email || '').includes(kw)
    )
  }
  if (stateFilter.value === '0') {
    list = list.filter(u => u.state !== '1')
  } else if (stateFilter.value === '1') {
    list = list.filter(u => u.state === '1')
  }
  return list
})

const availableRoleOptions = computed(() => {
  return ROLE_OPTIONS.filter(opt => !currentRoles.value.includes(opt.name))
})

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getAllUsers()
    users.value = res.data || []
    // 并行加载每个用户的角色名
    await Promise.all(users.value.map(async (u) => {
      try {
        const r = await getUserRoles({ userId: u.userId })
        roleMap.value[u.userId] = r.data || []
      } catch {
        roleMap.value[u.userId] = []
      }
    }))
  } catch (err) {
    ElMessage.error(err.message || '加载用户失败')
    users.value = []
  } finally {
    loading.value = false
  }
}

const handleStateChange = (row, enabled) => {
  const state = enabled ? '0' : '1'
  const action = enabled ? '解冻' : '冻结'
  ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？${enabled ? '' : '冻结后该用户将无法登录。'}`,
    `${action}确认`, { type: 'warning', confirmButtonText: `确定${action}`, cancelButtonText: '取消' }
  ).then(async () => {
    try {
      await updateUserState({ userId: row.userId, state })
      row.state = state
      ElMessage.success(`已${action}`)
    } catch (err) {
      ElMessage.error(err.message || `${action}失败`)
    }
  }).catch(() => {})
}

const openRoleDialog = async (row) => {
  currentUser.value = row
  selectedRoleId.value = null
  roleDialogVisible.value = true
  roleLoading.value = true
  try {
    const res = await getUserRoles({ userId: row.userId })
    currentRoles.value = res.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载角色失败')
    currentRoles.value = []
  } finally {
    roleLoading.value = false
  }
}

const handleAddRole = async () => {
  try {
    await addUserRole({ userId: currentUser.value.userId, roleId: selectedRoleId.value })
    const opt = ROLE_OPTIONS.find(o => o.roleId === selectedRoleId.value)
    if (opt && !currentRoles.value.includes(opt.name)) {
      currentRoles.value.push(opt.name)
    }
    roleMap.value[currentUser.value.userId] = [...currentRoles.value]
    ElMessage.success('角色添加成功')
    selectedRoleId.value = null
  } catch (err) {
    ElMessage.error(err.message || '添加角色失败')
  }
}

const handleRemoveRole = async (roleName) => {
  const opt = ROLE_OPTIONS.find(o => o.name === roleName)
  if (!opt) return
  try {
    await deleteUserRole({ userId: currentUser.value.userId, roleId: opt.roleId })
    currentRoles.value = currentRoles.value.filter(r => r !== roleName)
    roleMap.value[currentUser.value.userId] = [...currentRoles.value]
    ElMessage.success('角色已移除')
  } catch (err) {
    ElMessage.error(err.message || '移除角色失败')
  }
}

const openAddDialog = () => {
  addForm.value = {
    phone: '', username: '', password: '', defaultRoleId: 4,
    nickname: '', sex: '', age: '', email: '', address: '', state: '0'
  }
  addDialogVisible.value = true
}

const handleAddUser = async () => {
  let valid = true
  await addFormRef.value.validate((ok) => { valid = ok })
  if (!valid) return

  addLoading.value = true
  try {
    const f = addForm.value
    const formData = new FormData()
    formData.append('phone', f.phone)
    formData.append('username', f.username)
    formData.append('password', f.password)
    if (f.nickname) formData.append('nickname', f.nickname)
    if (f.sex) formData.append('sex', f.sex)
    formData.append('state', f.state || '0')
    const ageNum = parseInt(f.age)
    if (!isNaN(ageNum) && ageNum >= 0 && ageNum <= 120) {
      formData.append('age', String(ageNum))
    }
    if (f.email) formData.append('email', f.email)
    if (f.address) formData.append('address', f.address)

    await addUser(formData)
    ElMessage.success('用户新增成功')

    // 如果选的不是默认普通用户，追加角色（后端默认只会加 roleId=4）
    if (f.defaultRoleId && f.defaultRoleId !== 4) {
      try {
        const allRes = await getAllUsers()
        const allUsers = allRes.data || []
        const newUser = allUsers.find(u => u.phone === f.phone)
        if (newUser) {
          await addUserRole({ userId: newUser.userId, roleId: f.defaultRoleId })
          ElMessage.success(`已分配角色：${ROLE_OPTIONS.find(r => r.roleId === f.defaultRoleId)?.name}`)
        }
      } catch { /* 角色分配失败不影响用户创建 */ }
    }

    addDialogVisible.value = false
    loadUsers()
  } catch (err) {
    ElMessage.error(err.message || '新增失败')
  } finally {
    addLoading.value = false
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}
.btn-icon {
  margin-right: 4px;
}
.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
}
.keyword-input {
  width: 280px;
}
.state-select {
  width: 140px;
}
.full-width {
  width: 100%;
}
.role-tag {
  margin-right: 6px;
}
.dialog-tip {
  margin-bottom: 16px;
}
.role-list {
  margin-bottom: 12px;
}
.role-list-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}
.muted-text {
  color: #909399;
}
.role-divider {
  margin: 8px 0;
}
.role-add-row {
  display: flex;
  gap: 10px;
  align-items: center;
}
.flex-1 {
  flex: 1;
}
</style>
