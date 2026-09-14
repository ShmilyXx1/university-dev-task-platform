<template>
  <el-container style="height: 100vh; border: 1px solid #eee">
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header style="background: #fff; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; border-bottom: 1px solid #e4e7ed">
        <span style="font-size: 18px; font-weight: bold">客户端</span>

        <div style="display: flex; align-items: center; gap: 16px">
          <NotificationBell />
          <!-- 右上角头像用户名下拉菜单 -->
          <el-dropdown @command="handleDropdownCommand">
            <span class="user-header">
              <el-avatar :size="36" :src="userStore.avatarUrl" style="background: #409EFF">{{ userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U' }}</el-avatar>
              <span style="margin-left:8px">{{ userStore.username || '用户' }}</span>
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
      <el-main style="background: #f5f7fa; padding: 20px">
        <!-- 欢迎横幅(精简版) -->
        <div class="welcome-bar">
          <el-icon color="#409EFF" size="20"><UserFilled /></el-icon>
          <span class="welcome-text">欢迎，<b>{{ userStore.username || '用户' }}</b>，这里是客户端大厅</span>
        </div>

        <!-- 大厅订单列表，仅显示未接取的订单 -->
        <el-card shadow="hover">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px">
              <div style="display:flex;align-items:center;gap:12px">
                <span>大厅 - 待接取订单</span>
                <el-button type="primary" :icon="Plus" @click="goAddOrder">发布订单</el-button>
              </div>
              <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
                <!-- 搜索框 -->
                <el-input
                  v-model="searchKeyword"
                  placeholder="按订单ID/标题/内容搜索/订单类型/用户名"
                  clearable
                  style="width: 360px"
                  @keyup.enter="doSearch"
                  @clear="resetAll"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" @click="doSearch">
                  <el-icon style="margin-right:4px"><Search /></el-icon>搜索
                </el-button>
                <el-button @click="resetAll">
                  <el-icon style="margin-right:4px"><RefreshLeft /></el-icon>重置
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
                style="width: 130px"
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
                style="width: 130px"
                size="default"
              />

              <!-- 最高价 -->
              <el-input-number
                v-model="filter.maxPrice"
                :min="0"
                :precision="2"
                placeholder="最高价"
                controls-position="right"
                style="width: 130px"
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
                <el-icon style="margin-right:4px"><Filter /></el-icon>应用筛选
              </el-button>
            </div>

            <!-- 当前筛选信息 -->
            <div v-if="activeFilters" class="active-filters">
              <el-tag size="small" effect="plain">{{ activeFilters }}</el-tag>
              <el-button link type="primary" size="small" @click="clearFilter">清除筛选</el-button>
            </div>
          </div>

          <el-table :data="orderList" border stripe v-loading="loading" style="margin-top:10px">
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
.user-header {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.welcome-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(90deg, #ecf5ff, #f5f7fa);
  border-left: 4px solid #409EFF;
  padding: 10px 16px;
  border-radius: 6px;
  margin-bottom: 14px;
  font-size: 14px;
  color: #606266;
}
.welcome-text b {
  color: #303133;
  font-weight: 600;
}

.filter-panel {
  background: #fafbfc;
  border: 1px dashed #e4e7ed;
  border-radius: 8px;
  padding: 12px 16px;
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
:deep(.el-input-number) {
  width: 130px;
}
</style>
