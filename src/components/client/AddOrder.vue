<template>
  <el-container style="height: 100vh; background: #f5f7fa">
    <el-header style="background: #fff; display: flex; align-items: center; padding: 0 20px; border-bottom: 1px solid #e4e7ed">
      <el-button @click="handleBack" :icon="ArrowLeft" circle />
      <span style="margin-left: 12px; font-size: 18px; font-weight: bold">发布订单</span>
    </el-header>

    <el-main style="padding: 20px; display: flex; justify-content: center">
      <el-card style="width: 680px" shadow="hover">
        <template #header>
          <span style="font-size: 16px; font-weight: 600">填写订单信息</span>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="订单标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入订单标题" maxlength="50" show-word-limit />
          </el-form-item>

          <el-form-item label="订单内容" prop="content">
            <el-input v-model="form.content" type="textarea" placeholder="请详细描述订单需求内容" :rows="4" maxlength="500" show-word-limit />
          </el-form-item>

          <el-form-item label="订单类型" prop="type">
            <el-select v-model="form.type" placeholder="请选择订单类型" style="width: 100%">
              <el-option label="编程" value="编程" />
              <el-option label="学习" value="学习" />
              <el-option label="设计" value="设计" />
              <el-option label="翻译" value="翻译" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>

          <el-form-item label="发布方报价" prop="senderPrice">
            <el-input-number v-model="form.senderPrice" :min="0" :max="99999" :precision="2" :step="10" controls-position="right" style="width: 200px" />
            <span style="margin-left: 8px; color: #999">元</span>
          </el-form-item>

          <el-form-item label="押金">
            <el-checkbox v-model="form.hasDeposit">需要押金</el-checkbox>
            <template v-if="form.hasDeposit">
              <el-input-number v-model="form.deposit" :min="0" :max="99999" :precision="2" :step="10" controls-position="right" style="width: 200px; margin-left: 12px" />
              <span style="margin-left: 8px; color: #999">元</span>
            </template>
          </el-form-item>

          <el-form-item label="截止时间" prop="endDatetime">
            <el-date-picker v-model="form.endDatetime" type="datetime" placeholder="请选择截止时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">发布订单</el-button>
            <el-button @click="handleReset" style="margin-left: 12px">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { addOrder } from '../../api/order'
import { createPay } from '../../api/pay'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

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
