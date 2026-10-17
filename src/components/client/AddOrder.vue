<template>
  <el-container class="page-container">
    <el-header class="page-header">
      <el-button @click="handleBack" :icon="ArrowLeft" circle />
      <span class="page-header-title">发布订单</span>
    </el-header>

    <el-main class="page-main">
      <div class="form-wrap">
        <div class="page-banner">
          <div>
            <div class="banner-badge">发布需求 · NEW ORDER</div>
            <div class="banner-title">发布软件开发需求</div>
            <div class="banner-sub">填写需求信息并托管赏金后，订单才会展示在需求大厅</div>
          </div>
          <div class="banner-actions">
            <span class="banner-chip">一口价模式</span>
          </div>
        </div>

        <el-card class="form-card" shadow="never">
          <template #header>
            <span class="card-title">填写订单信息</span>
          </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="large">
          <el-form-item label="订单标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入订单标题" maxlength="50" show-word-limit />
          </el-form-item>

          <el-form-item label="订单内容" prop="content">
            <el-input v-model="form.content" type="textarea" placeholder="请详细描述订单需求内容" :rows="4" maxlength="500" show-word-limit />
          </el-form-item>

          <el-form-item label="订单类型" prop="type">
            <div class="type-select-wrap">
              <el-select v-model="form.type" placeholder="请选择订单类型" class="full-width">
                <el-option v-for="opt in typeOptions" :key="opt" :label="opt" :value="opt" />
              </el-select>
              <el-button class="add-type-btn" @click="showTypeDialog = true" :icon="Plus" circle />
            </div>
          </el-form-item>

          <el-form-item label="发布方报价" prop="senderPrice">
            <el-input-number v-model="form.senderPrice" :min="0" :max="99999" :precision="2" :step="10" controls-position="right" class="price-input" />
            <span class="unit-text">元</span>
          </el-form-item>

          <el-form-item label="押金">
            <el-checkbox v-model="form.hasDeposit">需要押金</el-checkbox>
            <template v-if="form.hasDeposit">
              <el-input-number v-model="form.deposit" :min="0" :max="99999" :precision="2" :step="10" controls-position="right" class="price-input deposit-input" />
              <span class="unit-text">元</span>
            </template>
          </el-form-item>

          <el-form-item label="截止时间" prop="endDatetime">
            <el-date-picker v-model="form.endDatetime" type="datetime" placeholder="请选择截止时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" class="full-width" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">发布订单</el-button>
            <el-button @click="handleReset" class="reset-btn">重置</el-button>
          </el-form-item>
        </el-form>
        </el-card>
      </div>
    </el-main>

    <!-- 自定义类型弹窗 -->
    <el-dialog v-model="showTypeDialog" title="添加自定义类型" width="380px" :close-on-click-modal="false" center>
      <el-input
        v-model="newTypeName"
        placeholder="请输入新类型名称"
        maxlength="20"
        show-word-limit
        @keyup.enter="handleAddType"
        ref="typeInputRef"
      />
      <template #footer>
        <el-button @click="showTypeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddType">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { addOrder } from '../../api/order'
import { createPay } from '../../api/pay'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

// 订单类型选项（前端维护，不存数据库）
const typeOptions = ref(['编程', '学习', '设计', '翻译', '其他'])
const showTypeDialog = ref(false)
const newTypeName = ref('')
const typeInputRef = ref(null)

// 打开弹窗后聚焦输入框
const onDialogOpen = () => {
  nextTick(() => typeInputRef.value?.focus())
}

// 添加自定义类型
const handleAddType = () => {
  const name = newTypeName.value.trim()
  if (!name) {
    ElMessage.warning('类型名称不能为空')
    return
  }
  if (typeOptions.value.includes(name)) {
    ElMessage.warning('该类型已存在')
    return
  }
  typeOptions.value.push(name)
  form.type = name
  showTypeDialog.value = false
  newTypeName.value = ''
  ElMessage.success('添加成功')
}

// 弹窗打开时聚焦
watch(showTypeDialog, (v) => { if (v) onDialogOpen() })

const form = reactive({
  title: '',
  content: '',
  type: '',
  senderPrice: 0,
  hasDeposit: false,
  deposit: 0,
  endDatetime: null
})

const rules = {
  title: [{ required: true, message: '请输入订单标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入订单内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择订单类型', trigger: 'change' }],
  senderPrice: [{ required: true, message: '请输入发布方报价', trigger: 'blur' }],
  endDatetime: [{ required: true, message: '请选择截止时间', trigger: 'change' }]
}

const handleBack = () => {
  router.push('/clientMain')
}

const handleReset = () => {
  formRef.value?.resetFields()
  form.hasDeposit = false
  form.deposit = 0
}

// 支付宝表单跳转：与 ClientOrderDetail.vue 中 goAlipay 一致
const goAlipay = (formHtml) => {
  const div = document.createElement('div')
  div.innerHTML = formHtml
  document.body.appendChild(div)
  const form = div.querySelector('form') || document.forms[document.forms.length - 1]
  if (form) form.submit()
  else { document.open(); document.write(formHtml); document.close() }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      type: form.type,
      senderPrice: form.senderPrice,
      endDatetime: form.endDatetime
    }
    if (form.hasDeposit) {
      payload.deposit = form.deposit
    }
    const res = await addOrder(payload)
    const orderId = res.data
    ElMessage.success('订单发布成功!')

    // 弹窗询问是否立即托管赏金
    try {
      await ElMessageBox.confirm(
        `是否立即托管赏金 ¥${form.senderPrice} ？\n未托管赏金的订单不会显示在订单大厅，其他用户无法接单。`,
        '托管赏金',
        {
          confirmButtonText: '立即支付',
          cancelButtonText: '稍后再说',
          type: 'warning'
        }
      )
      // 用户点"立即支付" → 跳转支付宝收银台
      ElMessage.info('即将跳转支付宝...')
      const payRes = await createPay({ orderId, type: 'BOUNTY' })
      goAlipay(payRes.data)
    } catch {
      // 用户点"稍后再说"或关闭弹窗 → 回大厅
      ElMessage.info('可在"我的订单"中随时托管赏金')
      router.push('/clientMain')
    }
  } catch (err) {
    console.error(err)
    ElMessage.error(err.message || '操作失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
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
  padding: 24px;
}
.form-wrap {
  max-width: 760px;
  margin: 0 auto;
}
.form-card {
  border-radius: 16px;
  box-shadow: var(--shadow);
}
.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
}
.card-title::before {
  content: '';
  width: 4px;
  height: 16px;
  border-radius: 3px;
  background: var(--gradient-primary);
}
.full-width {
  width: 100%;
}
.type-select-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
}
.add-type-btn {
  flex-shrink: 0;
}
.price-input {
  width: 200px;
}
.deposit-input {
  margin-left: 12px;
}
.unit-text {
  margin-left: 8px;
  color: var(--text-muted);
}
.reset-btn {
  margin-left: 12px;
}
</style>
