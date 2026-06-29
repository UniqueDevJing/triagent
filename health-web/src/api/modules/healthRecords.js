import request from '@/api/request'

export function getHealthRecords(params) {
  return request.get('/health-records', { params })
}

export function getHealthRecordsByUser(userId) {
  return request.get(`/health-records/user/${userId}`)
}

export function getLatestHealthRecord(userId) {
  return request.get(`/health-records/user/${userId}/latest`)
}

export function createHealthRecord(data) {
  return request.post('/health-records', data)
}

export function updateHealthRecord(id, data) {
  return request.put(`/health-records/${id}`, data)
}

export function deleteHealthRecord(id) {
  return request.delete(`/health-records/${id}`)
}
