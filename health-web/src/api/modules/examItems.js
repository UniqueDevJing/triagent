import request from '@/api/request'

export const getItems = (params) => request.get('/v1/appointment/exam-item', { params })
export const createItem = (data) => request.post('/v1/appointment/exam-item', data)
export const updateItem = (id, data) => request.put(`/v1/appointment/exam-item/${id}`, data)
export const deleteItem = (id) => request.delete(`/v1/appointment/exam-item/${id}`)

// ===== 体检项目组/分类（exam_item_group；后端 Controller 尚未实现，页面先接线） =====
export const getCategories = (params) => request.get('/v1/appointment/exam-item-group', { params })
export const createCategory = (data) => request.post('/v1/appointment/exam-item-group', data)
export const updateCategory = (id, data) => request.put(`/v1/appointment/exam-item-group/${id}`, data)
export const deleteCategory = (id) => request.delete(`/v1/appointment/exam-item-group/${id}`)
