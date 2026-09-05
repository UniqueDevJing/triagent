import request from '@/api/request'

export function getRecords(params) {
  return request.get('/v1/assessment/record', { params })
}

export function submitAssessment(data) {
  return request.post('/v1/assessment/record', data)
}

export function getTemplates(params) {
  return request.get('/v1/assessment/indicator', { params })
}
