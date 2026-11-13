import request from '@/api/request'

export const getMembers = (params) => request.get('/v1/member', { params })
export const getMember = (id) => request.get(`/v1/member/${id}`)
export const createMember = (data) => request.post('/v1/member', data)
export const updateMember = (id, data) => request.put(`/v1/member/${id}`, data)
export const deleteMember = (id) => request.delete(`/v1/member/${id}`)
export const getExamPlans = (memberId, params) => request.get(`/v1/member/exam-plan`, { params })
export const createExamPlan = (memberId, data) => request.post(`/v1/member/exam-plan`, data)
export const updateExamPlan = (planId, data) => request.put(`/v1/member/exam-plan/${planId}`, data)
