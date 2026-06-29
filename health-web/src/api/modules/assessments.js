import request from '@/api/request'

export function getTemplates() {
  return request.get('/assessments/templates')
}

export function getRecords(params) {
  return request.get('/assessments/records', { params })
}

export function submitAssessment(data) {
  return request.post('/assessments/submit', data)
}
