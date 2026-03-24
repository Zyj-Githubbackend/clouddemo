import request from '@/utils/request'

// 获取活动列表（空字符串 status/category 不传，避免后端按 "" 精确匹配导致无数据）
export const getActivityList = (params = {}) => {
  const q = {
    page: params.page ?? 1,
    size: params.size ?? 10
  }
  if (params.status) q.status = params.status
  if (params.category) q.category = params.category
  if (params.recruitmentPhase) q.recruitmentPhase = params.recruitmentPhase
  return request({
    url: '/activity/list',
    method: 'get',
    params: q
  })
}

// 获取活动详情
export const getActivityDetail = (id) => {
  return request({
    url: `/activity/${id}`,
    method: 'get'
  })
}

// 管理员删除活动
export const deleteActivity = (id) => {
  return request({
    url: `/activity/${id}`,
    method: 'delete'
  })
}

// 创建活动
export const createActivity = (data) => {
  return request({
    url: '/activity/create',
    method: 'post',
    data
  })
}

// 报名活动
export const registerActivity = (activityId) => {
  return request({
    url: `/activity/register/${activityId}`,
    method: 'post'
  })
}

// 我的报名记录
export const getMyRegistrations = () => {
  return request({
    url: '/activity/myRegistrations',
    method: 'get'
  })
}

// 核销时长
export const confirmHours = (registrationId) => {
  return request({
    url: `/activity/confirmHours/${registrationId}`,
    method: 'post'
  })
}

// 管理员：全部报名列表（可选 activityId 查询参数，筛选某一活动）
export const getAdminRegistrations = (activityId) => {
  return request({
    url: '/activity/admin/registrations',
    method: 'get',
    params: activityId != null && activityId !== '' ? { activityId } : {}
  })
}

// 管理员：指定活动的报名列表
export const getActivityRegistrations = (activityId) => {
  return request({
    url: `/activity/${activityId}/registrations`,
    method: 'get'
  })
}

// AI生成文案
export const generateDescription = (data) => {
  return request({
    url: '/activity/ai/generate',
    method: 'post',
    data
  })
}
