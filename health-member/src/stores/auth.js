import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const memberInfo = ref(JSON.parse(localStorage.getItem('memberInfo') || 'null'))

  const isLoggedIn = () => !!token.value

  function setToken(t) { token.value = t; localStorage.setItem('token', t) }
  function setMemberInfo(info) { memberInfo.value = info; localStorage.setItem('memberInfo', JSON.stringify(info)) }

  function logout() {
    token.value = ''
    memberInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('memberInfo')
  }

  return { token, memberInfo, isLoggedIn, setToken, setMemberInfo, logout }
})
