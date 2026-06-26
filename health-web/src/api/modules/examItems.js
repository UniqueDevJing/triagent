import request from '@/api/request'

export const getCategories = (params) => request.get('/admin/exam-categories', { params })
export const createCategory = (data) => request.post('/admin/exam-categories', data)
export const updateCategory = (id, data) => request.put(`/admin/exam-categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/admin/exam-categories/${id}`)

export const getItems = (params) => request.get('/admin/exam-items', { params })
export const createItem = (data) => request.post('/admin/exam-items', data)
export const updateItem = (id, data) => request.put(`/admin/exam-items/${id}`, data)
export const deleteItem = (id) => request.delete(`/admin/exam-items/${id}`)
