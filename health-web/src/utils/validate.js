export const phoneRegex = /^1[3-9]\d{9}$/

export const isPhone = (val) => phoneRegex.test(val)

export const isEmail = (val) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)

export const isIdCard = (val) => /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(val)

export const phoneRules = [
  { required: true, message: '请输入手机号', trigger: 'blur' },
  { pattern: phoneRegex, message: '手机号格式不正确', trigger: 'blur' },
]

export const emailRules = [
  { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
]

export const requiredRule = (label) => [
  { required: true, message: `请输入${label}`, trigger: 'blur' },
]
