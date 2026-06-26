import request from '@/api/request'

export const getMembers = (params) => request.get('/admin/members', { params })
export const getMember = (id) => request.get(`/admin/members/${id}`)
export const createMember = (data) => request.post('/admin/members', data)
export const updateMember = (id, data) => request.put(`/admin/members/${id}`, data)
export const deleteMember = (id) => request.delete(`/admin/members/${id}`)
export const getExamPlans = (memberId, params) => request.get(`/admin/members/${memberId}/exam-plans`, { params })
export const createExamPlan = (memberId, data) => request.post(`/admin/members/${memberId}/exam-plans`, data)
export const updateExamPlan = (planId, data) => request.put(`/admin/members/exam-plans/${planId}`, data)
