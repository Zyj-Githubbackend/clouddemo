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

// 管理员：分页查询用户
export const getAdminUserList = (params = {}) => {
  return request({
    url: '/user/admin/users',
    method: 'get',
    params
  })
}

// 管理员：查询用户详情
export const getAdminUserDetail = (id) => {
  return request({
    url: `/user/admin/users/${id}`,
    method: 'get'
  })
}

// 管理员：修改用户资料
export const updateAdminUserProfile = (id, data) => {
  return request({
    url: `/user/admin/users/${id}/profile`,
    method: 'put',
    data
  })
}

// 管理员：重置用户密码
export const resetAdminUserPassword = (id, data) => {
  return request({
    url: `/user/admin/users/${id}/password`,
    method: 'put',
    data
  })
}

// 管理员：调整用户角色
export const updateAdminUserRole = (id, data) => {
  return request({
    url: `/user/admin/users/${id}/role`,
    method: 'put',
    data
  })
}

// 管理员：调整用户状态
export const updateAdminUserStatus = (id, data) => {
  return request({
    url: `/user/admin/users/${id}/status`,
    method: 'put',
    data
  })
}
