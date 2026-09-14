# 任务发布平台 接口文档

## 通用约定

| 项目 | 说明 |
|------|------|
| 后端地址 | `http://localhost:8888` |
| 前端开发地址 | `http://localhost:5173`（Vite 代理 /user、/order、/pay 等到 8888） |
| 请求格式 | GET：query 参数；POST/PUT：JSON body 或表单（注明）；文件上传：multipart/form-data |
| 鉴权方式 | 除「公开接口」外，请求头必须携带 `token: <JWT>`（登录返回） |
| 统一响应 | `{ "code": 200, "msg": "success", "data": ... }`，code=200 成功，501/502 业务失败，401 未登录/token 过期 |

### 状态码字典

**订单状态 state**

| 值 | 含义 |
|----|------|
| 0 | 待被接取 |
| 1 | 已被接取 |
| 2 | 已完成 |
| 3 | 已取消 |

**接单人完成 userComplete**：0 未完成 / 1 已提交结果

**审核状态 auditorComplete**：0 待审核 / 1 审核通过 / 2 审核驳回

**赏金支付状态 payState**：0 未托管 / 1 已托管 / 2 已结算 / 3 已退款

**押金支付状态 depositState**：0 未支付 / 1 已支付 / 2 已退还

**支付类型 type**：`BOUNTY` 赏金托管 / `DEPOSIT` 接单押金

### OrderVO 字段（订单相关接口返回）

| 字段 | 说明 |
|------|------|
| orderId | 订单ID |
| title / content / type | 标题 / 内容 / 类型（编程、学习、设计、翻译、其他） |
| senderName / getterName | 发单人昵称 / 接单人昵称 |
| senderPrice | 发布方报价（赏金） |
| deposit | 押金金额（无押金为 null） |
| state / userComplete / auditorComplete | 订单状态 / 接单人完成 / 审核状态 |
| documentPath | 接单人提交的交付结果路径 |
| payState / depositState | 赏金支付状态 / 押金支付状态 |
| releaseDatetime / endDatetime / completeDatetime / updateDatetime | 发布 / 截止 / 完成 / 更新时间 |

---

## 一、/user 认证与账号

### 公开接口（无需 token）

**1. 客户端登录**
- `POST /user/userLogin`
- body：`{ "phone": "199xxxxxxxx", "password": "xxx" }`
- 返回 data：`{ role, token, username, ... }`

**2. 管理端登录**
- `POST /user/adminLogin`
- body：同上（账号需具备管理员/审核员/客服角色）

**3. 注册-获取验证码**
- `GET /user/registerGetCode?phone=199xxxxxxxx`
- 返回 data：验证码字符串（开发环境直接返回，有效期 60 秒）

**4. 注册-校验验证码**
- `GET /user/registerCheckCode?phone=xxx&checkCode=1234`

**5. 注册**
- `POST /user/register`
- body：`{ "phone": "xxx", "password": "xxx", "username": "xxx" }`（可选 multipart 字段 `avatar` 头像文件）
- 新用户默认角色为普通用户（roleId=4）

**6. 忘记密码-获取验证码**
- `GET /user/forgetPasswordGetCode?phone=xxx`

**7. 忘记密码-校验验证码**
- `GET /user/forgetPasswordCheckCode?phone=xxx&checkCode=1234`

**8. 忘记密码-重置密码**
- `PUT /user/updatePassword?phone=xxx&newPassword=xxx&againPassword=xxx`

### 需登录接口

**9. getUser（查看当前登录用户信息）**
- `GET /user/getUser`
- 返回 data：UserVO（含 userId、username、nickname、phone、imagePath、sex、age、email、address 等）

**10. getProfileUser（查看他人信息）**
- `GET /user/getProfileUser?username=xxx`

**11. updateUser（修改个人信息）**
- `PUT /user/updateUser`
- 表单（multipart/form-data 或 urlencoded）：
  - 客户端修改自己：**不传 userId**，后端从 token 获取；可带 `avatar` 头像文件；可改字段 username、nickname、sex、age、email、address
  - 管理端修改他人：额外传 `adminUserId=<目标用户ID>`（仅管理员角色可用）
  - phone、password、position、state 等敏感字段不允许通过此接口修改

**12. updatePhone（修改手机号）**
- `PUT /user/updatePhone?phone=新手机号`

---

## 二、/order 订单

**1. AllOrder（查询大厅所有可接订单）**
- `GET /order/AllOrder`
- 返回：state=0（待被接取）的订单列表

**2. searchOrder（搜索订单）**
- `GET /order/searchOrder?searchNum=关键词`
- 关键词匹配标题、发单人、接单人、类型

