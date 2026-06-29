import request from '@/api/request'

export function getNotifications(params) {
  return request.get('/notifications', { params })
}

export function getUnreadCount(userId) {
  return request.get('/notifications/unread-count', { params: { userId } })
}

export function markAsRead(id) {
  return request.put(`/notifications/${id}/read`)
}

export function markAllRead(userId) {
  return request.put('/notifications/read-all', { params: { userId } })
}

export function subscribeNotifications(userId) {
  return new EventSource(`/api/sse/subscribe/notifications:${userId}`)
}
