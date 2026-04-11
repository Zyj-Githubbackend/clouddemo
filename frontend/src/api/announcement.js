import request from '@/utils/request'

export const getHomeAnnouncements = (limit = 5) => {
  return request({
    url: '/announcement/home',
    method: 'get',
    params: { limit }
  })
}

export const getAnnouncementList = (params = {}) => {
  return request({
    url: '/announcement/list',
    method: 'get',
    params: {
      page: params.page ?? 1,
      size: params.size ?? 10
    }
  })
}

export const getAnnouncementDetail = (id) => {
  return request({
    url: `/announcement/${id}`,
    method: 'get'
  })
}

export const getAdminAnnouncementList = (params = {}) => {
  const q = {
    page: params.page ?? 1,
    size: params.size ?? 10
  }
  if (params.status) q.status = params.status
  return request({
    url: '/announcement/admin/list',
    method: 'get',
    params: q
  })
}

export const getAdminAnnouncementDetail = (id) => {
  return request({
    url: `/announcement/admin/${id}`,
    method: 'get'
  })
}

export const createAnnouncement = (data) => {
  return request({
    url: '/announcement/admin',
    method: 'post',
    data
  })
}

export const updateAnnouncement = (id, data) => {
  return request({
    url: `/announcement/admin/${id}`,
    method: 'put',
    data
  })
}

export const publishAnnouncement = (id) => {
  return request({
    url: `/announcement/admin/${id}/publish`,
    method: 'post'
  })
}

export const offlineAnnouncement = (id) => {
  return request({
    url: `/announcement/admin/${id}/offline`,
    method: 'post'
  })
}

export const deleteAnnouncement = (id) => {
  return request({
    url: `/announcement/admin/${id}`,
    method: 'delete'
  })
}

export const uploadAnnouncementImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/announcement/admin/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
