import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as userApi from '../api/users'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  const users = ref<any[]>([])
  const totalPages = ref(0)
  const totalElements = ref(0)
  const currentPage = ref(0)
  const loading = ref(false)

  async function fetchUsers(page = 0, size = 10, search?: string) {
    loading.value = true
    try {
      const { data } = await userApi.getUsers({ page, size, search })
      users.value = data.content
      totalPages.value = data.totalPages
      totalElements.value = data.totalElements
      currentPage.value = data.number
    } finally {
      loading.value = false
    }
  }

  async function createUser(userData: any) {
    await userApi.createUser(userData)
    ElMessage.success('使用者建立成功')
    await fetchUsers(currentPage.value)
  }

  async function updateUser(id: number, userData: any) {
    await userApi.updateUser(id, userData)
    ElMessage.success('使用者更新成功')
    await fetchUsers(currentPage.value)
  }

  async function deactivateUser(id: number) {
    await userApi.deactivateUser(id)
    ElMessage.success('使用者已停用')
    await fetchUsers(currentPage.value)
  }

  return { users, totalPages, totalElements, currentPage, loading, fetchUsers, createUser, updateUser, deactivateUser }
})
