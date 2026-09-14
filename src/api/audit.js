import request from '../utils/request'

/**
 * 查询所有待审核订单（user_complete=1 且 state=1）
 * GET /auditor/selectAuditorAllUserCompleteOrder
 */
export function selectAllAuditorOrder() {
  return request({ url: '/auditor/selectAuditorAllUserCompleteOrder', method: 'get' })
}

/**
 * 查询单个订单详情（审核端）
 * GET /auditor/selectAuditorOneOrder?orderId=xxx
 */
export function selectOneAuditorOrder(params) {
  return request({ url: '/auditor/selectAuditorOneOrder', method: 'get', params })
}

/**
 * 审核订单（1=通过，2=驳回）
 * PUT /auditor/auditorUpdateOrder?orderId=xxx&auditorComplete=1|2
 */
export function auditorUpdateOrder(params) {
  return request({ url: '/auditor/auditorUpdateOrder', method: 'put', params })
}
