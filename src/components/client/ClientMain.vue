<template>
  <el-container class="page-container">
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="page-header">
        <span class="brand">
          <span class="brand-logo"><el-icon><UserFilled /></el-icon></span>
          <span class="page-title">软件开发众包在线接单平台</span>
        </span>

        <div class="header-right">
          <el-button class="admin-entry-btn" link @click="goAdminLogin">
            <el-icon class="btn-icon"><Setting /></el-icon>管理端
          </el-button>
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
        <!-- Hero 概览区 -->
        <section class="hero-card">
          <div class="hero-deco hero-deco-1" aria-hidden="true"></div>
          <div class="hero-deco hero-deco-2" aria-hidden="true"></div>
          <div class="hero-left">
            <el-avatar :size="64" :src="userStore.avatarUrl" class="hero-avatar">
              {{ userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U' }}
            </el-avatar>
            <div class="hero-info">
              <div class="hero-badge">
                <el-icon><TrendCharts /></el-icon>
                软件开发众包 · 需求大厅
              </div>
              <div class="hero-greeting">欢迎回来，<b>{{ userStore.username || '用户' }}</b></div>
              <div class="hero-sub">挑选合适的需求，一口价接单，交付成果后由平台审核结算</div>
            </div>
          </div>

          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-icon"><el-icon><Tickets /></el-icon></span>
              <div class="stat-body">
                <span class="stat-num">{{ stats.total }}</span>
                <span class="stat-label">待接取需求</span>
              </div>
            </div>
            <div class="stat-item">
              <span class="stat-icon"><el-icon><Money /></el-icon></span>
              <div class="stat-body">
                <span class="stat-num">¥{{ stats.avg }}</span>
                <span class="stat-label">平均赏金</span>
              </div>
            </div>
            <div class="stat-item">
              <span class="stat-icon"><el-icon><GoldMedal /></el-icon></span>
              <div class="stat-body">
                <span class="stat-num">¥{{ stats.max }}</span>
                <span class="stat-label">最高赏金</span>
              </div>
            </div>
            <div class="stat-item">
              <span class="stat-icon"><el-icon><CollectionTag /></el-icon></span>
              <div class="stat-body">
                <span class="stat-num">{{ stats.types }}</span>
                <span class="stat-label">需求类型</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 快捷入口 -->
        <section class="quick-grid">
          <button class="quick-card" @click="goAddOrder">
            <span class="quick-icon quick-icon-blue"><el-icon><Plus /></el-icon></span>
            <span class="quick-text">
              <b>发布需求</b>
              <small>填写要求并托管赏金</small>
            </span>
          </button>
          <button class="quick-card" @click="router.push('/clientMyOrders')">
            <span class="quick-icon quick-icon-green"><el-icon><Tickets /></el-icon></span>
            <span class="quick-text">
              <b>我的订单</b>
              <small>查看发布与接取记录</small>
            </span>
          </button>
          <button class="quick-card" @click="router.push('/clientChat')">
            <span class="quick-icon quick-icon-purple"><el-icon><ChatDotRound /></el-icon></span>
            <span class="quick-text">
              <b>联系客服</b>
              <small>在线咨询排队接入</small>
            </span>
          </button>
          <button class="quick-card" @click="router.push('/clientFeedback')">
            <span class="quick-icon quick-icon-orange"><el-icon><EditPen /></el-icon></span>
            <span class="quick-text">
              <b>问题反馈</b>
              <small>提交建议与问题</small>
            </span>
          </button>
        </section>

        <!-- 大厅订单列表，仅显示未接取的订单 -->
        <el-card shadow="never" class="hall-card">
          <template #header>
            <div class="card-header-bar">
              <div class="card-header-left">
                <span class="card-title">需求大厅</span>
                <span class="count-pill">{{ orderList.length }} 个待接取</span>
              </div>
              <div class="card-header-right">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索订单ID / 标题 / 内容 / 类型 / 发布人"
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
                <el-button :icon="RefreshLeft" @click="resetAll">重置</el-button>
              </div>
            </div>
          </template>

          <!-- 筛选面板 -->
          <div class="filter-panel">
            <div class="type-row">
              <span class="filter-label">需求类型</span>
              <button
                class="chip"
                :class="{ active: !filter.type }"
                @click="selectType('')"
              >全部</button>
              <button
                v-for="t in typeOptions"
                :key="t"
                class="chip"
                :class="{ active: filter.type === t }"
                @click="selectType(t)"
              >{{ t }}</button>
            </div>

            <div class="price-row">
              <span class="filter-label">赏金范围</span>
              <el-input-number
                v-model="filter.minPrice"
                :min="0"
                :precision="0"
                placeholder="最低"
                controls-position="right"
              />
              <span class="price-sep">至</span>
              <el-input-number
                v-model="filter.maxPrice"
                :min="0"
                :precision="0"
                placeholder="最高"
                controls-position="right"
              />
              <el-button
                :type="sortState === 'none' ? 'default' : 'success'"
                :icon="sortIcon"
                @click="cycleSort"
              >
                按赏金{{ sortLabel }}
              </el-button>
              <el-button type="primary" plain @click="doFilter">
                <el-icon class="btn-icon"><Filter /></el-icon>应用筛选
              </el-button>
              <el-button v-if="activeFilters" link type="primary" @click="clearFilter">清除筛选</el-button>
            </div>

            <!-- 当前筛选信息 -->
            <div v-if="activeFilters" class="active-filters">
              <el-icon><Filter /></el-icon>
              <span>{{ activeFilters }}</span>
            </div>
          </div>

          <el-table :data="orderList" v-loading="loading" class="order-table">
            <el-table-column label="需求信息" min-width="280">
              <template #default="scope">
                <div class="order-cell">
                  <div class="order-title">
                    <span class="order-id">#{{ scope.row.orderId }}</span>
                    {{ scope.row.title }}
                  </div>
                  <div class="order-desc">{{ scope.row.content || '暂无需求描述' }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="100">
              <template #default="scope">
                <span class="type-tag">{{ scope.row.type || '其他' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="赏金" width="110">
              <template #default="scope">
                <span class="price-text">¥{{ formatAmount(scope.row.senderPrice) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="押金" width="100">
              <template #default="scope">
                <span v-if="Number(scope.row.deposit) > 0" class="deposit-text">¥{{ formatAmount(scope.row.deposit) }}</span>
                <span v-else class="text-muted">无押金</span>
              </template>
            </el-table-column>
            <el-table-column label="发布人" width="120">
              <template #default="scope">
                <el-button v-if="scope.row.senderName" link type="primary" @click.stop="goUserProfile(scope.row.senderName)">
                  {{ scope.row.senderName }}
                </el-button>
                <span v-else class="text-muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="截止时间" width="170">
              <template #default="scope">
                <span class="time-text">{{ formatDate(scope.row.endDatetime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="scope">
                <el-tag :type="stateTagType(scope.row.state)" effect="light" round>
                  {{ stateText(scope.row.state) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="scope">
                <el-button class="detail-btn" size="small" round @click="goDetail(scope.row.orderId)">
                  查看详情<el-icon class="btn-arrow"><ArrowRight /></el-icon>
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无待接取的需求，去发布一个吧" :image-size="90" />
            </template>
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
import { ArrowDown, UserFilled, Plus, Search, RefreshLeft, Filter, Sort, Bottom, Top, ArrowRight, TrendCharts, Tickets, Money, GoldMedal, CollectionTag, ChatDotRound, EditPen, Setting } from '@element-plus/icons-vue'
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

// 需求类型快捷筛选
const typeOptions = ['编程', '学习', '设计', '翻译', '其他']

// 顶部概览统计
const stats = computed(() => {
  const list = orderList.value || []
  const prices = list.map((o) => Number(o.senderPrice) || 0)
  const total = prices.reduce((a, b) => a + b, 0)
  return {
    total: list.length,
    avg: prices.length ? Math.round(total / prices.length) : 0,
    max: prices.length ? Math.max(...prices) : 0,
    types: new Set(list.map((o) => o.type).filter(Boolean)).size
  }
})

const formatAmount = (val) => {
  const num = Number(val)
  if (Number.isNaN(num)) return '0'
  return Number.isInteger(num) ? num : num.toFixed(2)
}

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

// 类型标签点击筛选
const selectType = (t) => {
  filter.type = t
  doFilter()
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

// 跳转管理端登录（管理员 / 审核员 / 客服）
const goAdminLogin = () => {
  router.push('/login')
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

.admin-entry-btn {
  color: var(--text-regular);
  font-size: 13px;
}
.admin-entry-btn:hover {
  color: var(--primary);
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
  color: var(--text-regular);
  font-weight: 500;
}

.page-main {
  background: var(--content-bg);
  padding: 24px;
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;
}

/* ============ Hero 概览区 ============ */
.hero-card {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 28px;
  overflow: hidden;
  background: linear-gradient(120deg, #3F54E6 0%, #4F6BFE 45%, #38BDF8 100%);
  padding: 30px 34px;
  border-radius: 20px;
  margin-bottom: 18px;
  box-shadow: 0 20px 40px -18px rgba(63, 84, 230, .65);
}
.hero-deco {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.hero-deco-1 {
  right: -70px;
  top: -100px;
  width: 280px;
  height: 280px;
  background: rgba(255, 255, 255, .13);
}
.hero-deco-2 {
  right: 200px;
  bottom: -110px;
  width: 190px;
  height: 190px;
  background: rgba(255, 255, 255, .08);
}
.hero-left {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}
.hero-avatar {
  flex: none;
  background: rgba(255, 255, 255, .22);
  border: 2px solid rgba(255, 255, 255, .65);
  color: #fff;
  font-size: 23px;
  font-weight: 700;
  box-shadow: 0 8px 20px -8px rgba(0, 0, 0, .35);
}
.hero-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .18);
  border: 1px solid rgba(255, 255, 255, .25);
  color: #fff;
  font-size: 12px;
  letter-spacing: .3px;
}
.hero-greeting {
  font-size: 22px;
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
  display: grid;
  grid-template-columns: repeat(4, minmax(96px, 1fr));
  gap: 10px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, .14);
  border: 1px solid rgba(255, 255, 255, .22);
  backdrop-filter: blur(6px);
}
.stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  background: rgba(255, 255, 255, .2);
  color: #fff;
  font-size: 17px;
}
.stat-body {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  min-width: 0;
}
.stat-num {
  font-size: 20px;
  font-weight: 800;
  color: #fff;
  white-space: nowrap;
}
.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, .78);
  white-space: nowrap;
}

/* ============ 快捷入口 ============ */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}
.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease;
}
.quick-card:hover {
  transform: translateY(-3px);
  border-color: #D7DEFF;
  box-shadow: var(--shadow-hover);
}
.quick-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex: none;
  border-radius: 12px;
  font-size: 19px;
}
.quick-icon-blue { background: #EEF1FF; color: #4F6BFE; }
.quick-icon-green { background: #EAF8F0; color: #21A56B; }
.quick-icon-purple { background: #F3EEFF; color: #7C5CFC; }
.quick-icon-orange { background: #FFF3E8; color: #E8871E; }
.quick-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.quick-text b {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
}
.quick-text small {
  font-size: 12px;
  color: var(--text-muted);
}

/* ============ 大厅卡片 ============ */
.hall-card {
  border-radius: 16px;
}
.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.card-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}
.card-title::before {
  content: '';
  width: 4px;
  height: 16px;
  border-radius: 3px;
  background: var(--gradient-primary);
}
.count-pill {
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
}
.card-header-right {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.search-input {
  width: 340px;
}

/* ============ 筛选面板 ============ */
.filter-panel {
  background: #FAFBFE;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.type-row,
.price-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.filter-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: .3px;
  flex: none;
}
.chip {
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
  color: var(--text-regular);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all .18s ease;
}
.chip:hover {
  border-color: #C4CFFF;
  color: var(--primary);
}
.chip.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
  box-shadow: 0 4px 10px -4px rgba(79, 107, 254, .55);
}
.price-sep {
  color: var(--text-muted);
  font-size: 13px;
}
.active-filters {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 2px;
  color: var(--text-muted);
  font-size: 12px;
}

/* ============ 表格 ============ */
.order-table {
  margin-top: 2px;
}
:deep(.order-table) {
  border-radius: 12px;
  overflow: hidden;
}
:deep(.order-table th.el-table__cell) {
  background: #F7F8FC;
  font-weight: 600;
  color: var(--text-regular);
  height: 46px;
}
:deep(.order-table .el-table__row) {
  transition: background .18s ease;
}
:deep(.order-table .el-table__row:hover > td) {
  background: #F6F8FF !important;
}
:deep(.order-table .el-table__cell) {
  padding: 12px 0;
}
.order-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.order-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.order-id {
  flex: none;
  padding: 1px 7px;
  border-radius: 6px;
  background: #F0F2F8;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 500;
}
.order-desc {
  font-size: 12px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.type-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 6px;
  background: #EEF1FF;
  color: #4F6BFE;
  font-size: 12px;
  font-weight: 500;
}
.price-text {
  color: #F0564A;
  font-size: 15px;
  font-weight: 700;
}
.deposit-text {
  color: #E6A23C;
  font-weight: 600;
}
.text-muted {
  color: var(--text-muted);
}
.time-text {
  color: var(--text-regular);
  font-size: 13px;
}
.detail-btn {
  border-color: #D7DEFF;
  color: var(--primary);
  background: #F7F9FF;
}
.detail-btn:hover {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
.btn-arrow {
  margin-left: 2px;
  font-size: 12px;
}
.btn-icon {
  margin-right: 4px;
}
:deep(.el-input-number) {
  width: 108px;
}

/* 窄屏适配 */
@media (max-width: 1180px) {
  .hero-card {
    flex-direction: column;
    align-items: stretch;
  }
  .hero-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .search-input {
    width: 240px;
  }
}
</style>
