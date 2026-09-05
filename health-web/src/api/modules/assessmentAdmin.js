import request from '@/api/request'

export const getIndicators = (params) => request.get('/v1/assessment/indicator', { params })
export const getAllIndicators = () => request.get('/v1/assessment/indicator')
export const createIndicator = (data) => request.post('/v1/assessment/indicator', data)
export const updateIndicator = (id, data) => request.put(`/v1/assessment/indicator/${id}`, data)
export const deleteIndicator = (id) => request.delete(`/v1/assessment/indicator/${id}`)

export const getConstitutions = (params) => request.get('/v1/assessment/tcm', { params })
export const createConstitution = (data) => request.post('/v1/assessment/tcm', data)
export const updateConstitution = (id, data) => request.put(`/v1/assessment/tcm/${id}`, data)
export const deleteConstitution = (id) => request.delete(`/v1/assessment/tcm/${id}`)

export const getPsychologyAssessments = (params) => request.get('/v1/assessment/psychology', { params })
export const getPsychologyAssessment = (id) => request.get(`/v1/assessment/psychology/${id}`)
export const createPsychologyAssessment = (data) => request.post('/v1/assessment/psychology', data)
export const updatePsychologyAssessment = (id, data) => request.put(`/v1/assessment/psychology/${id}`, data)
export const deletePsychologyAssessment = (id) => request.delete(`/v1/assessment/psychology/${id}`)

export const getAssessmentRecords = (params) => request.get('/v1/assessment/record', { params })
export const createAssessmentRecord = (data) => request.post('/v1/assessment/record', data)
export const deleteAssessmentRecord = (id) => request.delete(`/v1/assessment/record/${id}`)
