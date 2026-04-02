import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页', requireAuth: true }
  },
  {
    path: '/activities',
    name: 'Activities',
    component: () => import('@/views/ActivityList.vue'),
    meta: { title: '志愿活动', requireAuth: true }
  },
  {
    path: '/activity/:id',
    name: 'ActivityDetail',
    component: () => import('@/views/ActivityDetail.vue'),
    meta: { title: '活动详情', requireAuth: true }
  },
  {
    path: '/my',
    name: 'My',
    component: () => import('@/views/MyCenter.vue'),
    meta: { title: '我的志愿足迹', requireAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人资料', requireAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { title: '管理后台', requireAuth: true, requireAdmin: true },
    children: [
      {
        path: 'activities',
        name: 'AdminActivities',
        component: () => import('@/views/admin/ActivityManage.vue'),
        meta: { title: '活动管理' }
      },
      {
        path: 'create',
        name: 'AdminCreate',
        component: () => import('@/views/admin/ActivityCreate.vue'),
        meta: { title: '发布活动' }
      },
      {
        path: 'checkin',
        name: 'AdminCheckIn',
        component: () => import('@/views/admin/ActivityCheckIn.vue'),
        meta: { title: '活动签到' }
      },
      {
        path: 'confirm',
        name: 'AdminConfirm',
        component: () => import('@/views/admin/HoursConfirm.vue'),
        meta: { title: '时长核销' }
      },
      {
        path: 'hours',
        name: 'AdminHours',
        component: () => import('@/views/admin/VolunteerHours.vue'),
        meta: { title: '时长查询' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 校园志愿服务平台` : '校园志愿服务平台'
  
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  
  // 如果需要登录且没有token，跳转到登录页
  if (to.meta.requireAuth && !token) {
    next('/login')
    return
  }
  
  // 如果需要管理员权限但不是管理员，跳转到首页
  if (to.meta.requireAdmin && userInfo.role !== 'ADMIN') {
    next('/home')
    return
  }
  
  // 如果已经登录但访问登录/注册页面，跳转到首页
  if (token && (to.path === '/login' || to.path === '/register')) {
    next('/home')
    return
  }
  
  // 检查 token 是否有效（可选：增加后端验证）
  // 如果需要更严格的 token 验证，可以在这里添加
  // 例如：调用后端接口验证 token 是否有效
  
  next()
})

export default router
