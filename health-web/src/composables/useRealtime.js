import { ref, onUnmounted } from 'vue'

/**
 * SSE 实时数据订阅 + 轮询兜底
 * @param {string} topic - 订阅主题: dashboard | users | health_records | assessments | interventions | knowledge
 * @param {Function} onEvent - 事件回调 (eventName, data)
 * @param {number} pollInterval - 轮询兜底间隔(ms), 默认 30000
 */
export function useRealtime(topic, onEvent, pollInterval = 30000) {
  const connected = ref(false)
  let eventSource = null
  let pollTimer = null

  function connect() {
    try {
      eventSource = new EventSource(`/api/sse/subscribe/${topic}`)
      eventSource.onopen = () => { connected.value = true }
      eventSource.onmessage = (e) => {
        try {
          const data = JSON.parse(e.data)
          onEvent(e.type === 'message' ? 'data' : e.type, data)
        } catch { /* ignore parse errors */ }
      }
      // 监听所有自定义事件
      const eventNames = {
        dashboard: ['health_record_created', 'health_record_updated', 'health_record_deleted', 'assessment_submitted', 'plan_created', 'task_status_changed'],
        users: ['user_created', 'user_updated', 'user_deleted'],
        health_records: ['health_record_created', 'health_record_updated', 'health_record_deleted'],
        assessments: ['assessment_submitted'],
        interventions: ['plan_created', 'plan_updated', 'task_added', 'task_status_changed'],
        knowledge: ['article_created', 'article_updated', 'article_deleted'],
      }
      const events = eventNames[topic] || []
      events.forEach(name => {
        eventSource.addEventListener(name, (e) => {
          try { onEvent(name, JSON.parse(e.data)) } catch { /* ignore */ }
        })
      })
      eventSource.onerror = () => {
        connected.value = false
        eventSource?.close()
        eventSource = null
        // 切换到轮询兜底
        if (!pollTimer) {
          pollTimer = setInterval(() => onEvent('poll', null), pollInterval)
        }
      }
    } catch {
      // SSE 不支持，直接轮询
      pollTimer = setInterval(() => onEvent('poll', null), pollInterval)
    }
  }

  function disconnect() {
    clearInterval(pollTimer)
    pollTimer = null
    eventSource?.close()
    eventSource = null
    connected.value = false
  }

  onUnmounted(disconnect)

  return { connected, connect, disconnect }
}
