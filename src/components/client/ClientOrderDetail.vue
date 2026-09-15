<template>
  <el-container class="page-container">
    <el-header class="page-header">
      <el-button @click="handleBack" :icon="ArrowLeft" circle />
      <span class="page-title">订单详情</span>
    </el-header>

    <el-main class="page-main">
      <el-card class="detail-card" shadow="hover" v-loading="loading">
        <template #header v-if="detail">
          <div class="card-header-bar">
            <span class="card-title">
              #{{ detail.orderId }} - {{ detail.title }}
            </span>
            <el-tag :type="stateTagType(detail.state)" size="large" effect="dark">
              {{ stateText(detail.state) }}
            </el-tag>
          </div>
        </template>

        <el-empty v-if="!loading && !detail" description="订单不存在或已被删除" />

        <el-descriptions v-if="detail" :column="2" border size="default">
          <el-descriptions-item label="订单ID">{{ detail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">
            <el-tag>{{ detail.type }}</el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="发布人">
            <div class="user-cell">
              <el-avatar :size="28" class="sender-avatar">{{ detail.senderName ? detail.senderName.charAt(0).toUpperCase() : '-' }}</el-avatar>
              <el-button v-if="detail.senderName" link type="primary" @click="goUserProfile(detail.senderName)">{{ detail.senderName }}</el-button>
              <span v-else>-</span>
              <el-button
                v-if="detail.senderName"
                type="primary"
                link
                size="small"
                :icon="Message"
                @click="openContactSender"
              >
                联系发单人
              </el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="接单人">
            <div class="user-cell">
              <template v-if="detail.getterName">
                <el-avatar :size="28" class="getter-avatar">{{ detail.getterName.charAt(0).toUpperCase() }}</el-avatar>
                <el-button link type="primary" @click="goUserProfile(detail.getterName)">{{ detail.getterName }}</el-button>
              </template>
              <span v-else class="text-muted">暂未接取</span>
            </div>
          </el-descriptions-item>

          <el-descriptions-item label="发布方报价">
            <span class="price-text">¥ {{ detail.senderPrice }}</span>
          </el-descriptions-item>

          <el-descriptions-item label="押金">
            <span v-if="detail.deposit != null" class="deposit-text">¥ {{ detail.deposit }}</span>
            <span v-else class="text-muted">无押金</span>
          </el-descriptions-item>

          <el-descriptions-item label="订单状态" :span="2">
            <el-tag :type="stateTagType(detail.state)">{{ stateText(detail.state) }}</el-tag>
            <el-tag
              :type="String(detail.payState) === '1' ? 'success' : 'info'"
              class="status-tag"
            >
              {{ String(detail.payState) === '1' ? '赏金已托管' : '赏金未托管' }}
            </el-tag>
            <el-tag
              v-if="detail.deposit != null && Number(detail.deposit) > 0"
              :type="String(detail.depositState) === '1' ? 'success' : 'warning'"
              class="status-tag"
            >
              {{ String(detail.depositState) === '1' ? '押金已支付' : '押金待支付' }}
            </el-tag>
            <span class="status-info">
              接单人完成：{{ detail.userComplete ? '是' : '否' }}
              &nbsp;|&nbsp;
              审核确认：{{ detail.auditorComplete ? '是' : '否' }}
            </span>
          </el-descriptions-item>

          <el-descriptions-item label="发布时间">{{ formatDate(detail.releaseDatetime) }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ formatDate(detail.endDatetime) }}</el-descriptions-item>

          <el-descriptions-item label="完成时间">
            {{ formatDate(detail.completeDatetime) }}
          </el-descriptions-item>
          <el-descriptions-item label="最后更新">
            {{ formatDate(detail.updateDatetime) }}
          </el-descriptions-item>

          <el-descriptions-item label="订单内容" :span="2">
            <div class="order-content">
              {{ detail.content || '-' }}
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 底部操作按钮区 -->
        <div v-if="detail" class="action-bar">
          <el-button @click="handleBack" :icon="ArrowLeft">{{ route.query.from === 'myOrders' ? '返回我的订单' : '返回大厅' }}</el-button>

          <!-- 待接取状态：非发单人可接单 -->
          <!-- 发单人：待被接取且赏金未托管时，显示"托管赏金" -->
          <el-button
            v-if="String(detail.state) === '0' && !isNotSender && String(detail.payState) !== '1'"
            type="warning"
            :icon="Wallet"
            @click="handleEscrow"
            :loading="payLoading"
          >
            托管赏金 ¥{{ detail.senderPrice }}
          </el-button>

          <el-button
            v-if="String(detail.state) === '0' && isNotSender"
            type="success"
            :icon="Check"
            @click="handleAccept"
            :loading="acceptLoading"
          >
            {{ needDeposit ? `接单（需付押金 ¥${detail.deposit}）` : '接单' }}
          </el-button>

          <!-- 已被接取状态：接单人提交交付路径（提交后进入审核） -->
          <el-button
            v-if="String(detail.state) === '1' && isGetter && String(detail.userComplete) !== '1'"
            type="primary"
            @click="handleSubmitPath"
            :loading="completeLoading"
          >
            提交交付路径
          </el-button>

          <!-- 已提交结果、等待审核：提示状态 -->
          <el-tag
            v-if="String(detail.state) === '1' && isGetter && String(detail.userComplete) === '1'"
            type="warning"
            effect="plain"
          >
            已提交交付结果，等待审核
          </el-tag>

          <!-- 已完成状态：接单人可撤销，回退为已被接取重新提交 -->
          <el-button
            v-if="String(detail.state) === '2' && isGetter"
            type="warning"
            @click="handleRevoke"
            :loading="cancelLoading"
          >
            撤销完成状态
          </el-button>
        </div>
      </el-card>

      <!-- 联系发单人弹窗 -->
      <el-dialog v-model="contactVisible" title="联系发单人" width="440px" :close-on-click-modal="false">
        <div v-if="detail" class="contact-header">
          <el-avatar :size="48" class="sender-avatar-lg">{{ detail.senderName ? detail.senderName.charAt(0).toUpperCase() : 'U' }}</el-avatar>
          <div>
            <div class="contact-name">{{ detail.senderName }}</div>
            <div class="contact-role">订单发布人</div>
          </div>
        </div>
        <div class="contact-body">
          <div class="contact-label">留言内容（可选）：</div>
          <el-input
            v-model="contactMsg"
            type="textarea"
            :rows="4"
            placeholder="请输入您想对发布人说的话，例如：我对这个订单很感兴趣，可以详细聊聊吗？"
            maxlength="200"
            show-word-limit
          />
        </div>
        <template #footer>
          <el-button @click="contactVisible = false">取消</el-button>
          <el-button type="primary" @click="copyContactInfo" :icon="CopyDocument">复制用户名并关闭</el-button>
          <el-button type="success" @click="sendContactMsg" :icon="Promotion">发送消息</el-button>
        </template>
      </el-dialog>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Message, CopyDocument, Promotion, Wallet } from '@element-plus/icons-vue'
