import request from '../utils/request'

/**
 * 后台登录接口
 * POST /user/adminLogin
 * @param {Object} data
 * @param {string} data.phone - 账号或手机号
 * @param {string} data.password - 密码
 * @returns 响应中 data.role 决定跳转主页
 */
export function login(data) {
  return request({ url: '/user/adminLogin', method: 'post', data })
}

/**
 * 客户端登录接口
 * POST /user/userLogin
 * @param {Object} data
 * @param {string} data.phone - 手机号
 * @param {string} data.password - 密码
 */
export function clientLogin(data) {
  return request({ url: '/user/userLogin', method: 'post', data })
}

/**
 * 注册 - 获取验证码
 * GET /user/registerGetCode?phone=xxx
 */
export function registerGetCode(params) {
  return request({ url: '/user/registerGetCode', method: 'get', params })
}

/**
 * 注册 - 验证验证码
 * GET /user/registerCheckCode?phone=xxx&checkCode=xxx
 */
export function registerCheckCode(params) {
  return request({ url: '/user/registerCheckCode', method: 'get', params })
}

/**
 * 注册
 * POST /user/register
 * 后端: @RequestBody User user + @RequestParam(value="avatar",required=false) MultipartFile avatar
 * - 无头像时:直接 JSON body 提交,Content-Type: application/json
 * - 有头像时:后端需要改成 @RequestPart,此处先按无头像 JSON 提交(头像无法在 JSON body 中传,需后端调整或独立接口)
 * @param {Object} userData - User 对象字段 { phone,username,password,nickname,sex,age,email,address }
 */
export function register(userData) {
  return request({
    url: '/user/register',
    method: 'post',
    data: userData,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 忘记密码 - 获取验证码
 * GET /user/forgetPasswordGetCode?phone=xxx
 */
export function sendCode(params) {
  return request({ url: '/user/forgetPasswordGetCode', method: 'get', params })
}

/**
 * 忘记密码 - 验证验证码
 * GET /user/forgetPasswordCheckCode?phone=xxx&checkCode=xxx
 */
export function checkCode(params) {
  return request({ url: '/user/forgetPasswordCheckCode', method: 'get', params })
}

/**
 * 修改密码
 * PUT /user/updatePassword?phone=&newPassword=&againPassword=
 * 后端用 query 参数接收
 */
export function updatePassword(params) {
  return request({ url: '/user/updatePassword', method: 'put', params })
}

/**
 * 获取当前登录用户信息（含 imagePath、username 等）
 * GET /user/getUser
 */
export function getUserInfo() {
  return request({ url: '/user/getUser', method: 'get' })
}

/**
 * 修改当前登录用户信息（含头像）
 * PUT /user/updateUser
 * 后端: @RequestPart User user + @RequestParam("avatar") MultipartFile + @RequestParam("adminUserId")
 * 客户端使用: FormData, 字段包括 username/nickname/sex/age/email/address/imagePath + avatar文件
 */
export function updateUser(formData) {
  return request({
    url: '/user/updateUser',
    method: 'put',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 修改手机号
 * PUT /user/updatePhone?phone=xxx
 */
export function updatePhone(params) {
  return request({ url: '/user/updatePhone', method: 'put', params })
}

/**
 * 按用户名查询他人公开信息
 * GET /user/getProfileUser?username=xxx
 */
export function getProfileUser(params) {
  return request({ url: '/user/getProfileUser', method: 'get', params })
}
