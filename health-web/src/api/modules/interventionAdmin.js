import request from '@/api/request'

export const getChronicDiseases = (params) => request.get('/v1/intervention/chronic', { params })
export const createChronicDisease = (data) => request.post('/v1/intervention/chronic', data)
export const updateChronicDisease = (id, data) => request.put(`/v1/intervention/chronic/${id}`, data)
export const deleteChronicDisease = (id) => request.delete(`/v1/intervention/chronic/${id}`)

export const getDietLogs = (params) => request.get('/v1/intervention/diet', { params })
export const createDietLog = (data) => request.post('/v1/intervention/diet', data)
export const deleteDietLog = (id) => request.delete(`/v1/intervention/diet/${id}`)

export const getCrowdPrograms = (params) => request.get('/v1/intervention/crowd', { params })
export const createCrowdProgram = (data) => request.post('/v1/intervention/crowd', data)
export const updateCrowdProgram = (id, data) => request.put(`/v1/intervention/crowd/${id}`, data)
export const deleteCrowdProgram = (id) => request.delete(`/v1/intervention/crowd/${id}`)

export const getInterventionPlans = (params) => request.get('/v1/intervention/plan', { params })
export const createInterventionPlan = (data) => request.post('/v1/intervention/plan', data)
export const updateInterventionPlan = (id, data) => request.put(`/v1/intervention/plan/${id}`, data)
export const deleteInterventionPlan = (id) => request.delete(`/v1/intervention/plan/${id}`)
