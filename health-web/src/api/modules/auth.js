import request from '@/api/request'

export function getCaptchaImage() {
  return request.get('/v1/captchaImage')
}

export function login(data) {
  return request.post('/v1/login', data)
}

export function logout() {
  return request.post('/v1/logout')
}
