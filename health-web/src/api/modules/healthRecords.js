import request from '@/api/request'

export function getHealthRecords(params) {
  return request.get('/v1/member/health-record', { params })
}

export function createHealthRecord(data) {
  return request.post('/v1/member/health-record', data)
}

export function updateHealthRecord(id, data) {
  return request.put(`/v1/member/health-record/${id}`, data)
}

export function deleteHealthRecord(id) {
  return request.delete(`/v1/member/health-record/${id}`)
}
