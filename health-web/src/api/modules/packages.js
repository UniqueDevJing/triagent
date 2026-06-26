import request from '@/api/request'

export const getPackages = (params) => request.get('/admin/packages', { params })
export const getPackage = (id) => request.get(`/admin/packages/${id}`)
export const createPackage = (data) => request.post('/admin/packages', data)
export const updatePackage = (id, data) => request.put(`/admin/packages/${id}`, data)
export const deletePackage = (id) => request.delete(`/admin/packages/${id}`)
export const getPackageItems = (id) => request.get(`/admin/packages/${id}/items`)
