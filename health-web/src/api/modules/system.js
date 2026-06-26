import request from '@/api/request'

export const getUsers = (params) => request.get('/admin/users', { params })
export const createUser = (data) => request.post('/admin/users', data)
export const updateUser = (id, data) => request.put(`/admin/users/${id}`, data)
export const updateUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })
export const resetPassword = (id, password) => request.put(`/admin/users/${id}/password`, { password })
export const deleteUser = (id) => request.delete(`/admin/users/${id}`)

export const getDepartments = (params) => request.get('/admin/departments', { params })
export const createDepartment = (data) => request.post('/admin/departments', data)
export const updateDepartment = (id, data) => request.put(`/admin/departments/${id}`, data)
export const deleteDepartment = (id) => request.delete(`/admin/departments/${id}`)

export const getRoles = (params) => request.get('/admin/roles', { params })
export const updateRoleMenus = (id, menus) => request.put(`/admin/roles/${id}/menus`, { menus })

export const getMenuTree = () => request.get('/admin/menus')