import { selectOneOrder, getterUpdateOrder, resultUpdateOrder, revokeComplete } from '../../api/order'
import { createPay } from '../../api/pay'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const detail = ref(null)
const acceptLoading = ref(false)
const completeLoading = ref(false)
const cancelLoading = ref(false)
const payLoading = ref(false)

// 联系发单人弹窗
const contactVisible = ref(false)
const contactMsg = ref('')

const orderId = computed(() => route.params.orderId)

// 订单状态映射
const STATE_MAP = {
  '0': { text: '待被接取', type: 'warning' },
  '1': { text: '已被接取', type: 'primary' },
  '2': { text: '完成订单', type: 'success' },
  '3': { text: '接单人取消', type: 'info' }
}
const stateText = (s) => STATE_MAP[String(s)]?.text ?? '未知状态'
const stateTagType = (s) => STATE_MAP[String(s)]?.type ?? 'danger'

// 日期格式化
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')} ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`
}

// 角色判断
const isNotSender = computed(() => {
  return detail.value && detail.value.senderName !== userStore.username
})
const isGetter = computed(() => {
  return detail.value && detail.value.getterName === userStore.username
})

// 是否需要先付押金才能接单（有押金金额且尚未支付）
const needDeposit = computed(() => {
  return detail.value
    && detail.value.deposit != null
    && Number(detail.value.deposit) > 0
    && String(detail.value.depositState) !== '1'
})

// 跳转到支付宝收银台（后端返回自动提交的表单HTML）
const goAlipay = (formHtml) => {
  const div = document.createElement('div')
  div.innerHTML = formHtml
  document.body.appendChild(div)
  const form = div.querySelector('form') || document.forms[document.forms.length - 1]
  if (form) form.submit()
  else { document.open(); document.write(formHtml); document.close() }
}

