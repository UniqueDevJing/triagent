import request from '@/api/request'

export const getPackages = (params) => request.get('/v1/appointment/package', { params })
export const getPackage = (id) => request.get(`/v1/appointment/package/${id}`)
export const createPackage = (data) => request.post('/v1/appointment/package', data)
export const updatePackage = (id, data) => request.put(`/v1/appointment/package/${id}`, data)
export const deletePackage = (id) => request.delete(`/v1/appointment/package/${id}`)
export const getPackageItems = (id) => request.get(`/v1/appointment/package/${id}/items`)
