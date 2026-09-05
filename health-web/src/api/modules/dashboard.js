import request from '@/api/request'

export function getStats() {
  return request.get('/v1/dashboard/stats')
}