**3. Filter（筛选订单）**
- `GET /order/Filter?type=编程&minPrice=10&maxPrice=100&sort=asc`
- 参数均可选：type 类型、minPrice/maxPrice 价格区间、sort（asc 升序 / desc 降序，不传无序）

**4. getterUpdateOrder（接单人接单）**
- `PUT /order/getterUpdateOrder?orderId=15`
- 从 token 获取 userId 写入 getterId，订单 state：0 → 1

**5. CancelGetterUpdateOrder（接单人取消接单）**
- `PUT /order/CancelGetterUpdateOrder?orderId=15`
- 仅接该订单的用户可取消；订单回到 state=0；**押金已支付则自动原路退还**

**5-2. revokeComplete（接单人撤销"已完成"）**
- `PUT /order/revokeComplete?orderId=15`
- 仅该订单的接单人本人可操作，仅 state=2（已完成）可撤销
- 撤销后：state 回退为 1（已被接取）、userComplete/auditorComplete 清零、完成时间清空、赏金状态重置为已托管（payState=1），接单人需重新提交交付路径并再次审核；押金已在审核通过时退还，不重复处理

**6. senderUpdateOrder（发单人修改订单）**
- `PUT /order/senderUpdateOrder`
- body（JSON）：`{ orderId, title, content, type, senderPrice, deposit, endDatetime }`
- 仅发布者本人可修改；仅 state=0 待接取时可改

**7. resultUpdateOrder（提交交付结果）**
- `PUT /order/resultUpdateOrder?orderId=15&documentPath=结果文件路径或链接`
- 接单人提交完成路径，userComplete 置 1，进入待审核队列

**8. deleteOrder（发单人删除/取消订单）**
- `DELETE /order/deleteOrder?orderId=15`
- 仅发布者本人可删自己的订单；**赏金已托管则自动原路退还**

**9. getAllSenderUserOrderByState（我发布的订单）**
- `GET /order/getAllSenderUserOrderByState?state=0`
- state 可选：0/1/2/3 对应四种状态；**不传 state 查全部**

**10. getAllGetterUserOrderByState（我接取的订单）**
- `GET /order/getAllGetterUserOrderByState?state=1`
- 同上；接单人视角（无 state=0 的待接取订单）

**11. OneOrder（订单详情）**
- `GET /order/OneOrder?orderId=15`
- 返回 OrderVO 完整信息

**12. addOrder（发布订单）**
- `POST /order/addOrder`
- body（JSON）：
```json
{
  "title": "C++作业",
  "content": "完成xxx",
  "type": "设计",
  "senderPrice": 11111,
  "deposit": 500,
  "endDatetime": "2026-09-30 00:00:00"
}
```
- senderId 从 token 获取；deposit 押金可选（无押金不传或传 null）；发布后 state=0，赏金需再调 /pay/create 托管

---

## 三、/pay 支付（支付宝沙箱）

**1. create（发起支付，跳转收银台）**
- `POST /pay/create?orderId=15&type=BOUNTY`
- type：`BOUNTY` 赏金托管（发单人付）/ `DEPOSIT` 接单押金（接单人付）
- 逻辑：校验金额与支付状态 → 生成商户订单号写入 t_payment 流水 → 调用支付宝电脑网站支付（alipay.trade.page.pay）
- 返回 data：支付宝收银台表单 HTML 字符串，前端 `document.write(form)` 自动跳转沙箱收银台

**2. status（查询支付结果）**
- `GET /pay/status?orderId=15&type=BOUNTY`
- 后端调用支付宝 alipay.trade.query 查询真实交易状态：
  - 赏金支付成功 → payState=1（已托管）
  - 押金支付成功 → depositState=1（已支付），并**自动完成接单**（getterId=当前用户、state=1）
- 返回 data：`"1"` 支付成功 / `"0"` 未支付或处理中（前端结果页轮询调用）

> **退款不单独开放接口**，在以下场景自动触发（支付宝 alipay.trade.refund 原路退回）：
> - 接单人取消接单 → 押金退还
> - 发单人取消订单 → 赏金退还（押金若已付也退还）
> - 审核员审核通过 → 押金退还接单人；赏金账面结算给接单人（payState=2，写 SETTLE 流水）

---

## 四、/auditor 审核端

**1. selectManagerAllUserComplete（待审核订单列表）**
- `GET /auditor/selectAuditorAllUserCompleteOrder`
- 返回所有 `userComplete=1 且 state=1`（接单人已提交结果、待审核）的订单

