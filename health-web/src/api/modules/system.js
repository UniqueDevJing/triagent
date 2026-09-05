import request from '@/api/request'

export const getUsers = (params) => request.get('/v1/system/user', { params })
export const createUser = (data) => request.post('/v1/system/user', data)
export const updateUser = (id, data) => request.put(`/v1/system/user/${id}`, data)
export const updateUserStatus = (id, status) => request.put(`/v1/system/user/${id}`, { status })
export const resetPassword = (id, password) => request.put(`/v1/system/user/${id}`, { password })
export const deleteUser = (id) => request.delete(`/v1/system/user/${id}`)

export const getDepartments = (params) => request.get('/v1/system/department', { params })
export const createDepartment = (data) => request.post('/v1/system/department', data)
export const updateDepartment = (id, data) => request.put(`/v1/system/department/${id}`, data)
export const deleteDepartment = (id) => request.delete(`/v1/system/department/${id}`)

export const getRoles = (params) => request.get('/v1/system/role', { params })
export const createRole = (data) => request.post('/v1/system/role', data)
export const updateRole = (id, data) => request.put(`/v1/system/role/${id}`, data)
export const deleteRole = (id) => request.delete(`/v1/system/role/${id}`)

export const getMenuTree = () => request.get('/v1/system/menu')

// 角色-菜单授权（后端端点待实现；menuIds 兼容数组或 JSON 字符串）
export const updateRoleMenus = (roleId, menuIds) => {
  const ids = typeof menuIds === 'string' ? JSON.parse(menuIds) : menuIds
  return request.put(`/v1/system/role/${roleId}/menus`, { menuIds: ids })
}
