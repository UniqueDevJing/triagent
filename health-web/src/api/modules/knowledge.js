import request from '@/api/request'

export function getArticles(params) {
  return request.get('/v1/knowledge/article', { params })
}

export function getArticle(id) {
  return request.get(`/v1/knowledge/article/${id}`)
}

export function createArticle(data) {
  return request.post('/v1/knowledge/article', data)
}

export function updateArticle(id, data) {
  return request.put(`/v1/knowledge/article/${id}`, data)
}

export function deleteArticle(id) {
  return request.delete(`/v1/knowledge/article/${id}`)
}