// 发单人托管赏金
const handleEscrow = async () => {
  try {
    await ElMessageBox.confirm(
      `托管后赏金由平台保管，订单完成后结算给接单人。确认托管 ¥${detail.value.senderPrice} ？`,
      '托管赏金',
      { confirmButtonText: '去支付', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  payLoading.value = true
  try {
    const res = await createPay({ orderId: orderId.value, type: 'BOUNTY' })
    goAlipay(res.data)
  } catch (err) {
    ElMessage.error(err.message || '发起支付失败')
  } finally {
    payLoading.value = false
  }
}

// 加载详情
const loadDetail = async () => {
  loading.value = true
  try {
    const res = await selectOneOrder({ orderId: orderId.value })
    detail.value = res.data || null
    if (!detail.value) {
      ElMessage.warning('未找到该订单')
    }
  } catch (err) {
    ElMessage.error(err.message || '加载订单详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

// 返回列表 - 根据 from 参数判断从哪儿来回哪儿去
const handleBack = () => {
  const from = route.query.from
  if (from === 'myOrders') {
    router.push('/clientMyOrders')    // 从我的订单进来，回我的订单
  } else {
    router.push('/clientMain')        // 默认/从大厅进来，回大厅
  }
}

const goUserProfile = (username) => {
  if (!username) return
  router.push(`/clientUserProfile/${encodeURIComponent(username)}`)
}

// ========== 联系发单人 ==========
const openContactSender = () => {
  contactMsg.value = ''
  contactVisible.value = true
}

const copyContactInfo = async () => {
  if (!detail.value?.senderName) return
  try {
    await navigator.clipboard.writeText(detail.value.senderName)
    ElMessage.success(`已复制发单人用户名：${detail.value.senderName}`)
  } catch {
    // 兜底：用 textarea + execCommand
    const ta = document.createElement('textarea')
    ta.value = detail.value.senderName
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success(`已复制发单人用户名：${detail.value.senderName}`)
  }
  contactVisible.value = false
}

const sendContactMsg = async () => {
  // TODO: 若后端有站内信/消息接口，这里对接即可；目前先兜底展示"已发送（本地）"
  if (!detail.value?.senderName) return
  ElMessage.success({
    message: `消息已发送给【${detail.value.senderName}】（如对接站内信接口可替换为真实发送）`,
    duration: 2500
  })
  contactVisible.value = false
}

// 接单（有押金时先走支付宝支付押金，支付成功后结果页会自动接单）
const handleAccept = async () => {
  try {
    const tip = needDeposit.value
      ? `该订单需先支付押金 ¥${detail.value.deposit}，押金在订单完成后退还。确认去支付并接单吗？`
      : '确定要接取该订单吗？'
    await ElMessageBox.confirm(tip, '确认接单', {
      confirmButtonText: needDeposit.value ? '去支付' : '确定接单',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch { return }

  // 需要押金：跳转支付宝支付押金，付完自动跳回结果页完成接单
  if (needDeposit.value) {
    payLoading.value = true
    try {
      const res = await createPay({ orderId: orderId.value, type: 'DEPOSIT' })
      goAlipay(res.data)
    } catch (err) {
      ElMessage.error(err.message || '发起支付失败')
      payLoading.value = false
    }
    return
  }

  acceptLoading.value = true
  try {
    await getterUpdateOrder({ orderId: orderId.value })
    ElMessage.success('接单成功！')
    loadDetail()
  } catch (err) {
    ElMessage.error(err.message || '接单失败')
  } finally {
    acceptLoading.value = false
  }
}

// 提交交付路径（接单人完成任务，提交后进入审核）
const handleSubmitPath = async () => {
  let path = ''
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入交付结果的文件路径或网盘链接：',
      '提交交付路径',
      {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：https://pan.baidu.com/xxx 或 服务器文件路径',
        inputValidator: (v) => (v && v.trim() ? true : '交付路径不能为空')
      }
    )
    path = value.trim()
  } catch { return }

  completeLoading.value = true
  try {
    await resultUpdateOrder({ orderId: orderId.value, documentPath: path })
    ElMessage.success('已提交交付结果，等待审核')
    loadDetail()
  } catch (err) {
    ElMessage.error(err.message || '提交失败')
  } finally {
    completeLoading.value = false
  }
}

// 撤销"已完成"（state=2 回退为 state=1，需重新提交结果）
const handleRevoke = async () => {
  try {
    await ElMessageBox.confirm(
      '撤销后订单将回到"已被接取"状态，需要重新提交交付路径并再次审核，确认撤销？',
      '撤销确认',
      { confirmButtonText: '确认撤销', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  cancelLoading.value = true
  try {
    await revokeComplete({ orderId: orderId.value })
    ElMessage.success('已撤销，订单回到已被接取状态')
    loadDetail()
  } catch (err) {
    ElMessage.error(err.message || '撤销失败')
  } finally {
    cancelLoading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.page-container {
  height: 100vh;
  background: var(--content-bg);
}

.page-header {
  background: rgba(255, 255, 255, .85);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-light);
  box-shadow: 0 2px 10px rgba(16, 24, 40, .04);
  position: sticky;
  top: 0;
  z-index: 10;
  gap: 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-main {
  padding: 20px;
  display: flex;
  justify-content: center;
}

.detail-card {
  width: 820px;
}

.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sender-avatar {
  background: var(--gradient-primary);
  font-size: 12px;
}

.getter-avatar {
  background: #67C23A;
  font-size: 12px;
}

.text-muted {
  color: #909399;
}

.price-text {
  color: #F56C6C;
  font-weight: 700;
  font-size: 16px;
}

.deposit-text {
  color: #E6A23C;
  font-weight: 700;
  font-size: 16px;
}

.status-tag {
  margin-left: 10px;
}

.status-info {
  margin-left: 16px;
  color: #909399;
}

.order-content {
  white-space: pre-wrap;
  line-height: 1.8;
  min-height: 80px;
  color: #303133;
}

.action-bar {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.contact-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 4px 16px;
  border-bottom: 1px dashed #ebeef5;
}

.sender-avatar-lg {
  background: var(--gradient-primary);
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(64, 128, 255, .3);
}

.contact-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.contact-role {
  color: #909399;
  font-size: 13px;
  margin-top: 2px;
}

.contact-body {
  padding: 16px 4px 4px;
}

.contact-label {
  color: #606266;
  font-size: 13px;
  margin-bottom: 8px;
}

:deep(.el-descriptions__label) {
  width: 110px;
  font-weight: 600;
}
</style>
