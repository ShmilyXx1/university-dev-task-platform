<template>
  <el-container class="page-container">
    <el-header class="page-header">
      <el-button @click="handleBack" :icon="ArrowLeft" circle />
      <span class="page-title">订单详情</span>
    </el-header>

    <el-main class="page-main">
      <div class="detail-wrap" v-loading="loading">
        <el-empty v-if="!loading && !detail" description="订单不存在或已被删除" />

        <template v-if="detail">
          <!-- 顶部横幅 -->
          <section class="detail-hero">
            <div class="hero-main">
              <div class="hero-eyebrow">
                <span class="order-no">订单 #{{ detail.orderId }}</span>
                <span class="dot"></span>
                <span>{{ detail.type || '其他' }}</span>
              </div>
              <h1 class="hero-title">{{ detail.title }}</h1>
              <div class="hero-meta">
                <span class="meta-pill"><el-icon><Calendar /></el-icon>发布于 {{ formatDate(detail.releaseDatetime) }}</span>
                <span class="meta-pill"><el-icon><Clock /></el-icon>截止 {{ formatDate(detail.endDatetime) }}</span>
                <el-tag :type="stateTagType(detail.state)" effect="dark" round>{{ stateText(detail.state) }}</el-tag>
              </div>
            </div>
            <div class="hero-price">
              <div class="price-label">订单赏金</div>
              <div class="price-value"><span class="cny">¥</span>{{ detail.senderPrice }}</div>
              <div class="price-sub">发单人定价 · 一口价</div>
            </div>
          </section>

          <!-- 流程步骤 -->
          <el-card shadow="never" class="steps-card">
            <el-alert
              v-if="String(detail.state) === '3'"
              title="该订单已取消，交易流程已终止"
              type="info"
              :closable="false"
              show-icon
            />
            <el-steps v-else :active="stepActive" finish-status="success" align-center>
              <el-step title="发布需求" :description="formatDate(detail.releaseDatetime)" />
              <el-step title="赏金托管" :description="String(detail.payState) === '1' ? '已托管' : '待托管'" />
              <el-step title="接单" :description="detail.getterName || '待接取'" />
              <el-step title="提交成果" :description="detail.userComplete ? '已提交' : '待提交'" />
              <el-step title="审核结算" :description="detail.auditorComplete ? '审核通过' : (detail.userComplete ? '审核中' : '待审核')" />
            </el-steps>
          </el-card>

          <div class="detail-grid">
            <!-- 左侧内容 -->
            <div class="detail-left">
              <el-card shadow="never" class="info-card">
                <template #header>
                  <div class="section-title"><span class="bar"></span>需求信息</div>
                </template>

                <div class="meta-grid">
                  <div class="meta-item">
                    <span class="meta-label">订单编号</span>
                    <span class="meta-value">#{{ detail.orderId }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">需求类型</span>
                    <span class="meta-value"><span class="type-tag">{{ detail.type || '其他' }}</span></span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">发布时间</span>
                    <span class="meta-value">{{ formatDate(detail.releaseDatetime) }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">截止时间</span>
                    <span class="meta-value">{{ formatDate(detail.endDatetime) }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">完成时间</span>
                    <span class="meta-value">{{ formatDate(detail.completeDatetime) }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">最后更新</span>
                    <span class="meta-value">{{ formatDate(detail.updateDatetime) }}</span>
                  </div>
                </div>

                <div class="content-block">
                  <div class="block-label">需求描述</div>
                  <div class="order-content">{{ detail.content || '暂无详细描述' }}</div>
                </div>
              </el-card>

              <el-card shadow="never" class="info-card">
                <template #header>
                  <div class="section-title"><span class="bar"></span>交易与交付</div>
                </template>

                <div class="trade-grid">
                  <div class="trade-item">
                    <div class="trade-name">赏金托管</div>
                    <el-tag :type="payTag.type" effect="light" round>{{ payTag.text }}</el-tag>
                  </div>
                  <div class="trade-item">
                    <div class="trade-name">履约押金</div>
                    <el-tag :type="depositTag.type" effect="light" round>{{ depositTag.text }}</el-tag>
                  </div>
                  <div class="trade-item">
                    <div class="trade-name">接单人完成</div>
                    <el-tag :type="detail.userComplete ? 'success' : 'info'" effect="light" round>
                      {{ detail.userComplete ? '已提交成果' : '未提交' }}
                    </el-tag>
                  </div>
                  <div class="trade-item">
                    <div class="trade-name">审核确认</div>
                    <el-tag :type="detail.auditorComplete ? 'success' : 'info'" effect="light" round>
                      {{ detail.auditorComplete ? '审核通过' : '未审核' }}
                    </el-tag>
                  </div>
                </div>
              </el-card>
            </div>

            <!-- 右侧操作栏 -->
            <aside class="detail-right">
              <el-card shadow="never" class="side-card">
                <div class="side-title">订单金额</div>
                <div class="amount-row">
                  <span>订单赏金</span>
                  <b class="amount-bounty">¥{{ detail.senderPrice }}</b>
                </div>
                <div class="amount-row">
                  <span>履约押金</span>
                  <b class="amount-deposit">{{ Number(detail.deposit) > 0 ? '¥' + detail.deposit : '无' }}</b>
                </div>
                <p class="amount-tip">赏金由发单人托管至平台，审核通过后结算给接单人；押金在订单完成后原路退还。</p>

                <div class="side-actions">
                  <!-- 发单人：待被接取且赏金未托管时显示托管 -->
                  <el-button
                    v-if="String(detail.state) === '0' && !isNotSender && String(detail.payState) !== '1'"
                    type="warning"
                    :icon="Wallet"
                    @click="handleEscrow"
                    :loading="payLoading"
                  >
                    托管赏金 ¥{{ detail.senderPrice }}
                  </el-button>

                  <!-- 接单人：待接取订单可接单 -->
                  <el-button
                    v-if="String(detail.state) === '0' && isNotSender"
                    type="success"
                    :icon="Check"
                    @click="handleAccept"
                    :loading="acceptLoading"
                  >
                    {{ needDeposit ? `接单（需付押金 ¥${detail.deposit}）` : '接单' }}
                  </el-button>

                  <!-- 已被接取：接单人提交交付路径 -->
                  <el-button
                    v-if="String(detail.state) === '1' && isGetter && String(detail.userComplete) !== '1'"
                    type="primary"
                    :icon="Upload"
                    @click="handleSubmitPath"
                    :loading="completeLoading"
                  >
                    提交交付路径
                  </el-button>

                  <!-- 已提交、等待审核 -->
                  <el-alert
                    v-if="String(detail.state) === '1' && isGetter && String(detail.userComplete) === '1'"
                    title="已提交交付结果，等待审核"
                    type="warning"
                    :closable="false"
                    show-icon
                  />

                  <!-- 已完成：接单人可撤销 -->
                  <el-button
                    v-if="String(detail.state) === '2' && isGetter"
                    type="warning"
                    plain
                    @click="handleRevoke"
                    :loading="cancelLoading"
                  >
                    撤销完成状态
                  </el-button>

                  <el-button class="back-btn" :icon="ArrowLeft" @click="handleBack">
                    {{ route.query.from === 'myOrders' ? '返回我的订单' : '返回大厅' }}
                  </el-button>
                </div>
              </el-card>

              <el-card shadow="never" class="side-card">
                <div class="side-title">交易双方</div>

                <div class="person-row">
                  <el-avatar :size="42" class="sender-avatar">
                    {{ detail.senderName ? detail.senderName.charAt(0).toUpperCase() : 'U' }}
                  </el-avatar>
                  <div class="person-info">
                    <div class="person-name">{{ detail.senderName || '未知用户' }}</div>
                    <div class="person-role">发单人</div>
                  </div>
                  <el-button
                    v-if="detail.senderName"
                    size="small"
                    :icon="Message"
                    @click="openContactSender"
                  >联系</el-button>
                </div>

                <div class="person-divider"></div>

                <div class="person-row">
                  <el-avatar :size="42" class="getter-avatar">
                    {{ detail.getterName ? detail.getterName.charAt(0).toUpperCase() : '?' }}
                  </el-avatar>
                  <div class="person-info">
                    <div class="person-name">{{ detail.getterName || '暂未接取' }}</div>
                    <div class="person-role">接单人</div>
                  </div>
                  <el-button
                    v-if="detail.getterName"
                    size="small"
                    :icon="User"
                    @click="goUserProfile(detail.getterName)"
                  >主页</el-button>
                </div>
              </el-card>
            </aside>
          </div>
        </template>
      </div>

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
import { ArrowLeft, Check, Message, CopyDocument, Promotion, Wallet, Calendar, Clock, User, Upload } from '@element-plus/icons-vue'
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

// 流程步骤进度
const stepActive = computed(() => {
  const d = detail.value
  if (!d) return 0
  const s = String(d.state)
  if (s === '3') return 1
  if (s === '0') return String(d.payState) === '1' ? 2 : 1
  if (s === '1') return String(d.userComplete) === '1' ? 4 : 3
  if (s === '2') return 5
  return 0
})

// 赏金 / 押金状态标签
const payTag = computed(() => {
  const s = String(detail.value?.payState)
  if (s === '1') return { text: '已托管', type: 'success' }
  if (s === '2') return { text: '已结算', type: 'success' }
  if (s === '3') return { text: '已退款', type: 'info' }
  return { text: '未托管', type: 'warning' }
})
const depositTag = computed(() => {
  const d = detail.value
  if (!d || !(Number(d.deposit) > 0)) return { text: '无需押金', type: 'info' }
  const s = String(d.depositState)
  if (s === '1') return { text: '已支付', type: 'success' }
  if (s === '2') return { text: '已退还', type: 'info' }
  return { text: '待支付', type: 'warning' }
})

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

const sendContactMsg = () => {
  const peerId = detail.value?.senderId
  if (!peerId) {
    ElMessage.error('未获取到发单人信息，无法发起私信')
    return
  }
  const content = contactMsg.value.trim()
  contactVisible.value = false
  // 跳转到私信页，把留言内容带过去预填
  router.push({
    path: '/clientP2PChat',
    query: {
      peerId,
      peerName: detail.value.senderName,
      ...(content ? { content } : {})
    }
  })
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
  padding: 24px;
  background: var(--content-bg);
}

.detail-wrap {
  max-width: 1180px;
  margin: 0 auto;
  min-height: 320px;
}

/* ============ 顶部横幅 ============ */
.detail-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  overflow: hidden;
  padding: 28px 32px;
  border-radius: 20px;
  background: linear-gradient(120deg, #3F54E6 0%, #4F6BFE 50%, #38BDF8 100%);
  box-shadow: 0 20px 40px -20px rgba(63, 84, 230, .65);
  color: #fff;
}
.detail-hero::after {
  content: '';
  position: absolute;
  right: -80px;
  top: -120px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .12);
  pointer-events: none;
}
.hero-main {
  position: relative;
  z-index: 1;
  min-width: 0;
}
.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, .82);
  letter-spacing: .4px;
}
.order-no {
  padding: 2px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .18);
  border: 1px solid rgba(255, 255, 255, .22);
  color: #fff;
}
.dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .55);
}
.hero-title {
  margin: 10px 0 14px;
  font-size: 25px;
  font-weight: 800;
  letter-spacing: .4px;
  color: #fff;
  line-height: 1.35;
  word-break: break-word;
}
.hero-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, .85);
}
.hero-price {
  position: relative;
  z-index: 1;
  flex: none;
  text-align: right;
  padding: 14px 22px;
  border-radius: 16px;
  background: rgba(255, 255, 255, .16);
  border: 1px solid rgba(255, 255, 255, .25);
  backdrop-filter: blur(6px);
}
.price-label {
  font-size: 12px;
  color: rgba(255, 255, 255, .78);
  letter-spacing: .5px;
}
.price-value {
  font-size: 34px;
  font-weight: 800;
  color: #fff;
  line-height: 1.2;
}
.price-value .cny {
  font-size: 18px;
  font-weight: 600;
  margin-right: 2px;
}
.price-sub {
  font-size: 11.5px;
  color: rgba(255, 255, 255, .72);
}

