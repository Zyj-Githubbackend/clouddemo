import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { clearAuthStorage, getStoredToken, isTokenExpired } from '@/utils/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

let sessionExpiredNotified = false

const redirectToLogin = (message = '登录已过期，请重新登录') => {
  clearAuthStorage()

  if (!sessionExpiredNotified) {
    sessionExpiredNotified = true
    ElMessage.error(message)
    window.setTimeout(() => {
      sessionExpiredNotified = false
    }, 1500)
  }

  if (router.currentRoute.value.path !== '/login') {
    router.replace({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath }
    })
  }
}

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = getStoredToken()
    if (token) {
      if (isTokenExpired(token)) {
        redirectToLogin()
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    
    if (res.code === 200) {
      return res
    } else if (res.code === 401) {
      redirectToLogin(res.message || '登录已过期，请重新登录')
      return Promise.reject(new Error(res.message || '登录已过期'))
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    console.error('请求错误:', error)
    if (error.response?.status === 401) {
      const message = error.response.data?.message || '登录已过期，请重新登录'
      redirectToLogin(message)
      return Promise.reject(new Error(message))
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
