import axios from 'axios'

// 后端走 /v2 上下文；dev 下 vite 代理到 127.0.0.1:18092
const http = axios.create({ baseURL: '/v2', timeout: 30000 })

function ok(res) {
  const body = res.data
  if (body && body.code === 200) return body.data
  throw new Error((body && body.msg) || '请求失败')
}

export const api = {
  get: (url, params) => http.get(url, { params }).then(ok),
  post: (url, params, data) => http.post(url, data, { params }).then(ok),
  sse: (url, params) => {
    const qs = new URLSearchParams(params).toString()
    return `${http.defaults.baseURL}${url}?${qs}`
  }
}

export default api
