# 大学开发任务众包平台

毕业设计：由 Spring Boot 和 Vue3 提供支持的大学开发任务众包平台。支持用户发布开发任务、在线接单、客服实时沟通、任务审核与支付宝沙箱在线支付。

## 技术栈

**前端**

- 框架：Vue 3（`<script setup>`）+ Vite
- UI 组件：Element Plus（按需自动引入）
- 状态管理：Pinia
- 路由：Vue Router 4
- 数据请求：Axios
- 实时通信：WebSocket（在线客服 / P2P 私信）

**后端**

- 框架：Spring Boot 2.7.18（JDK 1.8）
- 安全框架：Spring Security + JWT（JJWT 0.11.5）
- ORM：MyBatis-Plus 3.5.3
- 数据库：MySQL 8
- 缓存：Redis（登录态 / Token 管理）
- 消息队列：RabbitMQ
- 实时通信：Spring WebSocket
- 支付：支付宝沙箱 SDK（alipay-sdk-java 4.40.272）
- 工具：Lombok、Fastjson、Maven

## 功能模块

- 用户端：注册登录、忘记/重置密码、个人资料、头像上传
- 任务订单：发布任务、接单、我的订单、订单详情
- 支付：赏金托管与押金支付（支付宝沙箱）、支付结果回调
- 即时通讯：用户与客服在线聊天、用户间 P2P 私信、消息通知
- 反馈：用户提交服务反馈
- 管理端：用户管理、反馈处理
- 审核端：任务/内容审核
- 鉴权：基于 JWT + Redis 的登录拦截，`@PreAuthorize` 角色权限控制（管理员/客服/审核员等）

## 项目结构

```
.
├── src/                    # Vue3 前端源码
│   ├── api/                # Axios 接口封装
│   ├── components/         # 页面组件（client 客户端 / admin 管理端 / audit 审核端）
│   ├── router/             # 路由配置
│   └── stores/             # Pinia 状态管理
├── vite.config.js          # Vite 配置（含 /user、/order、/pay、/ws 等代理到 8888）
└── task/                   # Spring Boot 后端源码（Maven 工程）
    ├── pom.xml
    └── src/main/
        ├── java/com/svtu/  # controller / service / mapper / entity / config / websocket ...
        └── resources/
            ├── application.properties
            └── sql/
                └── db_task_init.sql   # 完整建库脚本（表结构+演示数据，含支付模块）
```

## 快速开始

### 环境要求

- JDK 1.8、Maven 3.6+
- MySQL 8.x、Redis、RabbitMQ
- Node.js 16+

### 1. 数据库准备

脚本位于 `task/src/main/resources/sql/`：

直接执行 `db_task_init.sql`，会自动创建 `db_task` 数据库、全部 7 张表（含支付模块的 `t_payment` 表与 `t_order` 支付字段），并写入角色、测试账号等演示数据：

```bash
mysql -uroot -p < task/src/main/resources/sql/db_task_init.sql
```

### 2. 启动后端

按需修改 `task/src/main/resources/application.properties` 中的 MySQL、Redis、RabbitMQ 连接信息；如需联调支付，填入你自己的[支付宝沙箱](https://open.alipay.com/develop/sandbox/app)参数：

```properties
alipay.app-id=你的沙箱APPID
alipay.app-private-key=你的沙箱应用私钥
alipay.alipay-public-key=你的沙箱支付宝公钥
```

然后用 IDEA 运行 `com.svtu.App`，或在 `task/` 目录执行：

```bash
mvn spring-boot:run
```

后端端口：**8888**

### 3. 启动前端

项目根目录执行：

```bash
npm install
npm run dev
```

启动后访问：http://localhost:5173

前端已在 `vite.config.js` 中配置代理，`/user`、`/order`、`/pay`、`/admin`、`/ws` 等请求会自动转发到后端 8888 端口，无需处理跨域。

## 说明

- 仓库中的 `application.properties` 不包含真实支付宝密钥，仅保留占位符，请勿向公开仓库提交私钥。
- 前端构建：`npm run build`；后端打包：`mvn clean package`。
