import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

describe('AuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('should have empty token by default', () => {
    const store = useAuthStore()
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
  })

  it('should read token from localStorage', () => {
    localStorage.setItem('token', 'stored-token')
    localStorage.setItem('user', JSON.stringify({ username: 'admin', role: 'ADMIN', name: '管理员' }))
    const store = useAuthStore()
    expect(store.token).toBe('stored-token')
    expect(store.isLoggedIn).toBe(true)
    expect(store.userName).toBe('管理员')
    expect(store.role).toBe('ADMIN')
  })

  it('should clear state on logout', () => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('user', JSON.stringify({ username: 'admin' }))
    const store = useAuthStore()
    store.logout()
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
    expect(store.userName).toBe('')
  })

  it('should return empty role when user is null', () => {
    const store = useAuthStore()
    expect(store.role).toBe('')
  })
})
