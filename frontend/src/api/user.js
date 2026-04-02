import request from '@/utils/request'

// 用户登录
export const login = (data) => {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

// 用户注册
export const register = (data) => {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

// 获取用户信息
export const getUserInfo = () => {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

// 更新用户信息
export const updateUserInfo = (data) => {
  return request({
    url: '/user/update',
    method: 'put',
    data
  })
}

// 修改密码
export const updatePassword = (data) => {
  return request({
    url: '/user/updatePassword',
    method: 'put',
    data
  })
}

// 管理员：查询所有志愿者时长（支持 keyword 关键字筛选）
export const getVolunteerHoursList = (keyword = '') => {
  return request({
    url: '/user/admin/hours',
    method: 'get',
    params: keyword ? { keyword } : {}
  })
}