**2. auditorUpdateOrder（审核订单）**
- `PUT /auditor/auditorUpdateOrder?orderId=15&auditorComplete=1`
- auditorComplete：**1 通过 / 2 驳回**
- 通过：state → 2（已完成）、记录完成时间；同时事务内执行资金结算——押金原路退还接单人（depositState=2）、托管赏金结算给接单人（payState=2）；赏金未托管则报错回滚
- 驳回：auditorComplete=2、userComplete 重置为 0，接单人修改后可重新提交

**3. selectManegerOneOrder（审核端订单详情）**
- `GET /auditor/selectAuditorOneOrder?orderId=15`

---

## 五、/Feedback 问题反馈

> 实际路径前缀为 `/Feedback`（首字母大写）

**1. addFeedback（提交反馈）**
- `POST /Feedback/addFeedback?content=反馈内容`

**2. getFeedback（查看单条反馈）**
- `GET /Feedback/getFeedback?feedbackId=1`

**3. getUserAllFeedbackSolve（我的反馈列表）**
- `GET /Feedback/getUserAllFeedback?solve=1`
- solve：1 已解决 / 0 未解决 / 不传查全部

**4. updateUserFeedbackByContent（修改反馈内容）**
- `PUT /Feedback/updateUserFeedbackByContent?feedbackId=1&content=新内容`

**5. updateUserFeedbackBySolve（标记反馈是否解决）**
- `PUT /Feedback/updateUserFeedbackBySolve?feedbackId=1&solve=1`

**6. deleteUserFeedback（删除反馈）**
- `DELETE /Feedback/deleteUserFeedback?feedbackId=1`

**7. AllFeedback（查看所有反馈）**
- `GET /Feedback/AllFeedback?type=xxx`

---

## 六、/admin/user 用户管理（管理员）

**1. AllUser（查询所有用户）**
- `GET /admin/user/AllUser`

**2. addUser（添加用户）**
- `POST /admin/user/addUser`
- 表单：User 字段（phone、password、username 等）+ 可选 `avatar` 文件；可指定角色（普通用户/审核员/客服）

**3. updateUserState（用户状态管理）**
- `PUT /admin/user/updateUserState?userId=1&state=0`
- state：0 正常 / 1 冻结 / 2 封禁

**4. addUserRole（给用户分配角色）**
- `PUT /admin/user/addUserRole?userId=1&roleId=2`

**5. getAllUserRole（查看用户的所有角色）**
- `GET /admin/user/getAllUserRole?userId=1`

**6. deleteUserRole（删除用户角色）**
- `DELETE /admin/user/deleteUserRole?userId=1&roleId=2`

---

## 七、/admin/menu 菜单权限管理（管理员）

**1. addMenu（新建菜单/权限）**
- `POST /admin/menu/addMenu`
- body（JSON）：Menu 对象（menuName、权限标识等）

**2. UserAllMenu（查看指定用户的菜单权限）**
- `GET /admin/menu/userAllMenu?userId=1`

**3. getMenu（查看单个菜单）**
- `GET /admin/menu/getMenu?menuId=1`

**4. deleteMenu（删除菜单）**
- `DELETE /admin/menu/deleteMenu?menuId=1`
- 需先删除 menu_role 表中与该菜单关联的数据

**5. AllMenu（查看所有菜单）**
- `GET /admin/menu/AllMenu`

**6. addMenuRole（给角色分配菜单）**
- `POST /admin/menu/addMenuRole`
- 参数：MenuRole（roleId、menuId）

**7. updateMenu（修改菜单）**
- `PUT /admin/menu/updateMenu`
- 参数：Menu 对象

**8. selectMenuRole（查询指定角色的所有菜单）**
- `GET /admin/menu/selectMenuRole?roleId=2`

**9. deleteMenuRole（删除角色菜单关联）**
- `DELETE /admin/menu/deleteMenuRole`
- 参数：MenuRole（roleId、menuId）

---

## 附：完整支付业务流程

```
发单人发布订单(addOrder)
   └─ 托管赏金 POST /pay/create?type=BOUNTY → 支付宝付款 → 跳回 /payResult 轮询 status
        payState: 0 → 1
接单人接单（有押金时）
   └─ 付押金 POST /pay/create?type=DEPOSIT → 支付宝付款 → status 确认后自动接单
        depositState: 0 → 1，state: 0 → 1
接单人完成任务
   └─ PUT /order/resultUpdateOrder 提交路径 → userComplete=1，进入审核队列
审核员审核 PUT /auditor/auditorUpdateOrder
   ├─ 通过(auditorComplete=1)：state=2，押金退还接单人(depositState=2)，赏金结算(payState=2)
   └─ 驳回(auditorComplete=2)：userComplete=0，接单人修改后重新提交
中途取消
   ├─ 接单人取消：押金退还，订单回 state=0
   └─ 发单人取消：赏金（及押金）退还，订单删除
```
