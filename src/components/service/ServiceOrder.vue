<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span>全部订单查询（客服只读）</span>
        <el-button type="primary" link @click="loadList">
          <el-icon class="btn-icon"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </template>

    <!-- 筛选栏 -->
    <div class="toolbar">
      <el-radio-group v-model="stateFilter" size="small" @change="loadList">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="0">待被接取</el-radio-button>
        <el-radio-button label="1">已被接取</el-radio-button>
        <el-radio-button label="2">已完成</el-radio-button>
        <el-radio-button label="3">已取消</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="keyword"
        placeholder="搜索标题/内容/用户名/类型"
        clearable
        class="keyword-input"
        @keyup.enter="loadList"
        @clear="loadList"
      />
      <el-button type="primary" size="small" @click="loadList">
        <el-icon class="btn-icon"><Search /></el-icon>搜索
      </el-button>
    </div>

    <!-- 订单表格 -->
    <el-table :data="orderList" border stripe v-loading="loading" class="full-width">
      <el-table-column label="订单ID" prop="orderId" width="80" align="center" />
      <el-table-column label="标题" prop="title" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" prop="type" width="90" align="center" />
      <el-table-column label="发布人" prop="senderName" width="110" align="center" />
      <el-table-column label="接单人" prop="getterName" width="110" align="center">
        <template #default="{ row }">{{ row.getterName || '—' }}</template>
      </el-table-column>
      <el-table-column label="赏金(元)" prop="senderPrice" width="90" align="center">
        <template #default="{ row }">¥{{ row.senderPrice }}</template>
      </el-table-column>
      <el-table-column label="押金(元)" prop="deposit" width="90" align="center">
        <template #default="{ row }">{{ row.deposit ? '¥' + row.deposit : '无' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="stateTag(row.state).type" size="small">{{ stateTag(row.state).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="160" align="center">
        <template #default="{ row }">{{ formatDate(row.releaseDatetime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && orderList.length === 0" description="暂无订单" />

    <!-- 详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="订单详情" width="680px" destroy-on-close>
      <el-descriptions v-if="current" :column="2" border>
        <el-descriptions-item label="订单ID">{{ current.orderId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="stateTag(current.state).type" size="small">{{ stateTag(current.state).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ current.type }}</el-descriptions-item>
        <el-descriptions-item label="赏金">¥{{ current.senderPrice }}（一口价）</el-descriptions-item>
        <el-descriptions-item label="押金">{{ current.deposit ? '¥' + current.deposit : '无押金' }}</el-descriptions-item>
        <el-descriptions-item label="赏金托管">
          <el-tag :type="payStateTag(current.payState).type" size="small">{{ payStateTag(current.payState).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布人">{{ current.senderName }}</el-descriptions-item>
        <el-descriptions-item label="接单人">{{ current.getterName || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ formatDate(current.releaseDatetime) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ formatDate(current.endDatetime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDate(current.completeDatetime) }}</el-descriptions-item>
        <el-descriptions-item label="交付结果">
          <el-link v-if="current.documentPath" :href="current.documentPath" target="_blank" type="primary">查看交付文件</el-link>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="订单内容" :span="2">
          <div class="pre-wrap-text">{{ current.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { selectAllOrderForService } from '../../api/order'

const loading = ref(false)
const orderList = ref([])
const stateFilter = ref('all')
const keyword = ref('')
const dialogVisible = ref(false)
const current = ref(null)

const formatDate = (d) => {
  if (!d) return '—'
  const dt = new Date(d)
  if (isNaN(dt.getTime())) return String(d)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

const stateTag = (state) => {
  const map = {
    0: { type: 'warning', text: '待被接取' },
    1: { type: 'primary', text: '已被接取' },
    2: { type: 'success', text: '已完成' },
    3: { type: 'info', text: '已取消' }
  }
  return map[Number(state)] || { type: 'info', text: '未知' }
}

const payStateTag = (payState) => {
  const map = {
    0: { type: 'info', text: '未托管' },
    1: { type: 'primary', text: '已托管' },
    2: { type: 'success', text: '已结算' },
    3: { type: 'warning', text: '已退款' }
  }
  return map[Number(payState)] || { type: 'info', text: '未托管' }
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {}
    if (stateFilter.value !== 'all') params.state = stateFilter.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res = await selectAllOrderForService(params)
    orderList.value = res.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载订单失败')
    orderList.value = []
  } finally {
    loading.value = false
  }
}

const openDetail = (row) => {
  current.value = row
  dialogVisible.value = true
}

onMounted(loadList)
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
  flex-wrap: wrap;
  align-items: center;
}
.keyword-input {
  width: 280px;
}
.full-width {
  width: 100%;
}
.pre-wrap-text {
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>
