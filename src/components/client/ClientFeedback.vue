<template>
  <el-container class="page-container">
    <el-header class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft" circle />
      <span class="page-header-title">问题反馈</span>
    </el-header>

    <el-main class="page-main">
      <div class="feedback-wrap">
        <!-- 提交反馈 -->
        <el-card shadow="hover" class="feedback-card">
          <template #header>
            <span class="card-title">提交新反馈</span>
          </template>
          <el-input
            v-model="newContent"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请描述您遇到的问题，客服会尽快回复处理"
          />
          <div class="submit-row">
            <el-button type="primary" :loading="submitting" @click="submitFeedback">提交反馈</el-button>
          </div>
        </el-card>

        <!-- 反馈列表 -->
        <el-card shadow="hover" v-loading="loading">
          <template #header>
            <div class="card-header-inner">
              <span class="card-title">我的反馈记录</span>
              <el-radio-group v-model="filterSolve" size="small" @change="loadList">
                <el-radio-button label="">全部</el-radio-button>
                <el-radio-button label="0">处理中</el-radio-button>
                <el-radio-button label="1">已解决</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <el-empty v-if="!loading && list.length === 0" description="暂无反馈记录" />

          <div v-for="item in list" :key="item.feedbackId" class="feedback-item">
            <!-- 用户反馈内容 -->
            <div class="fb-row">
              <div class="fb-content">
                <el-icon color="#4080FF"><ChatDotRound /></el-icon>
                <div class="fb-content-body">
                  <div class="fb-text">{{ item.content }}</div>
                  <div class="fb-time">提交于 {{ formatDate(item.sendDatetime) }}</div>
                </div>
                <el-tag :type="statusTag(item).type" size="small">{{ statusTag(item).text }}</el-tag>
              </div>
            </div>

            <!-- 客服回复 -->
            <div v-if="item.reply" class="reply-box">
              <el-icon color="#67C23A"><Service /></el-icon>
              <div class="fb-content-body">
                <div class="reply-title">客服回复</div>
                <div class="fb-text">{{ item.reply }}</div>
                <div class="fb-time">回复于 {{ formatDate(item.replyDatetime) }}</div>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="fb-actions">
              <template v-if="item.reply && String(item.solve) === '0'">
                <el-button type="success" size="small" @click="markSolve(item, '1')">
                  <el-icon class="btn-icon"><Check /></el-icon>已解决
                </el-button>
                <el-button type="warning" size="small" @click="markSolve(item, '0')">
                  <el-icon class="btn-icon"><RefreshLeft /></el-icon>未解决，继续反馈
                </el-button>
              </template>
              <el-button type="danger" size="small" link @click="removeFeedback(item)">
                <el-icon class="btn-icon"><Delete /></el-icon>删除
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, RefreshLeft, Delete, ChatDotRound, Service } from '@element-plus/icons-vue'
import { addFeedback, getMyFeedback, updateFeedbackSolve, deleteFeedback } from '../../api/feedback'

const router = useRouter()

const newContent = ref('')
const submitting = ref(false)
const loading = ref(false)
const list = ref([])
const filterSolve = ref('')

const goBack = () => router.push('/clientMain')

const formatDate = (d) => {
  if (!d) return '-'
  const dt = new Date(d)
  if (isNaN(dt.getTime())) return String(d)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

// 状态：无回复=待回复；有回复+solve=0=待确认；solve=1=已解决
const statusTag = (item) => {
  if (String(item.solve) === '1') return { type: 'success', text: '已解决' }
  if (item.reply) return { type: 'primary', text: '客服已回复，待确认' }
  return { type: 'warning', text: '待客服回复' }
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterSolve.value !== '') params.solve = filterSolve.value
    const res = await getMyFeedback(params)
    list.value = res.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载反馈失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

const submitFeedback = async () => {
  if (!newContent.value.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }
  submitting.value = true
  try {
    await addFeedback({ content: newContent.value.trim() })
    ElMessage.success('反馈已提交，请等待客服回复')
    newContent.value = ''
    filterSolve.value = ''
    loadList()
  } catch (err) {
    ElMessage.error(err.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const markSolve = (item, solve) => {
  const tip = solve === '1'
    ? '确认该反馈已解决？'
    : '标记为未解决后客服将继续跟进，确定吗？'
  ElMessageBox.confirm(tip, '提示', { type: solve === '1' ? 'success' : 'warning' })
    .then(async () => {
      try {
        await updateFeedbackSolve({ feedbackId: item.feedbackId, solve })
        ElMessage.success(solve === '1' ? '已确认解决' : '已重新打开，客服将继续跟进')
        loadList()
      } catch (err) {
        ElMessage.error(err.message || '操作失败')
      }
    }).catch(() => {})
}

const removeFeedback = (item) => {
  ElMessageBox.confirm('确定删除该反馈记录？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteFeedback({ feedbackId: item.feedbackId })
        ElMessage.success('已删除')
        loadList()
      } catch (err) {
        ElMessage.error(err.message || '删除失败')
      }
    }).catch(() => {})
}

onMounted(loadList)
</script>

<style scoped>
.feedback-item {
  border-bottom: 1px solid #ebeef5;
  padding: 16px 0;
}
.feedback-item:last-child {
  border-bottom: none;
}
.fb-content {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.fb-time {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
.reply-box {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  background: #f0f9eb;
  border-radius: 8px;
  padding: 12px 14px;
  margin: 10px 0 0 30px;
}
.fb-actions {
  margin: 10px 0 0 30px;
  display: flex;
  gap: 8px;
}

.page-container {
  height: 100vh;
  background: var(--content-bg);
}
.page-header {
  background: rgba(255, 255, 255, .85);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-light);
  box-shadow: 0 2px 10px rgba(16, 24, 40, .04);
  position: sticky;
  top: 0;
  z-index: 10;
}
.page-header-title {
  margin-left: 12px;
  font-size: 18px;
  font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.page-main {
  background: var(--content-bg);
  padding: 20px;
}
.feedback-wrap {
  width: 820px;
  margin: 0 auto;
}
.feedback-card {
  margin-bottom: 20px;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}
.card-title {
  font-weight: 600;
}
.submit-row {
  margin-top: 12px;
  text-align: right;
}
.card-header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.fb-content-body {
  flex: 1;
}
.fb-text {
  white-space: pre-wrap;
  line-height: 1.7;
}
.reply-title {
  font-weight: 600;
  color: #67C23A;
  margin-bottom: 4px;
}
.btn-icon {
  margin-right: 4px;
}
</style>
