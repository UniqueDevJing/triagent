import request from '@/api/request'

export const getAppointments = (params) => request.get('/admin/appointments', { params })
export const getAppointment = (id) => request.get(`/admin/appointments/${id}`)
export const createAppointment = (data) => request.post('/admin/appointments', data)
export const updateAppointment = (id, data) => request.put(`/admin/appointments/${id}`, data)
export const updateAppointmentStatus = (id, status) => request.put(`/admin/appointments/${id}/status`, { status })
export const deleteAppointment = (id) => request.delete(`/admin/appointments/${id}`)
