import request from '@/api/request'

// Auth
export const login = (phone, code) => request.post('/member/auth/login', { phone, code })
export const sendSmsCode = (phone) => request.post('/member/auth/send-code', { phone })
export const getMemberProfile = () => request.get('/member/profile')

// 预约
export const getMyAppointments = (params) => request.get('/member/appointments', { params })
export const createAppointment = (data) => request.post('/member/appointments', data)
export const cancelAppointment = (id) => request.put(`/member/appointments/${id}/cancel`)

// 健康档案
export const getMemberInfo = () => request.get('/member/info')
export const updateMemberInfo = (data) => request.put('/member/info', data)
export const getHealthRecords = (params) => request.get('/member/health-records', { params })

// 评估
export const getMyAssessments = (params) => request.get('/member/assessments', { params })
export const getAssessmentDetail = (id) => request.get(`/member/assessments/${id}`)

// 干预
export const getMyInterventions = (params) => request.get('/member/interventions', { params })
export const getInterventionDetail = (id) => request.get(`/member/interventions/${id}`)

// 膳食日志
export const getDietLogs = (params) => request.get('/member/diet-logs', { params })
export const createDietLog = (data) => request.post('/member/diet-logs', data)

// 知识库
export const getKnowledgeArticles = (params) => request.get('/member/knowledge/articles', { params })
export const getRecipes = (params) => request.get('/member/knowledge/recipes', { params })
export const getExercises = (params) => request.get('/member/knowledge/exercises', { params })
