import request from '@/api/request'

export function getStats() {
  return request.get('/dashboard/stats')
}
