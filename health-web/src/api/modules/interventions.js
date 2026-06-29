import request from '@/api/request'

export function getPlans(params) {
  return request.get('/interventions/plans', { params })
}

export function getPlanTasks(planId) {
  return request.get(`/interventions/plans/${planId}/tasks`)
}

export function createPlan(data) {
  return request.post('/interventions/plans', data)
}

export function updatePlan(id, data) {
  return request.put(`/interventions/plans/${id}`, data)
}

export function createTask(data) {
  return request.post('/interventions/tasks', data)
}

export function updateTaskStatus(id, status) {
  return request.put(`/interventions/tasks/${id}/status`, null, { params: { status } })
}
