import request from '@/api/request'

export function getCategories() {
  return request.get('/knowledge/categories')
}

export function getArticles(params) {
  return request.get('/knowledge/articles', { params })
}

export function getArticle(id) {
  return request.get(`/knowledge/articles/${id}`)
}

export function createArticle(data) {
  return request.post('/knowledge/articles', data)
}

export function updateArticle(id, data) {
  return request.put(`/knowledge/articles/${id}`, data)
}

export function deleteArticle(id) {
  return request.delete(`/knowledge/articles/${id}`)
}
