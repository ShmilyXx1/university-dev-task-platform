<template>
  <el-container class="page-container">
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="page-header">
        <span class="brand">
          <span class="brand-logo"><el-icon><UserFilled /></el-icon></span>
          <span class="page-title">软件开发接单客户端</span>
        </span>

        <div class="header-right">
          <NotificationBell />
          <!-- 右上角头像用户名下拉菜单 -->
          <el-dropdown @command="handleDropdownCommand">
            <span class="user-header">
              <el-avatar :size="36" :src="userStore.avatarUrl" class="user-avatar">{{ userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U' }}</el-avatar>
              <span class="user-name">{{ userStore.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="myOrders">我的订单</el-dropdown-item>
                <el-dropdown-item command="chat">联系客服</el-dropdown-item>
                <el-dropdown-item command="feedback">问题反馈</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 页面内容区域 -->
      <el-main class="page-main">
        <!-- Hero 欢迎区 -->
        <div class="hero-card">
          <div class="hero-deco" aria-hidden="true"></div>
          <div class="hero-left">
            <el-avatar :size="62" :src="userStore.avatarUrl" class="hero-avatar">
              {{ userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U' }}
            </el-avatar>
            <div class="hero-info">
              <div class="hero-greeting">欢迎回来，<b>{{ userStore.username || '用户' }}</b></div>
              <div class="hero-sub">悬赏任务客户端 · 大厅待接订单</div>
            </div>
          </div>
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-num">{{ orderList.length }}</span>
              <span class="stat-label">待接取</span>
            </div>
          </div>
        </div>

        <!-- 大厅订单列表，仅显示未接取的订单 -->
        <el-card shadow="hover">
          <template #header>
            <div class="card-header-bar">
              <div class="card-header-left">
                <span class="card-title">大厅 - 待接取订单</span>
                <el-button type="primary" :icon="Plus" @click="goAddOrder">发布订单</el-button>
              </div>
              <div class="card-header-right">
                <!-- 搜索框 -->
                <el-input
                  v-model="searchKeyword"
                  placeholder="按订单ID/标题/内容搜索/订单类型/用户名"
                  clearable
                  class="search-input"
                  @keyup.enter="doSearch"
                  @clear="resetAll"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" @click="doSearch">
                  <el-icon class="btn-icon"><Search /></el-icon>搜索
                </el-button>
                <el-button @click="resetAll">
                  <el-icon class="btn-icon"><RefreshLeft /></el-icon>重置
                </el-button>
              </div>
            </div>
          </template>

          <!-- 筛选面板 -->
          <div class="filter-panel">
            <div class="filter-row">
              <span class="filter-label">筛选:</span>

              <!-- 类型 -->
              <el-select
                v-model="filter.type"
                placeholder="订单类型"
                clearable
                class="filter-select"
                @change="doFilter"
              >
                <el-option label="编程" value="编程"/>
                <el-option label="学习" value="学习"/>
                <el-option label="设计" value="设计"/>
                <el-option label="翻译" value="翻译"/>
                <el-option label="其他" value="其他"/>
              </el-select>

              <!-- 最低价 -->
              <el-input-number
                v-model="filter.minPrice"
                :min="0"
                :precision="2"
                placeholder="最低价"
                controls-position="right"
                size="default"
              />

              <!-- 最高价 -->
              <el-input-number
                v-model="filter.maxPrice"
                :min="0"
                :precision="2"
                placeholder="最高价"
                controls-position="right"
                size="default"
              />

              <!-- 排序切换按钮: 未点击(默认) → 点击升序asc → 再点降序desc -->
              <el-button
                :type="sortState === 'none' ? 'default' : 'success'"
                :icon="sortIcon"
                @click="cycleSort"
              >
                按发布价{{ sortLabel }}
              </el-button>

              <el-button type="primary" plain @click="doFilter">
                <el-icon class="btn-icon"><Filter /></el-icon>应用筛选
              </el-button>
            </div>

            <!-- 当前筛选信息 -->
            <div v-if="activeFilters" class="active-filters">
              <el-tag size="small" effect="plain">{{ activeFilters }}</el-tag>
              <el-button link type="primary" size="small" @click="clearFilter">清除筛选</el-button>
            </div>
          </div>

          <el-table :data="orderList" border stripe v-loading="loading" class="order-table">
            <el-table-column prop="orderId" label="订单ID" width="80"/>
            <el-table-column prop="title" label="订单标题"/>
            <el-table-column prop="content" label="订单内容" show-overflow-tooltip/>
            <el-table-column prop="type" label="订单类型" width="110"/>
            <el-table-column prop="senderPrice" label="发布方报价" width="110"/>
            <el-table-column prop="deposit" label="押金" width="100">
              <template #default="scope">
                {{ scope.row.deposit != null ? scope.row.deposit : '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="senderName" label="发布人" width="110">
              <template #default="scope">
                <el-button v-if="scope.row.senderName" link type="primary" @click.stop="goUserProfile(scope.row.senderName)">
                  {{ scope.row.senderName }}
                </el-button>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column prop="getterName" label="接单人" width="110">
              <template #default="scope">
                <el-button v-if="scope.row.getterName" link type="primary" @click.stop="goUserProfile(scope.row.getterName)">
                  {{ scope.row.getterName }}
                </el-button>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="订单状态" width="120">
              <template #default="scope">
                <el-tag :type="stateTagType(scope.row.state)">
                  {{ stateText(scope.row.state) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发布时间" width="170">
              <template #default="scope">
                {{ formatDate(scope.row.releaseDatetime) }}
              </template>
            </el-table-column>
            <el-table-column label="结束时间" width="170">
              <template #default="scope">
                {{ formatDate(scope.row.endDatetime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button type="primary" link @click="goDetail(scope.row.orderId)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'
import { ArrowDown, UserFilled, Plus, Search, RefreshLeft, Filter, Sort, Bottom, Top } from '@element-plus/icons-vue'
import router from '../../router'
import { selectAllOrder, searchOrder, filterOrders } from '../../api/order'
import NotificationBell from '../../components/NotificationBell.vue'

const userStore = useUserStore()
const loading = ref(false)
const orderList = ref([])

// 订单状态映射: 0=待被接取 1=已被接取 2=完成订单 3=接单人取消
const STATE_MAP = {
  '0': { text: '待被接取', type: 'warning' },
  '1': { text: '已被接取', type: 'primary' },
  '2': { text: '完成订单', type: 'success' },
  '3': { text: '接单人取消', type: 'info' }
}
const stateText = (s) => STATE_MAP[String(s)]?.text ?? '未知状态'
const stateTagType = (s) => STATE_MAP[String(s)]?.type ?? 'danger'

// ============ 搜索 + 筛选 状态 ============
const searchKeyword = ref('')
// 类型/最低价/最高价 + sort 三态: 'none' 默认(不发sort) | 'asc' 升序 | 'desc' 降序
const filter = reactive({
  type: '',
  minPrice: null,
  maxPrice: null
})
const sortState = ref('none') // none -> asc -> desc 循环

const cycleSort = () => {
  if (sortState.value === 'none') sortState.value = 'asc'
  else if (sortState.value === 'asc') sortState.value = 'desc'
  else sortState.value = 'none'
}
const sortLabel = computed(() => {
  if (sortState.value === 'asc') return '升序'
  if (sortState.value === 'desc') return '降序'
  return ''
})
const sortIcon = computed(() => {
  if (sortState.value === 'asc') return Top
  if (sortState.value === 'desc') return Bottom
  return Sort
})

// 当前激活的筛选条件文字说明
const activeFilters = computed(() => {
  const parts = []
  if (searchKeyword.value) parts.push(`搜索关键字: ${searchKeyword.value}`)
  if (filter.type) parts.push(`类型: ${filter.type}`)
  if (filter.minPrice != null) parts.push(`最低价: ${filter.minPrice}`)
  if (filter.maxPrice != null) parts.push(`最高价: ${filter.maxPrice}`)
  if (sortState.value !== 'none') parts.push(`发布价${sortState.value === 'asc' ? '升序' : '降序'}`)
  return parts.join(' | ')
})

// ============ 核心:搜索/筛选/重置 方法 ============
// 统一的 post-process:过滤只展示 state=0(未接取)
const postProcess = (data) => {
  const list = data || []
  orderList.value = list.filter(o => String(o.state) === '0')
}

const getAllOrder = async () => {
  loading.value = true
  try {
    const res = await selectAllOrder()
    postProcess(res.data)
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const doSearch = async () => {
  if (!searchKeyword.value.trim()) {
    // 没关键字就回到默认列表
    return getAllOrder()
  }
  loading.value = true
  try {
    const res = await searchOrder({ searchNum: searchKeyword.value.trim() })
    postProcess(res.data)
    if (!orderList.value.length) {
      ElMessage.info('未搜索到匹配订单')
    }
  } catch (err) {
    ElMessage.error(err.message || '搜索失败')
  } finally {
    loading.value = false
  }
}

const doFilter = async () => {
  loading.value = true
  try {
    // 价格校验
    if (filter.minPrice != null && filter.maxPrice != null && filter.minPrice > filter.maxPrice) {
      ElMessage.warning('最低价不能大于最高价')
      loading.value = false
      return
    }
    const params = {}
    if (filter.type) params.type = filter.type
    if (filter.minPrice != null) params.minPrice = filter.minPrice
    if (filter.maxPrice != null) params.maxPrice = filter.maxPrice
    if (sortState.value !== 'none') params.sort = sortState.value // asc / desc
    if (!Object.keys(params).length) {
      // 没条件就全量
      return getAllOrder()
    }
    const res = await filterOrders(params)
    postProcess(res.data)
    if (!orderList.value.length) {
      ElMessage.info('当前筛选条件无匹配订单')
    }
  } catch (err) {
    ElMessage.error(err.message || '筛选失败')
  } finally {
    loading.value = false
  }
}

const clearFilter = () => {
  filter.type = ''
  filter.minPrice = null
  filter.maxPrice = null
  sortState.value = 'none'
  if (searchKeyword.value.trim()) {
    doSearch()
  } else {
    getAllOrder()
  }
}

const resetAll = () => {
  searchKeyword.value = ''
  filter.type = ''
  filter.minPrice = null
  filter.maxPrice = null
  sortState.value = 'none'
  getAllOrder()
}

// 日期格式化函数，后端返回Date对象/时间戳都兼容
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')} ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`
}

// 下拉菜单点击事件
const handleDropdownCommand = (command) => {
  if(command === 'profile'){
    router.push('/clientProfile')
  }else if(command === 'myOrders'){
    router.push('/clientMyOrders')
  }else if(command === 'feedback'){
    router.push('/clientFeedback')
  }else if(command === 'chat'){
    router.push('/clientChat')
  }else if(command === 'logout'){
    handleLogout()
  }
}

const handleLogout = () => {
  ElMessage.success('已退出登录')
  userStore.logout()
  router.push('/clientLogin')
}

const goAddOrder = () => {
  router.push('/clientAddOrder')
}

const goDetail = (orderId) => {
  router.push({ path: `/clientOrderDetail/${orderId}`, query: { from: 'hall' } })
}

const goUserProfile = (username) => {
  if (!username) return
  router.push(`/clientUserProfile/${encodeURIComponent(username)}`)
}

onMounted(()=>{
  getAllOrder()
})

</script>

<style scoped>
.page-container {
  height: 100vh;
}

.page-header {
  background: rgba(255, 255, 255, .85);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-light);
  box-shadow: 0 2px 10px rgba(16, 24, 40, .04);
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--gradient-primary);
  color: #fff;
  font-size: 16px;
  box-shadow: 0 4px 10px -2px rgba(64, 128, 255, .5);
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: .5px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-header {
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background: var(--gradient-primary);
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(64, 128, 255, .3);
}

.user-name {
  margin-left: 8px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.page-main {
  background: var(--content-bg);
  padding: 20px;
}

/* Hero 欢迎区：仪表盘风格 */
.hero-card {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  overflow: hidden;
  background: var(--gradient-primary);
  padding: 22px 26px;
  border-radius: 16px;
  margin-bottom: 18px;
  box-shadow: 0 14px 30px -12px rgba(64, 128, 255, .6);
}
.hero-deco {
  position: absolute;
  right: -60px;
  top: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .12);
  pointer-events: none;
}
.hero-card::before {
  content: '';
  position: absolute;
  right: 90px;
  bottom: -70px;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .08);
  pointer-events: none;
}
.hero-left {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}
.hero-avatar {
  background: rgba(255, 255, 255, .25);
  border: 2px solid rgba(255, 255, 255, .7);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
}
.hero-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.hero-greeting {
  font-size: 19px;
  font-weight: 700;
  color: #fff;
  letter-spacing: .3px;
}
.hero-greeting b {
  font-weight: 800;
}
.hero-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, .82);
  letter-spacing: .2px;
}
.hero-stats {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 28px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 18px;
  border-left: 1px solid rgba(255, 255, 255, .35);
}
.stat-num {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  line-height: 1.1;
}
.stat-label {
  margin-top: 2px;
  font-size: 12px;
  color: rgba(255, 255, 255, .8);
}

.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.card-title::before {
  content: '';
  width: 4px;
  height: 15px;
  border-radius: 3px;
  background: var(--gradient-primary);
}

.card-header-right {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: 360px;
}

.filter-panel {
  background: var(--primary-lighter);
  border: 1px solid #e4eeff;
  border-radius: var(--radius);
  padding: 14px 16px;
  margin: 0 0 4px;
}
.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.filter-label {
  font-weight: 600;
  color: #606266;
  margin-right: 4px;
}
.active-filters {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.order-table {
  margin-top: 12px;
}
:deep(.order-table) {
  border-radius: 10px;
  overflow: hidden;
}
:deep(.order-table .el-table__row:hover > td) {
  background: var(--primary-lighter) !important;
}
:deep(.order-table th.el-table__cell) {
  background: linear-gradient(180deg, #f3f7ff 0%, #eaf1ff 100%);
  font-weight: 600;
  color: #2b3a55;
}
.btn-icon {
  margin-right: 4px;
}
.filter-select {
  width: 130px;
}
:deep(.el-input-number) {
  width: 130px;
}
</style>
