import request from '../utils/request'

/**
 * 查询全部用户
 * GET /admin/user/AllUser
 */
export function getAllUsers() {
  return request({ url: '/admin/user/AllUser', method: 'get' })
}

/**
 * 管理员新增用户（可带头像）
 * POST /admin/user/addUser
 * @param {FormData} formData - User 字段 + avatar 文件（可选）
 */
export function addUser(formData) {
  return request({
    url: '/admin/user/addUser',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 修改用户状态（0=正常 1=冻结/封禁）
 * PUT /admin/user/updateUserState?userId=&state=
 */
export function updateUserState(params) {
  return request({ url: '/admin/user/updateUserState', method: 'put', params })
}

/**
 * 给用户分配角色
 * PUT /admin/user/addUserRole?userId=&roleId=
 */
export function addUserRole(params) {
  return request({ url: '/admin/user/addUserRole', method: 'put', params })
}

/**
 * 查询用户拥有的角色名列表
 * GET /admin/user/getAllUserRole?userId=
 */
export function getUserRoles(params) {
  return request({ url: '/admin/user/getAllUserRole', method: 'get', params })
}

/**
 * 删除用户的某个角色
 * DELETE /admin/user/deleteUserRole?userId=&roleId=
 */
export function deleteUserRole(params) {
  return request({ url: '/admin/user/deleteUserRole', method: 'delete', params })
}
