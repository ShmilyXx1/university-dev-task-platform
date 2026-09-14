import request from '../utils/request'

/**
 * 获取所有订单
 * GET /order/AllOrder
 */
export function selectAllOrder() {
  return request({ url: '/order/AllOrder', method: 'get' })
}

/**
 * 客服/管理员：查询全部订单（所有状态），可按状态/关键字筛选
 * GET /order/AllOrderForService?state=&keyword=
 */
export function selectAllOrderForService(params) {
  return request({ url: '/order/AllOrderForService', method: 'get', params })
}

/**
 * 搜索订单
 * GET /order/searchOrder?searchNum=xxx
 */
export function searchOrder(params) {
  return request({ url: '/order/searchOrder', method: 'get', params })
}

/**
 * 筛选订单
 * GET /order/Filter?type=&minPrice=&maxPrice=&sort=
 */
export function filterOrders(params) {
  return request({ url: '/order/Filter', method: 'get', params })
}

/**
 * 接单（接单人接单）
 * PUT /order/getterUpdateOrder?orderId=xxx
 */
export function getterUpdateOrder(params) {
  return request({ url: '/order/getterUpdateOrder', method: 'put', params })
}

/**
 * 取消接单
 * PUT /order/CancelGetterUpdateOrder?orderId=xxx
 */
export function cancelGetterUpdateOrder(params) {
  return request({ url: '/order/CancelGetterUpdateOrder', method: 'put', params })
}

/**
 * 接单人撤销"已完成"（state=2 回退为 state=1，需重新提交结果）
 * PUT /order/revokeComplete?orderId=xxx
 */
export function revokeComplete(params) {
  return request({ url: '/order/revokeComplete', method: 'put', params })
}

/**
 * 发单人更新订单
 * PUT /order/senderUpdateOrder
 */
export function senderUpdateOrder(data) {
  return request({ url: '/order/senderUpdateOrder', method: 'put', data })
}

/**
 * 审核结果更新订单
 * PUT /order/resultUpdateOrder?orderId=&documentPath=
 */
export function resultUpdateOrder(params) {
  return request({ url: '/order/resultUpdateOrder', method: 'put', params })
}

/**
 * 删除订单
 * DELETE /order/deleteOrder?orderId=xxx
 */
export function deleteOrder(params) {
  return request({ url: '/order/deleteOrder', method: 'delete', params })
}

/**
 * 新增订单
 * POST /order/addOrder
 */
export function addOrder(data) {
  return request({ url: '/order/addOrder', method: 'post', data })
}

/**
 * 获取单个订单
 * GET /order/OneOrder?orderId=xxx
 */
export function selectOneOrder(params) {
  return request({ url: '/order/OneOrder', method: 'get', params })
}

/**
 * 获取发单人的所有订单（按状态筛选）
 * GET /order/getAllSenderUserOrderByState?state=xxx
 */
export function getSenderAllUserOrderByState(params) {
  return request({ url: '/order/getAllSenderUserOrderByState', method: 'get', params })
}

/**
 * 获取接单人的所有订单（按状态筛选）
 * GET /order/getAllGetterUserOrderByState?state=xxx
 */
export function getGetterAllUserOrderByState(params) {
  return request({ url: '/order/getAllGetterUserOrderByState', method: 'get', params })
}
