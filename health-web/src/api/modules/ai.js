import request from '@/api/request'

export function chat(data) {
  return request.post('/ai/chat', data)
}

export function healthAnalysis(data, userId) {
  return request.post('/ai/health-analysis', { data, userId })
}

export function medicationReminder(medicationInfo, userId) {
  return request.post('/ai/medication-reminder', { medicationInfo, userId })
}

export function companion(message, userId) {
  return request.post('/ai/companion', { message, userId })
}

export function behaviorDetect(behaviorDesc, userId) {
  return request.post('/ai/behavior-detect', { behaviorDesc, userId })
}
