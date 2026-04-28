import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<any>(null)
  const role = ref<string>(localStorage.getItem('userRole') || '')

  async function login(email: string, password: string) {
    const { data } = await authApi.login({ email, password })
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('userRole', data.user.role)
    localStorage.setItem('userName', data.user.name)
    localStorage.setItem('userId', String(data.user.id))
    user.value = data.user
    role.value = data.user.role
    return data.user
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    await authApi.changePassword({ oldPassword, newPassword })
  }

  function logout() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userRole')
    localStorage.removeItem('userName')
    localStorage.removeItem('userId')
    user.value = null
    role.value = ''
  }

  function getMustChangePassword() {
    return user.value?.mustChangePassword ?? false
  }

  return { user, role, login, changePassword, logout, getMustChangePassword }
})
