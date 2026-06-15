export function formatDate(date, fallback = '-') {
  if (!date) return fallback
  const d = new Date(date)
  if (isNaN(d.getTime())) return fallback
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

export function formatDateTime(date, fallback = '-') {
  if (!date) return fallback
  const d = new Date(date)
  if (isNaN(d.getTime())) return fallback
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function riskLevelLabel(level) {
  const map = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }
  return map[level] || level || '-'
}

export function riskLevelType(level) {
  const map = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }
  return map[level] || 'info'
}

export function calcBMI(height, weight) {
  if (!height || !weight) return null
  const bmi = (weight / ((height / 100) ** 2)).toFixed(1)
  const label = bmi < 18.5 ? '偏瘦' : bmi < 24 ? '正常' : bmi < 28 ? '偏胖' : '肥胖'
  return { value: bmi, label }
}

export function genderLabel(val) {
  if (val === 1) return '男'
  if (val === 2) return '女'
  return '未知'
}
