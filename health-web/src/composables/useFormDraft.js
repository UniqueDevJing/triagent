import { ref, watch, onUnmounted } from 'vue'

/**
 * 表单草稿自动保存到 localStorage
 * @param {string} key 草稿唯一标识
 * @param {object} sources 要监听的响应式数据源，如 { form: reactiveObj } 或 { form: refObj, extra: refObj }
 * @param {object} options
 * @param {number} options.debounce 防抖毫秒数，默认 2000
 * @returns {{ hasDraft, restoreDraft, clearDraft }}
 */
export function useFormDraft(key, sources = {}, { debounce = 2000 } = {}) {
  const storageKey = `draft:${key}`
  const hasDraft = ref(false)
  let timer = null

  // 检查是否有草稿
  try {
    const saved = localStorage.getItem(storageKey)
    if (saved) {
      try { hasDraft.value = Object.keys(JSON.parse(saved)).length > 0 } catch {}
    }
  } catch {}

  // 序列化数据源
  function snapshot() {
    const data = {}
    for (const [name, src] of Object.entries(sources)) {
      data[name] = JSON.parse(JSON.stringify(src))
    }
    return data
  }

  // 防抖保存
  function scheduleSave() {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      try {
        localStorage.setItem(storageKey, JSON.stringify(snapshot()))
        hasDraft.value = true
      } catch {}
    }, debounce)
  }

  // 监听所有数据源
  const stopWatchers = []
  for (const [name, src] of Object.entries(sources)) {
    if (src && typeof src === 'object') {
      stopWatchers.push(
        watch(src, () => scheduleSave(), { deep: true })
      )
    }
  }

  onUnmounted(() => {
    if (timer) clearTimeout(timer)
    stopWatchers.forEach(s => s())
  })

  /** 恢复草稿到数据源 */
  function restoreDraft() {
    try {
      const saved = localStorage.getItem(storageKey)
      if (!saved) return false
      const data = JSON.parse(saved)
      for (const [name, src] of Object.entries(sources)) {
        if (data[name] != null && src) {
          if (Array.isArray(data[name])) {
            // 数组：清空后逐个推入（保持响应式）
            src.length = 0
            data[name].forEach(item => src.push(item))
          } else {
            Object.assign(src, data[name])
          }
        }
      }
      return true
    } catch { return false }
  }

  /** 清除草稿 */
  function clearDraft() {
    try {
      localStorage.removeItem(storageKey)
      hasDraft.value = false
    } catch {}
  }

  return { hasDraft, restoreDraft, clearDraft }
}
