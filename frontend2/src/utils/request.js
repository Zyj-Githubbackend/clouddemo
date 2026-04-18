import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
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
      // Token 失效或未认证，清除本地存储并跳转到登录页
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 也清除 Pinia store 中的数据
      window.location.reload() // 重新加载页面以重置应用状态
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new Error(res.message || '登录已过期'))
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    console.error('请求错误:', error)
    // 如果是网络错误或其他异常，不进行特殊处理
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