/* ============ 流程卡 ============ */
.steps-card {
  margin-top: 16px;
  border-radius: 16px;
}
:deep(.steps-card .el-card__body) {
  padding: 22px 26px 12px;
}
:deep(.el-step__title) {
  font-size: 13.5px;
  font-weight: 600;
}
:deep(.el-step__description) {
  font-size: 11.5px;
  padding: 0 6px;
}

/* ============ 双栏布局 ============ */
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  margin-top: 16px;
  align-items: start;
}
.detail-left {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.info-card {
  border-radius: 16px;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}
.section-title .bar {
  width: 4px;
  height: 15px;
  border-radius: 3px;
  background: var(--gradient-primary);
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 24px;
  padding: 4px 2px 18px;
  border-bottom: 1px dashed var(--border-light);
}
.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.meta-label {
  font-size: 12px;
  color: var(--text-muted);
}
.meta-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  word-break: break-all;
}
.type-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  background: #EEF1FF;
  color: #4F6BFE;
  font-size: 12px;
  font-weight: 500;
}
.content-block {
  padding-top: 16px;
}
.block-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
}
.order-content {
  white-space: pre-wrap;
  line-height: 1.85;
  min-height: 84px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #FAFBFE;
  border: 1px solid var(--border-light);
  color: var(--text-primary);
  font-size: 13.5px;
}
.trade-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.trade-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border-radius: 12px;
  background: #FAFBFE;
  border: 1px solid var(--border-light);
}
.trade-name {
  font-size: 12px;
  color: var(--text-muted);
}
.trade-item .el-tag {
  align-self: flex-start;
}

