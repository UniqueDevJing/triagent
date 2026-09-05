import request from '@/api/request'

/**
 * 智能分诊助手 API（透明推理 SSE）。
 * 说明：原生 EventSource 无法携带 Authorization 头，因此用 fetch + ReadableStream
 * 解析 text/event-stream（POST 语义也更贴合聊天接口）。
 */

/**
 * 流式对话。
 * @param {string} sessionId 会话 ID
 * @param {string} message   用户消息
 * @param {Object} handlers  事件回调 { token, tool_call, tool_result, clarify, done, error }
 * @returns {Promise<void>}
 */
export function chatStream(sessionId, message, handlers = {}) {
  const token = localStorage.getItem('token') || ''
  return fetch('/api/v1/assistant/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      Authorization: token,
    },
    body: JSON.stringify({ sessionId, message }),
  }).then(async (res) => {
    if (!res.ok || !res.body) {
      throw new Error('HTTP ' + res.status)
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buf = ''
    let event = ''
    const emit = (name, raw) => {
      if (!raw) return
      let data
      try {
        data = JSON.parse(raw)
      } catch (e) {
        return
      }
      if (typeof handlers[name] === 'function') handlers[name](data)
    }
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buf.indexOf('\n')) >= 0) {
        const line = buf.slice(0, idx).trim()
        buf = buf.slice(idx + 1)
        if (!line) continue
        if (line.startsWith('event:')) {
          event = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          emit(event, line.slice(5).trim())
          event = ''
        }
      }
    }
  })
}

export function getMetrics() {
  return request.get('/v1/assistant/metrics')
}

export function confirmPreOrder(id) {
  return request.post(`/v1/assistant/preorders/${id}/confirm`)
}
