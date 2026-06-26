import request from '@/api/request'

export const getChronicDiseases = (params) => request.get('/admin/chronic-diseases', { params })
export const createChronicDisease = (data) => request.post('/admin/chronic-diseases', data)
export const updateChronicDisease = (id, data) => request.put(`/admin/chronic-diseases/${id}`, data)
export const deleteChronicDisease = (id) => request.delete(`/admin/chronic-diseases/${id}`)

export const getDietLogs = (params) => request.get('/admin/diet-logs', { params })
export const createDietLog = (data) => request.post('/admin/diet-logs', data)
export const deleteDietLog = (id) => request.delete(`/admin/diet-logs/${id}`)

export const getCrowdPrograms = (params) => request.get('/admin/crowd-programs', { params })
export const createCrowdProgram = (data) => request.post('/admin/crowd-programs', data)
export const updateCrowdProgram = (id, data) => request.put(`/admin/crowd-programs/${id}`, data)
export const deleteCrowdProgram = (id) => request.delete(`/admin/crowd-programs/${id}`)