/* ============ 右侧栏 ============ */
.detail-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 84px;
}
.side-card {
  border-radius: 16px;
}
.side-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 14px;
}
.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 0;
  font-size: 13px;
  color: var(--text-regular);
}
.amount-bounty {
  color: #F0564A;
  font-size: 20px;
  font-weight: 800;
}
.amount-deposit {
  color: #E6A23C;
  font-size: 15px;
  font-weight: 700;
}
.amount-tip {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #F6F8FF;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
}
.side-actions {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.side-actions .el-button {
  width: 100%;
  margin-left: 0;
}
.back-btn {
  color: var(--text-regular);
}
.person-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.person-info {
  flex: 1;
  min-width: 0;
}
.person-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.person-role {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}
.person-divider {
  height: 1px;
  background: var(--border-light);
  margin: 14px 0;
}
.sender-avatar {
  background: var(--gradient-primary);
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(79, 107, 254, .28);
}
.getter-avatar {
  background: linear-gradient(135deg, #23B26D 0%, #6FDC9E 100%);
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(35, 178, 109, .25);
}

/* ============ 弹窗 ============ */
.contact-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 4px 16px;
  border-bottom: 1px dashed var(--border-light);
}
.sender-avatar-lg {
  background: var(--gradient-primary);
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(64, 128, 255, .3);
}
.contact-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.contact-role {
  color: var(--text-muted);
  font-size: 13px;
  margin-top: 2px;
}
.contact-body {
  padding: 16px 4px 4px;
}
.contact-label {
  color: var(--text-regular);
  font-size: 13px;
  margin-bottom: 8px;
}

/* ============ 窄屏适配 ============ */
@media (max-width: 980px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .detail-right {
    position: static;
  }
  .detail-hero {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-price {
    text-align: left;
  }
  .trade-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
