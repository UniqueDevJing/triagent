import request from '@/api/request'

export function getPlans(params) {
  return request.get('/v1/intervention/plan', { params })
}

export function createPlan(data) {
  return request.post('/v1/intervention/plan', data)
}

export function updatePlan(id, data) {
  return request.put(`/v1/intervention/plan/${id}`, data)
}

export function getPlanTasks(planId) {
  return request.get('/v1/intervention/task', { params: { planId } })
}

export function createTask(data) {
  return request.post('/v1/intervention/task', data)
}

export function updateTaskStatus(taskId, status) {
  return request.put(`/v1/intervention/task/${taskId}`, { status })
}
