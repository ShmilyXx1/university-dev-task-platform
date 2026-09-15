<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span class="card-title">订单审核</span>
        <el-button type="primary" link @click="loadOrders">
          <el-icon class="btn-icon"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </template>

    <!-- 搜索筛选 -->
    <el-row :gutter="16" class="filter-row">
      <el-col :span="6">
        <el-input v-model="searchKeyword" placeholder="输入订单ID搜索" clearable @clear="loadOrders" @keyup.enter="loadOrders" />
      </el-col>
      <el-col :span="4">
        <el-button type="primary" :icon="Search" @click="loadOrders">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetSearch">重置</el-button>
      </el-col>
    </el-row>

    <!-- 订单表格 -->
    <el-table :data="orderList" border stripe v-loading="loading" class="full-width">
      <el-table-column label="订单ID" prop="orderId" width="80" align="center" />
      <el-table-column label="订单标题" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" prop="type" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布人" prop="senderName" width="110" align="center" />
      <el-table-column label="接单人" prop="getterName" width="110" align="center" />
      <el-table-column label="赏金" width="100" align="center">
        <template #default="{ row }">
          <span class="price-text">¥{{ row.senderPrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="押金" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.deposit" class="deposit-text">¥{{ row.deposit }}</span>
          <span v-else class="text-muted">无</span>
        </template>
      </el-table-column>
      <el-table-column label="交付文件" width="100" align="center">
        <template #default="{ row }">
          <el-button v-if="row.documentPath" link type="primary" size="small" @click="openDocument(row.documentPath)">
            查看
          </el-button>
          <span v-else class="text-muted">无</span>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="auditTagType(row.auditorComplete)" size="small">
            {{ auditText(row.auditorComplete) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button link type="success" size="small" @click="handlePass(row)">通过</el-button>
          <el-button link type="danger" size="small" @click="handleReject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空数据提示 -->
    <el-empty v-if="!loading && orderList.length === 0" description="暂无待审核订单" />

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px" destroy-on-close>
      <el-descriptions v-if="currentOrder" :column="2" border size="default">
        <el-descriptions-item label="订单ID">{{ currentOrder.orderId }}</el-descriptions-item>
        <el-descriptions-item label="订单标题">{{ currentOrder.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag size="small">{{ currentOrder.type || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="stateTagType(currentOrder.state)" size="small">{{ stateText(currentOrder.state) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布人">{{ currentOrder.senderName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接单人">{{ currentOrder.getterName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="赏金">
          <span class="price-text">¥{{ currentOrder.senderPrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="押金">
          <span v-if="currentOrder.deposit" class="deposit-text">¥{{ currentOrder.deposit }}</span>
          <span v-else class="text-muted">无</span>
        </el-descriptions-item>
        <el-descriptions-item label="接单人完成">
          <el-tag :type="String(currentOrder.userComplete) === '1' ? 'success' : 'info'" size="small">
            {{ String(currentOrder.userComplete) === '1' ? '已提交' : '未提交' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="auditTagType(currentOrder.auditorComplete)" size="small">
            {{ auditText(currentOrder.auditorComplete) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="赏金状态">
          <el-tag :type="payStateTagType(currentOrder.payState)" size="small">{{ payStateText(currentOrder.payState) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="押金状态">
          <el-tag :type="depositStateTagType(currentOrder.depositState)" size="small">{{ depositStateText(currentOrder.depositState) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间" :span="2">{{ formatDate(currentOrder.releaseDatetime) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ formatDate(currentOrder.endDatetime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDate(currentOrder.completeDatetime) }}</el-descriptions-item>
        <el-descriptions-item label="订单内容" :span="2">
          <div class="content-text">{{ currentOrder.content || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="交付文件" :span="2">
          <el-button v-if="currentOrder.documentPath" link type="primary" @click="openDocument(currentOrder.documentPath)">
            {{ currentOrder.documentPath }}
          </el-button>
          <span v-else class="text-muted">未上传</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="handlePass(currentOrder)">通过审核</el-button>
        <el-button type="danger" @click="handleReject(currentOrder)">驳回</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, RefreshLeft } from '@element-plus/icons-vue'
import { selectAllAuditorOrder, selectOneAuditorOrder, auditorUpdateOrder } from '../../api/audit'

const loading = ref(false)
const orderList = ref([])
const searchKeyword = ref('')
const detailVisible = ref(false)
const currentOrder = ref(null)

// ========== 状态格式化 ==========
const stateText = (s) => ({ '0': '待接取', '1': '已接取', '2': '已完成', '3': '已取消' }[String(s)] || '-')
const stateTagType = (s) => ({ '0': 'info', '1': 'warning', '2': 'success', '3': 'danger' }[String(s)] || 'info')

const auditText = (s) => ({ '0': '待审核', '1': '通过', '2': '驳回' }[String(s)] || '待审核')
const auditTagType = (s) => ({ '0': 'warning', '1': 'success', '2': 'danger' }[String(s)] || 'warning')

const payStateText = (s) => ({ '0': '未托管', '1': '已托管', '2': '已结算', '3': '已退款' }[String(s)] || '未托管')
const payStateTagType = (s) => ({ '0': 'info', '1': 'success', '2': 'success', '3': 'warning' }[String(s)] || 'info')

const depositStateText = (s) => ({ '0': '未支付', '1': '已支付', '2': '已退还' }[String(s)] || '未支付')
const depositStateTagType = (s) => ({ '0': 'info', '1': 'success', '2': 'warning' }[String(s)] || 'info')

const formatDate = (d) => {
  if (!d) return '-'
  const dt = new Date(d)
  if (isNaN(dt.getTime())) return String(d)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

// ========== 数据加载 ==========
const loadOrders = async () => {
  loading.value = true
  try {
    const res = await selectAllAuditorOrder()
    let list = res.data || []
    // 前端按 orderId 搜索
    if (searchKeyword.value) {
      list = list.filter(o => String(o.orderId).includes(searchKeyword.value.trim()))
    }
    orderList.value = list
  } catch (err) {
    ElMessage.error(err.message || '加载审核订单失败')
    orderList.value = []
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchKeyword.value = ''
  loadOrders()
}

// ========== 详情弹窗 ==========
const openDetail = async (row) => {
  try {
    const res = await selectOneAuditorOrder({ orderId: row.orderId })
    currentOrder.value = res.data || row
    detailVisible.value = true
  } catch (err) {
    // 降级用列表数据
    currentOrder.value = row
    detailVisible.value = true
  }
}

// ========== 交付文件 ==========
const openDocument = (path) => {
  if (!path) return
  const url = path.startsWith('http') ? path : `http://localhost:8888${path.startsWith('/') ? '' : '/'}${path}`
  window.open(url, '_blank')
}

// ========== 审核操作 ==========
const handlePass = (row) => {
  if (!row) return
  ElMessageBox.confirm(
    `确定通过订单【#${row.orderId} - ${row.title}】的审核？\n通过后将自动结算：押金退还接单人，赏金结算给接单人。`,
    '审核确认',
    { type: 'success', confirmButtonText: '确认通过', cancelButtonText: '取消' }
  ).then(async () => {
    try {
      await auditorUpdateOrder({ orderId: row.orderId, auditorComplete: '1' })
      ElMessage.success('审核通过，资金已结算')
      loadOrders()
      detailVisible.value = false
    } catch (err) {
      ElMessage.error(err.message || '审核操作失败')
    }
  }).catch(() => {})
}

const handleReject = (row) => {
  if (!row) return
  ElMessageBox.confirm(
    `确定驳回订单【#${row.orderId} - ${row.title}】？\n驳回后接单人可修改结果重新提交。`,
    '审核确认',
    { type: 'warning', confirmButtonText: '确认驳回', cancelButtonText: '取消' }
  ).then(async () => {
    try {
      await auditorUpdateOrder({ orderId: row.orderId, auditorComplete: '2' })
      ElMessage.success('已驳回，接单人可重新提交')
      loadOrders()
      detailVisible.value = false
    } catch (err) {
      ElMessage.error(err.message || '审核操作失败')
    }
  }).catch(() => {})
}

onMounted(loadOrders)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.btn-icon {
  margin-right: 4px;
}

.filter-row {
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}

.price-text {
  color: #F56C6C;
  font-weight: 600;
}

.deposit-text {
  color: #E6A23C;
  font-weight: 600;
}

.text-muted {
  color: #909399;
}

.content-text {
  white-space: pre-wrap;
  line-height: 1.8;
}
</style>
