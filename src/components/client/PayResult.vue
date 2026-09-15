<template>
  <div class="pay-result-page">
    <el-card class="result-card" shadow="hover">
      <div class="result-icon">
        <el-icon v-if="status === 'success'" color="#67C23A" :size="64"><CircleCheckFilled /></el-icon>
        <el-icon v-else-if="status === 'fail'" color="#F56C6C" :size="64"><CircleCloseFilled /></el-icon>
        <el-icon v-else color="#4080FF" :size="64" class="rotating"><Loading /></el-icon>
      </div>

      <h2 class="result-title">
        {{ status === 'success' ? '支付成功' : status === 'fail' ? '未查到支付结果' : '正在确认支付结果...' }}
      </h2>
      <p class="result-desc">
        <span v-if="type === 'BOUNTY'">赏金托管</span>
        <span v-else-if="type === 'DEPOSIT'">接单押金</span>
        <span v-if="status === 'success' && type === 'DEPOSIT'">已支付，正在为您完成接单...</span>
        <span v-else-if="status === 'success'">已完成</span>
        <span v-else-if="status === 'fail'">若您已付款，系统可能存在延迟，可稍后在订单详情中查看</span>
        <span v-else>支付完成后将自动跳转，请勿关闭页面</span>
      </p>

      <div class="result-actions">
        <el-button type="primary" @click="goDetail">返回订单详情</el-button>
        <el-button v-if="status === 'fail'" @click="startQuery">重新查询</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, CircleCloseFilled, Loading } from '@element-plus/icons-vue'
import { getPayStatus } from '../../api/pay'
import { getterUpdateOrder } from '../../api/order'

const route = useRoute()
const router = useRouter()

const orderId = route.query.orderId
const type = route.query.type || 'BOUNTY'
const status = ref('loading')   // loading | success | fail
let timer = null
let times = 0
const MAX_TIMES = 15             // 最多查询 15 次（约 30 秒）

const goDetail = () => {
  clearTimer()
  router.replace(`/clientOrderDetail/${orderId}?from=hall`)
}

const clearTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const doQuery = async () => {
  times++
  try {
    const res = await getPayStatus({ orderId, type })
    if (res.data === '1' || res.data === 1) {
      clearTimer()
      status.value = 'success'
      if (type === 'DEPOSIT') {
        // 押金支付成功 → 自动完成接单
        try {
          await getterUpdateOrder({ orderId })
          ElMessage.success('押金支付成功，接单成功！')
        } catch {
          ElMessage.warning('押金已支付，但接单失败，请手动点击接单')
        }
      } else {
        ElMessage.success('赏金托管成功！')
      }
      setTimeout(goDetail, 1500)
    } else if (times >= MAX_TIMES) {
      clearTimer()
      status.value = 'fail'
    }
  } catch {
    if (times >= MAX_TIMES) {
      clearTimer()
      status.value = 'fail'
    }
  }
}

const startQuery = () => {
  status.value = 'loading'
  times = 0
  doQuery()
  timer = setInterval(doQuery, 2000)
}

onMounted(() => {
  if (!orderId) {
    status.value = 'fail'
    return
  }
  startQuery()
})

onUnmounted(clearTimer)
</script>

<style scoped>
.pay-result-page {
  min-height: 100vh;
  background: var(--content-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}
.result-card {
  max-width: 500px;
  margin: 40px auto;
  width: 100%;
  border-radius: var(--radius);
  padding: 40px 30px;
  text-align: center;
  box-shadow: var(--shadow);
}
.result-icon {
  width: 120px;
  height: 120px;
  font-size: 64px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 auto 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f2f6ff, #f8fafc);
  box-shadow: inset 0 0 0 1px #e6edfa, 0 14px 32px -14px rgba(64, 128, 255, .4);
}
.rotating {
  animation: rotate 1.2s linear infinite;
}
@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.result-title {
  margin: 0 0 12px;
  font-size: 22px;
  color: #303133;
}
.result-desc {
  color: #909399;
  font-size: 14px;
  margin: 0 0 28px;
}
.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
