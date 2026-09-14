import request from '../utils/request'

/**
 * 提交问题反馈（用户）
 * POST /Feedback/addFeedback?content=xxx
 */
export function addFeedback(params) {
  return request({ url: '/Feedback/addFeedback', method: 'post', params })
}

/**
 * 查询我的反馈列表（用户）
 * GET /Feedback/getUserAllFeedback?solve=0|1（不传=全部）
 */
export function getMyFeedback(params) {
  return request({ url: '/Feedback/getUserAllFeedback', method: 'get', params })
}

/**
 * 用户确认反馈是否已解决（1=已解决，0=未解决/重新打开）
 * PUT /Feedback/updateUserFeedbackBySolve?feedbackId=&solve=
 */
export function updateFeedbackSolve(params) {
  return request({ url: '/Feedback/updateUserFeedbackBySolve', method: 'put', params })
}

/**
 * 删除我的反馈（用户）
 * DELETE /Feedback/deleteUserFeedback?feedbackId=
 */
export function deleteFeedback(params) {
  return request({ url: '/Feedback/deleteUserFeedback', method: 'delete', params })
}

/**
 * 查询全部用户反馈（客服/管理员）
 * GET /Feedback/AllFeedback?type=0|1（不传=全部）
 */
export function getAllFeedback(params) {
  return request({ url: '/Feedback/AllFeedback', method: 'get', params })
}

/**
 * 客服回复反馈
 * PUT /Feedback/serviceReply?feedbackId=&reply=
 */
export function serviceReply(params) {
  return request({ url: '/Feedback/serviceReply', method: 'put', params })
}
