import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layout/AdminLayout.vue'
import AuditLayout from '../layout/AuditLayout.vue'
import ServiceLayout from '../layout/ServiceLayout.vue'

// 路由表
const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../components/Login.vue')
  },
  {
    path: '/clientLogin',
    name: 'ClientLogin',
    component: () => import('../components/client/ClientLogin.vue')
  },
  {
    path: '/clientRegister',
    name: 'ClientRegister',
    component: () => import('../components/client/Register.vue')
  },
  {
    path: '/clientSetAccount',
    name: 'ClientSetAccount',
    component: () => import('../components/client/ClientSetAccount.vue')
  },
  {
    path: '/clientForgetPassword',
    name: 'ClientForgetPassword',
    component: () => import('../components/client/forgetPassword.vue')
  },
  {
    path: '/clientResetPassword',
    name: 'ClientResetPassword',
    component: () => import('../components/client/ClientResetPassword.vue')
  },
  {
    path: '/clientMain',
    name: 'ClientMain',
    component: () => import('../components/client/ClientMain.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/clientAddOrder',
    name: 'ClientAddOrder',
    component: () => import('../components/client/AddOrder.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/clientProfile',
    name: 'ClientProfile',
    component: () => import('../components/client/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/clientOrderDetail/:orderId',
    name: 'ClientOrderDetail',
    component: () => import('../components/client/ClientOrderDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/clientMyOrders',
    name: 'ClientMyOrders',
    component: () => import('../components/client/ClientMyOrders.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/payResult',
    name: 'PayResult',
    component: () => import('../components/client/PayResult.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/clientUserProfile/:username',
    name: 'ClientUserProfile',
    component: () => import('../components/client/ClientUserProfile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/clientFeedback',
    name: 'ClientFeedback',
    component: () => import('../components/client/ClientFeedback.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/clientChat',
    name: 'ClientChat',
    component: () => import('../components/client/ClientChat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/clientP2PChat',
    name: 'ClientP2PChat',
    component: () => import('../components/client/ClientP2PChat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/getCode',
    name: 'GetCode',
    component: () => import('../components/GetCode.vue')
  },
  {
    path: '/forgetPassword',
    name: 'ForgetPassword',
    component: () => import('../components/ForgetPassword.vue')
  },


  {
    path: '/audit',
    component: AuditLayout,
    redirect: '/audit/main',
    children: [
      {
    path: '/auditMain',
    name: 'AuditMain',
    component: () => import('../components/audit/AuditMain.vue'),
    meta:{requiresAuth:true, roles:['审核员','管理员']}
     }
   ]
  },

  {
    path: '/service',
    component: ServiceLayout,
    redirect: '/service/main',
    children: [
      {
        path: '/serviceMain',
        name: 'ServiceMain',
        component: () => import('../components/service/ServiceMain.vue'),
        meta:{requiresAuth:true, roles:['客服','管理员']}
      },
      {
        path: '/serviceOrder',
        name: 'ServiceOrder',
        component: () => import('../components/service/ServiceOrder.vue'),
        meta:{requiresAuth:true, roles:['客服','管理员']}
      },
      {
        path: '/serviceFeedback',
        name: 'ServiceFeedback',
        component: () => import('../components/service/ServiceFeedback.vue'),
        meta:{requiresAuth:true, roles:['客服','管理员']}
      }
    ]
  },


  // 后台布局嵌套路由
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/main',
    children: [
      {
        path: '/adminMain',
        name: 'AdminMain',
        component: () => import('../components/admin/AdminMain.vue'),
        meta:{requiresAuth:true, roles:['管理员']}
      },
      {
        path: '/adminUser',
        name: 'AdminUser',
        component: () => import('../components/admin/AdminUser.vue'),
        meta:{requiresAuth:true, roles:['管理员']}
      },
      {
        path: '/adminFeedback',
        name: 'AdminFeedback',
        component: () => import('../components/admin/AdminFeedback.vue'),
        meta:{requiresAuth:true, roles:['管理员']}
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 角色对应的默认首页（角色不符被拦截时，跳回自己的首页）
const roleHomeMap = {
  '管理员': '/adminMain',
  '审核员': '/auditMain',
  '客服': '/serviceMain'
}

// 全局前置路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 从本地存储恢复token
  if (!userStore.token) {
    const localToken = localStorage.getItem('token')
    if (localToken) userStore.setToken(localToken)
  }
  // 从本地存储恢复role
  if (!userStore.role) {
    const localRole = localStorage.getItem('role')
    if (localRole) userStore.setRole(localRole)
  }

  // 判断该路由是否需要登录权限
  if (to.meta.requiresAuth) {
    // token 不存在或已过期（本地校验 JWT exp）都视为未登录
    if (userStore.token && !userStore.isTokenExpired) {
      // 角色校验：管理端/审核端/客服端页面要求特定角色
      if (to.meta.roles && !to.meta.roles.includes(userStore.role)) {
        ElMessage.error('无权访问该页面')
        next(roleHomeMap[userStore.role] || '/clientMain')
        return
      }
      next()
    } else {
      // 有过期 token 先清掉，避免残留
      if (userStore.token) userStore.logout()
      // 客户端页面（含支付结果页）回客户端登录，其余回管理端登录
      next((to.path.startsWith('/client') || to.path.startsWith('/payResult')) ? '/clientLogin' : '/login')
    }
  } else {
    next()
  }
})

export default router
