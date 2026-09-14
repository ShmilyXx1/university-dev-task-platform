<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span>用户问题反馈处理</span>
        <el-button type="primary" link @click="loadList">
          <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </template>

    <!-- 状态筛选 -->
    <el-radio-group v-model="filter" style="margin-bottom: 16px" @change="loadList">
      <el-radio-button label="all">全部</el-radio-button>
      <el-radio-button label="pending">待回复</el-radio-button>
      <el-radio-button label="replied">已回复待确认</el-radio-button>
      <el-radio-button label="solved">已解决</el-radio-button>
    </el-radio-group>

    <!-- 反馈表格 -->
    <el-table :data="filteredList" border stripe v-loading="loading" style="width: 100%">
      <el-table-column label="ID" prop="feedbackId" width="70" align="center" />
      <el-table-column label="用户" prop="username" width="120" align="center" />
      <el-table-column label="反馈内容" prop="content" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row).type" size="small">{{ statusTag(row).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160" align="center">
        <template #default="{ row }">{{ formatDate(row.sendDatetime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openReply(row)">
            {{ row.reply ? '查看/追加回复' : '回复' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && filteredList.length === 0" description="暂无反馈记录" />

    <!-- 回复弹窗 -->
    <el-dialog v-model="dialogVisible" :title="current ? `回复反馈 #${current.feedbackId}` : '回复反馈'" width="600px" destroy-on-close>
      <div v-if="current" style="margin-bottom: 16px">
        <div style="font-weight:600;margin-bottom:6px">
          <el-tag size="small" type="info">{{ current.username }}</el-tag>
          <el-tag :type="statusTag(current).type" size="small" style="margin-left:8px">{{ statusTag(current).text }}</el-tag>
        </div>
        <div class="user-content">
          <el-icon color="#409EFF"><ChatDotRound /></el-icon>
          <span style="white-space:pre-wrap;line-height:1.7">{{ current.content }}</span>
        </div>
        <div class="fb-time">提交于 {{ formatDate(current.sendDatetime) }}</div>

        <div v-if="current.reply" class="old-reply">
          <div style="font-weight:600;color:#67C23A;margin-bottom:4px">历史回复</div>
          <div style="white-space:pre-wrap;line-height:1.7">{{ current.reply }}</div>
          <div class="fb-time">回复于 {{ formatDate(current.replyDatetime) }}</div>
        </div>
      </div>

      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请输入回复内容，用户将在客户端看到"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReply">发送回复</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, ChatDotRound } from '@element-plus/icons-vue'
import { getAllFeedback, serviceReply } from '../../api/feedback'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const filter = ref('pending')
const dialogVisible = ref(false)
const current = ref(null)
const replyContent = ref('')

const formatDate = (d) => {
  if (!d) return '-'
  const dt = new Date(d)
  if (isNaN(dt.getTime())) return String(d)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

const statusTag = (item) => {
  if (!item) return { type: 'info', text: '-' }
  if (String(item.solve) === '1') return { type: 'success', text: '已解决' }
  if (item.reply) return { type: 'primary', text: '已回复待确认' }
  return { type: 'warning', text: '待回复' }
}

const loadList = async () => {
  loading.value = true
  try {
    // 已解决走 type=1；其余（待回复/已回复待确认/全部）拉取后前端过滤
    const params = {}
    if (filter.value === 'solved') params.type = '1'
    else if (filter.value === 'pending' || filter.value === 'replied') params.type = '0'
    const res = await getAllFeedback(params)
    let data = res.data || []
    if (filter.value === 'pending') {
      data = data.filter(f => !f.reply)
    } else if (filter.value === 'replied') {
      data = data.filter(f => f.reply)
    }
    list.value = data
  } catch (err) {
    ElMessage.error(err.message || '加载反馈失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

const filteredList = computed(() => (list.value || []).filter(item => item && item.feedbackId != null))

const openReply = (row) => {
  current.value = row
  replyContent.value = row.reply || ''
  dialogVisible.value = true
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  submitting.value = true
  try {
    await serviceReply({ feedbackId: current.value.feedbackId, reply: replyContent.value.trim() })
    ElMessage.success('回复成功，用户可在客户端查看')
    dialogVisible.value = false
    loadList()
  } catch (err) {
    ElMessage.error(err.message || '回复失败')
  } finally {
    submitting.value = false
  }
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
.user-content {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  background: #f4f8ff;
  border-radius: 8px;
  padding: 12px;
}
.old-reply {
  background: #f0f9eb;
  border-radius: 8px;
  padding: 12px;
  margin-top: 12px;
}
.fb-time {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
</style>
