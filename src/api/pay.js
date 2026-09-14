import request from '../utils/request'

/**
 * 创建支付（支付宝沙箱电脑网站支付）
 * @param {Object} params
 * @param {number} params.orderId - 订单ID
 * @param {string} params.type    - BOUNTY=赏金托管  DEPOSIT=押金
 * @returns {Promise} data 为支付宝收银台表单HTML字符串，前端 document.write 后自动跳转
 */
export function createPay(params) {
  return request({
    url: '/pay/create',
    method: 'post',
    params,
    timeout: 30000
  })
}

/**
 * 查询支付状态（后端会主动向支付宝查询并更新订单）
 * @param {Object} params
 * @param {number} params.orderId
 * @param {string} params.type    - BOUNTY / DEPOSIT
 * @returns {Promise} data: "1"=已支付成功  "0"=未支付/处理中
 */
export function getPayStatus(params) {
  return request({
    url: '/pay/status',
    method: 'get',
    params
  })
}
