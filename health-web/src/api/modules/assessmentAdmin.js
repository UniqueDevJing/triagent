import request from '@/api/request'

// 评估指标
export const getIndicators = (params) => request.get('/admin/assessment-indicators', { params })
export const getAllIndicators = () => request.get('/admin/assessment-indicators/all')
export const createIndicator = (data) => request.post('/admin/assessment-indicators', data)
export const updateIndicator = (id, data) => request.put(`/admin/assessment-indicators/${id}`, data)
export const deleteIndicator = (id) => request.delete(`/admin/assessment-indicators/${id}`)

// 中医体质
export const getConstitutions = (params) => request.get('/admin/tcm-constitutions', { params })
export const createConstitution = (data) => request.post('/admin/tcm-constitutions', data)
export const updateConstitution = (id, data) => request.put(`/admin/tcm-constitutions/${id}`, data)
export const deleteConstitution = (id) => request.delete(`/admin/tcm-constitutions/${id}`)

// 心理评测
export const getPsychologyAssessments = (params) => request.get('/admin/psychology-assessments', { params })
export const getPsychologyAssessment = (id) => request.get(`/admin/psychology-assessments/${id}`)
export const createPsychologyAssessment = (data) => request.post('/admin/psychology-assessments', data)
export const updatePsychologyAssessment = (id, data) => request.put(`/admin/psychology-assessments/${id}`, data)
export const deletePsychologyAssessment = (id) => request.delete(`/admin/psychology-assessments/${id}`)

// 评估记录
export const getAssessmentRecords = (params) => request.get('/admin/assessment-records', { params })
export const createAssessmentRecord = (data) => request.post('/admin/assessment-records', data)
