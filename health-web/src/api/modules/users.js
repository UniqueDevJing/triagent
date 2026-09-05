import request from '@/api/request'

export function getUsers(params) {
  return request.get('/v1/system/user', { params })
}

export function createUser(data) {
  return request.post('/v1/system/user', data)
}

export function updateUser(id, data) {
  return request.put(`/v1/system/user/${id}`, data)
}

export function deleteUser(id) {
  return request.delete(`/v1/system/user/${id}`)
}
