import request from '@/api/request'

export const getAppointments = (params) => request.get('/v1/appointment', { params })
export const getAppointment = (id) => request.get(`/v1/appointment/${id}`)
export const createAppointment = (data) => request.post('/v1/appointment', data)
export const updateAppointment = (id, data) => request.put(`/v1/appointment/${id}`, data)
export const updateAppointmentStatus = (id, status) => request.put(`/v1/appointment/${id}`, { status })
export const deleteAppointment = (id) => request.delete(`/v1/appointment/${id}`)
