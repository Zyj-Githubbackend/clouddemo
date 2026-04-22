import axios from 'axios'
import request from '@/utils/request'
import { getValidToken } from '@/utils/auth'

export const createFeedback = (data) => {
  return request({
    url: '/feedback',
    method: 'post',
    data
  })
}

export const getMyFeedback = (params = {}) => {
  const q = {
    page: params.page ?? 1,
    size: params.size ?? 10
  }
  if (params.status) q.status = params.status
  return request({
    url: '/feedback/my',
    method: 'get',
    params: q
  })
}

export const getFeedbackDetail = (id) => {
  return request({
    url: `/feedback/${id}`,
    method: 'get'
  })
}

export const replyMyFeedback = (id, data) => {
  return request({
    url: `/feedback/${id}/messages`,
    method: 'post',
    data
  })
}

export const closeMyFeedback = (id) => {
  return request({
    url: `/feedback/${id}/close`,
    method: 'post'
  })
}

export const uploadFeedbackAttachment = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/feedback/attachments',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getAdminFeedbackList = (params = {}) => {
  const q = {
    page: params.page ?? 1,
    size: params.size ?? 10
  }
  if (params.status) q.status = params.status
  if (params.category) q.category = params.category
  if (params.priority) q.priority = params.priority
  if (params.keyword) q.keyword = params.keyword
  return request({
    url: '/feedback/admin/list',
    method: 'get',
    params: q
  })
}

export const getAdminFeedbackDetail = (id) => {
  return request({
    url: `/feedback/admin/${id}`,
    method: 'get'
  })
}

export const replyFeedbackAsAdmin = (id, data) => {
  return request({
    url: `/feedback/admin/${id}/messages`,
    method: 'post',
    data
  })
}

export const closeFeedbackAsAdmin = (id) => {
  return request({
    url: `/feedback/admin/${id}/close`,
    method: 'post'
  })
}

export const rejectFeedbackAsAdmin = (id, reason) => {
  return request({
    url: `/feedback/admin/${id}/reject`,
    method: 'post',
    data: { reason }
  })
}

export const updateFeedbackPriority = (id, priority) => {
  return request({
    url: `/feedback/admin/${id}/priority`,
    method: 'post',
    data: { priority }
  })
}

export const getFeedbackAttachmentBlob = (objectKey, fileName) => {
  const token = getValidToken()
  return axios({
    url: '/api/feedback/attachments',
    method: 'get',
    responseType: 'blob',
    params: { objectKey, fileName },
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
}

export const downloadFeedbackAttachment = async (attachment) => {
  const response = await getFeedbackAttachmentBlob(attachment.attachmentKey, attachment.fileName)
  const blobUrl = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = attachment.fileName || 'attachment'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(blobUrl)
}
