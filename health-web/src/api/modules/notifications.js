import request from '@/api/request'

export function getNotifications(params) {
  return request.get('/v1/system/notice', { params })
}

export function getUnreadCount() {
  return request.get('/v1/system/notice/unread-count')
}

export function markAsRead(id) {
  return request.put(`/v1/system/notice/${id}/read`)
}

export function markAllRead() {
  return request.put('/v1/system/notice/read-all')
}
