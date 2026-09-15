<template>
  <div class="my-orders-page">
    <!-- 顶部 -->
    <div class="page-header">
      <el-card class="header-card" shadow="never">
        <div class="header-inner">
          <el-button type="primary" text @click="goBack">
            <el-icon class="back-icon"><ArrowLeft /></el-icon>返回个人中心
          </el-button>
          <h2 class="page-title">我的订单</h2>
          <el-tag size="large" type="info">{{ userStore.username || '用户' }}</el-tag>
        </div>
      </el-card>
    </div>

    <div class="page-body">
      <el-card shadow="hover" class="content-card">
        <!-- 第一层 Tab: 发布的 / 接取的 -->
        <el-tabs v-model="mainTab" type="card" @tab-change="onMainTabChange">
          <!-- ============ Tab1: 我发布的 ============ -->
          <el-tab-pane label="我发布的订单" name="sender">
            <!-- 状态子筛选: 全部 + 4 种状态 -->
            <div class="state-tags">
              <el-tag
                v-for="item in senderStates"
                :key="item.value"
                :type="senderState === item.value ? '' : 'info'"
                :effect="senderState === item.value ? 'dark' : 'plain'"
                :round="true"
                class="tag-item"
                @click="changeSenderState(item.value)"
              >
                {{ item.label }}
                <span v-if="countMap[mainTab + '_' + item.value] != null" class="count-span">
                  ({{ countMap[mainTab + '_' + item.value] }})
                </span>
              </el-tag>
            </div>

            <el-table :data="list" border stripe v-loading="loading" class="state-table">
              <el-table-column prop="orderId" label="订单ID" width="80"/>
              <el-table-column prop="title" label="订单标题"/>
              <el-table-column prop="content" label="订单内容" show-overflow-tooltip/>
              <el-table-column prop="type" label="类型" width="90"/>
              <el-table-column prop="senderPrice" label="报价(¥)" width="100"/>
              <el-table-column prop="getterName" label="接单人" width="100">
                <template #default="s">{{ s.row.getterName || '—' }}</template>
              </el-table-column>
              <el-table-column label="订单状态" width="115">
                <template #default="s">
                  <el-tag :type="stateTagType(s.row.state)">{{ stateText(s.row.state) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="截止时间" width="160">
                <template #default="s">{{ formatDate(s.row.endDatetime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="240" fixed="right">
                <template #default="s">
                  <el-button type="primary" link size="small" @click="goDetail(s.row.orderId)">查看详情</el-button>
                  <!-- 发布人: 待被接取时可修改订单 -->
                  <el-button
                    v-if="String(s.row.state) === '0'"
                    type="primary" link size="small"
                    @click="openEditSender(s.row)"
                  >修改订单</el-button>
                  <!-- 发布人: 待被接取时可取消 -->
                  <el-button
                    v-if="String(s.row.state) === '0'"
                    type="danger" link size="small"
                    @click="handleCancelBySender(s.row)"
                  >取消订单</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-if="!loading && !list.length" description="暂无订单，去发布一个吧~" />
          </el-tab-pane>

          <!-- ============ Tab2: 我接取的 ============ -->
          <el-tab-pane label="我接取的订单" name="getter">
            <!-- 状态子筛选: 全部 + 3 种(不含待被接取) -->
            <div class="state-tags">
              <el-tag
                v-for="item in getterStates"
                :key="item.value"
                :type="getterState === item.value ? '' : 'info'"
                :effect="getterState === item.value ? 'dark' : 'plain'"
                :round="true"
                class="tag-item"
                @click="changeGetterState(item.value)"
              >
                {{ item.label }}
                <span v-if="countMap[mainTab + '_' + item.value] != null" class="count-span">
                  ({{ countMap[mainTab + '_' + item.value] }})
                </span>
              </el-tag>
            </div>

            <el-table :data="list" border stripe v-loading="loading" class="state-table">
              <el-table-column prop="orderId" label="订单ID" width="80"/>
              <el-table-column prop="title" label="订单标题"/>
              <el-table-column prop="content" label="订单内容" show-overflow-tooltip/>
              <el-table-column prop="type" label="类型" width="90"/>
              <el-table-column prop="senderPrice" label="报价(¥)" width="100"/>
              <el-table-column prop="senderName" label="发布人" width="100"/>
              <el-table-column label="订单状态" width="115">
                <template #default="s">
                  <el-tag :type="stateTagType(s.row.state)">{{ stateText(s.row.state) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="截止时间" width="160">
                <template #default="s">{{ formatDate(s.row.endDatetime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="360" fixed="right">
                <template #default="s">
                  <el-button type="primary" link size="small" @click="goDetail(s.row.orderId)">查看详情</el-button>
                  <!-- 接单人: 已被接取可提交路径/凭证(不论是否标记完成) -->
                  <el-button
                    v-if="String(s.row.state) === '1'"
                    type="success" link size="small"
                    @click="openSubmitPath(s.row)"
                  >提交路径</el-button>
                  <!-- 接单人: 已被接取可取消接单 -->
                  <el-button
                    v-if="String(s.row.state) === '1'"
                    type="danger" link size="small"
                    @click="handleCancelByGetter(s.row)"
                  >取消接单</el-button>
                  <!-- 接单人: 已完成(state=2) 可撤销 -->
                  <el-button
                    v-if="String(s.row.state) === '2'"
                    type="warning" link size="small"
                    @click="handleCancelComplete(s.row)"
                  >撤销</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-if="!loading && !list.length" description="还没接过订单，去大厅看看吧~" />
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <!-- ========= 弹窗1: 发单人修改订单 (state=0) ========= -->
      <el-dialog v-model="editVisible" title="修改订单" width="520px" :close-on-click-modal="false">
        <el-form :model="editForm" label-width="90px" v-if="editVisible">
          <el-form-item label="订单标题">
            <el-input v-model="editForm.title" maxlength="50" show-word-limit placeholder="请输入标题"/>
          </el-form-item>
          <el-form-item label="订单内容">
            <el-input type="textarea" v-model="editForm.content" :rows="4" maxlength="500" show-word-limit placeholder="请输入详细需求"/>
          </el-form-item>
          <el-form-item label="订单类型">
            <el-select v-model="editForm.type" placeholder="请选择类型" class="full-width">
              <el-option label="编程" value="编程" />
              <el-option label="学习" value="学习" />
              <el-option label="设计" value="设计" />
              <el-option label="翻译" value="翻译" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="订单报价(¥)">
            <el-input-number v-model="editForm.senderPrice" :min="1" :max="99999" controls-position="right" class="full-width"/>
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker
              v-model="editForm.endDatetime"
              type="datetime"
              placeholder="选择截止日期时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="full-width"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editVisible=false">取消</el-button>
          <el-button type="primary" :loading="editLoading" @click="submitEdit">保存修改</el-button>
        </template>
      </el-dialog>

      <!-- ========= 弹窗2: 发单人提交路径/凭证 (state=1) ========= -->
      <el-dialog v-model="pathVisible" title="提交完成凭证/路径" width="480px" :close-on-click-modal="false">
        <el-form label-width="110px">
          <el-form-item label="订单标题">
            <span>{{ pathRow?.title }}</span>
          </el-form-item>
          <el-form-item label="凭证/文件路径" required>
            <el-input
              v-model="pathForm.documentPath"
              type="textarea"
              :rows="4"
              placeholder="请输入文件路径、链接或完成说明（如：GitHub链接、百度网盘地址、完成截图描述等）"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="pathVisible=false">取消</el-button>
          <el-button type="primary" :loading="pathLoading" @click="submitPath">确认提交</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import {
  getSenderAllUserOrderByState,
  getGetterAllUserOrderByState,
  cancelGetterUpdateOrder,
  revokeComplete,
  senderUpdateOrder,
  resultUpdateOrder,
  deleteOrder
} from '../../api/order'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const mainTab = ref('sender')          // sender | getter
const senderState = ref('all')          // all | 0 | 1 | 2 | 3
const getterState = ref('all')          // all | 1 | 2 | 3   (不含 0 待被接取)
const countMap = reactive({})           // 各状态数量缓存

// ========= 弹窗状态 =========
// 发单人修改订单弹窗
const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({ orderId: null, title: '', content: '', type: '', senderPrice: 1, endDatetime: '' })
// 发单人提交路径弹窗
const pathVisible = ref(false)
const pathLoading = ref(false)
const pathRow = ref(null)
const pathForm = reactive({ documentPath: '' })

// 状态配置:发布单有4种,接单单有3种(去掉待被接取)
const STATE_MAP = {
  '0': { text: '待被接取', type: 'warning' },
  '1': { text: '已被接取', type: 'primary' },
  '2': { text: '已完成',   type: 'success' },
  '3': { text: '已取消',   type: 'info' }
}
const stateText = (s) => STATE_MAP[String(s)]?.text ?? '未知状态'
const stateTagType = (s) => STATE_MAP[String(s)]?.type ?? 'danger'

const senderStates = [
  { value: 'all', label: '全部' },
  { value: '0',   label: '待被接取' },
  { value: '1',   label: '已被接取' },
  { value: '2',   label: '已完成' },
  { value: '3',   label: '已取消' }
]
const getterStates = [
  { value: 'all', label: '全部' },
  { value: '1',   label: '已被接取' },
  { value: '2',   label: '已完成' },
  { value: '3',   label: '已取消' }
]

const formatDate = (d) => {
  if (!d) return '—'
  const dt = new Date(d)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth()+1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

const goBack = () => router.push('/clientProfile')
const goDetail = (oid) => router.push({ path: `/clientOrderDetail/${oid}`, query: { from: 'myOrders' } })

// ========== 主加载逻辑 ==========
async function loadSenderList() {
  loading.value = true
  try {
    // 全部状态不传state参数（避免空字符串导致后端int/Integer转换异常）
    const params = senderState.value === 'all' ? {} : { state: senderState.value }
    const res = await getSenderAllUserOrderByState(params)
    list.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载发布订单失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

async function loadGetterList() {
  loading.value = true
  try {
    // 全部状态不传state参数
    const params = getterState.value === 'all' ? {} : { state: getterState.value }
    const res = await getGetterAllUserOrderByState(params)
    list.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载接取订单失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

// 数量统计: 每个状态单独请求一次(数据量不会大),展示在tag旁
async function loadCounts() {
  try {
    // 发单人: 0/1/2/3
    for (const s of ['0','1','2','3']) {
      const r = await getSenderAllUserOrderByState({ state: s })
      countMap['sender_' + s] = (r.data || []).length
    }
    // 接单人: 1/2/3 (不含 0)
    for (const s of ['1','2','3']) {
      const r = await getGetterAllUserOrderByState({ state: s })
      countMap['getter_' + s] = (r.data || []).length
    }
  } catch (e) {
    // 数量不影响主流程,失败静默
  }
}

const onMainTabChange = () => {
  list.value = []
  if (mainTab.value === 'sender') loadSenderList()
  else loadGetterList()
}

const changeSenderState = (v) => {
  senderState.value = v
  loadSenderList()
}
const changeGetterState = (v) => {
  getterState.value = v
  loadGetterList()
}

// ========== 操作: 取消/完成/撤销 ==========
// 发单人取消订单 (调用删除接口 or 取消接口,这里优先deleteOrder)
const handleCancelBySender = async (row) => {
  const paid = String(row.payState) === '1'
  try {
    await ElMessageBox.confirm(
      `确定取消订单「${row.title}」吗？取消后订单将标记为"已取消"，可在「已取消」列表中查看。` +
      (paid ? '该订单赏金已托管，取消后赏金将原路退回您的支付账户。' : ''),
      '取消订单确认',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' }
    )
  } catch { return }
  loading.value = true
  try {
    await deleteOrder({ orderId: row.orderId })
    ElMessage.success(paid ? '已取消订单，托管赏金将原路退回' : '已取消订单')
    Promise.all([loadSenderList(), loadCounts()])
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  } finally {
    loading.value = false
  }
}

// 接单人取消接单 (调用 cancelGetterUpdateOrder)
const handleCancelByGetter = async (row) => {
  const depositPaid = String(row.depositState) === '1'
  try {
    await ElMessageBox.confirm(
      `确定要取消接取订单「${row.title}」吗？` +
      (depositPaid ? '取消后押金将原路退回您的支付账户。' : ''),
      '取消接单确认',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' }
    )
  } catch { return }
  loading.value = true
  try {
    await cancelGetterUpdateOrder({ orderId: row.orderId })
    ElMessage.success(depositPaid ? '已取消接单，押金将原路退回' : '已取消接单')
    Promise.all([loadGetterList(), loadCounts()])
  } catch (e) {
    ElMessage.error(e.message || '取消接单失败')
  } finally {
    loading.value = false
  }
}

// 接单人撤销"已完成"（state=2 回退为 state=1，需重新提交路径）
const handleCancelComplete = async (row) => {
  try {
    await ElMessageBox.confirm(
      '撤销后订单将回到"已被接取"状态，需要重新提交结果路径并再次审核，确认撤销？',
      '撤销确认',
      { type: 'warning', confirmButtonText: '确认撤销', cancelButtonText: '取消' }
    )
  } catch { return }
  loading.value = true
  try {
    await revokeComplete({ orderId: row.orderId })
    ElMessage.success('已撤销，订单回到已被接取状态')
    Promise.all([loadGetterList(), loadCounts()])
  } catch (e) {
    ElMessage.error(e.message || '撤销失败')
  } finally {
    loading.value = false
  }
}

// ========== 操作: 发单人修改订单 ==========
const openEditSender = (row) => {
  editForm.orderId = row.orderId
  editForm.title = row.title || ''
  editForm.content = row.content || ''
  editForm.type = row.type || '编程'
  editForm.senderPrice = Number(row.senderPrice) || 1
  // endDatetime 后端返回的可能是字符串，直接塞给 date-picker
  editForm.endDatetime = row.endDatetime || ''
  editVisible.value = true
}
const submitEdit = async () => {
  if (!editForm.title.trim()) return ElMessage.warning('请输入订单标题')
  if (!editForm.content.trim()) return ElMessage.warning('请输入订单内容')
  if (!editForm.type) return ElMessage.warning('请选择订单类型')
  if (!editForm.endDatetime) return ElMessage.warning('请选择截止时间')
  if (editForm.senderPrice < 1) return ElMessage.warning('报价不能少于1元')

  editLoading.value = true
  try {
    // ✅ 调用 senderUpdateOrder (发单人修改自己的订单)
    await senderUpdateOrder({
      orderId: editForm.orderId,
      title: editForm.title.trim(),
      content: editForm.content.trim(),
      type: editForm.type,
      senderPrice: editForm.senderPrice,
      endDatetime: editForm.endDatetime
    })
    ElMessage.success('订单修改成功')
    editVisible.value = false
    Promise.all([loadSenderList(), loadCounts()])
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    editLoading.value = false
  }
}

// ========== 操作: 发单人提交路径/凭证 ==========
const openSubmitPath = (row) => {
  pathRow.value = row
  pathForm.documentPath = ''
  pathVisible.value = true
}
const submitPath = async () => {
  if (!pathForm.documentPath.trim()) return ElMessage.warning('请输入凭证/文件路径')
  pathLoading.value = true
  try {
    // ✅ 调用 resultUpdateOrder (PUT /order/resultUpdateOrder?orderId=&documentPath=)
    await resultUpdateOrder({
      orderId: pathRow.value.orderId,
      documentPath: pathForm.documentPath.trim()
    })
    ElMessage.success('凭证提交成功，已进入审核阶段')
    pathVisible.value = false
    Promise.all([loadGetterList(), loadCounts()])
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    pathLoading.value = false
  }
}

onMounted(async () => {
  if (mainTab.value === 'sender') loadSenderList()
  else loadGetterList()
  loadCounts()
})
</script>

<style scoped>
.my-orders-page {
  min-height: 100vh;
  background: #f5f7fa;
}
.page-header { padding: 16px 24px 0; }
.header-card { border-radius: 12px; }
.header-inner {
  display: flex; align-items: center; justify-content: space-between;
}
.page-title {
  margin: 0; font-size: 20px; font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.page-body { padding: 20px 24px 40px; }

.state-tags {
  display: flex; flex-wrap: wrap; gap: 10px; padding: 4px 2px;
}
.tag-item {
  cursor: pointer;
  padding: 6px 14px !important;
  font-size: 13px;
  transition: all .2s;
}
.tag-item:hover { opacity: .82; transform: translateY(-1px); }
:deep(.el-tabs__header) { margin-bottom: 18px; }
:deep(.el-tabs__item) { font-size: 15px; }

.back-icon { margin-right: 4px; }
.content-card { border-radius: 12px; }
.count-span { margin-left: 4px; opacity: .85; }
.state-table { margin-top: 14px; }
.full-width { width: 100%; }
</style>
