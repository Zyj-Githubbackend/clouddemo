import { createRouter, createWebHistory } from 'vue-router'
import { clearAuthStorage, getStoredToken, getStoredUserInfo, isTokenExpired } from '@/utils/auth'

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
    component: () => import('@/views/AnnouncementHome.vue'),
    meta: { title: '首页', requireAuth: true }
  },
  {
    path: '/activities',
    name: 'Activities',
    component: () => import('@/views/ActivityList.vue'),
    meta: { title: '志愿活动', requireAuth: true }
  },
  {
    path: '/announcement/:id',
    name: 'AnnouncementDetail',
    component: () => import('@/views/AnnouncementDetail.vue'),
    meta: { title: '公告详情', requireAuth: true }
  },
  {
    path: '/activity/:id',
    name: 'ActivityDetail',
    component: () => import('@/views/ActivityDetail.vue'),
    meta: { title: '活动详情', requireAuth: true }
  },
  {
    path: '/feedback',
    name: 'FeedbackCreate',
    component: () => import('@/views/FeedbackCreate.vue'),
    meta: { title: '提交反馈', requireAuth: true }
  },
  {
    path: '/my-feedback',
    name: 'MyFeedback',
    component: () => import('@/views/MyFeedback.vue'),
    meta: { title: '我的反馈', requireAuth: true }
  },
  {
    path: '/feedback/:id',
    name: 'FeedbackDetail',
    component: () => import('@/views/FeedbackDetail.vue'),
    meta: { title: '反馈详情', requireAuth: true }
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
        path: 'announcements',
        name: 'AdminAnnouncements',
        component: () => import('@/views/admin/AnnouncementManage.vue'),
        meta: { title: '公告管理' }
      },
      {
        path: 'feedback',
        name: 'AdminFeedback',
        component: () => import('@/views/admin/FeedbackManage.vue'),
        meta: { title: '反馈工单' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理' }
      },
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
  
  const token = getStoredToken()
  const userInfo = getStoredUserInfo()

  if (token && isTokenExpired(token)) {
    clearAuthStorage()
    next({
      path: '/login',
      query: to.path === '/login' ? {} : { redirect: to.fullPath }
    })
    return
  }
  
  // 如果需要登录且没有token，跳转到登录页
  if (to.meta.requireAuth && !token) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
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
  
  next()
})

export default router
