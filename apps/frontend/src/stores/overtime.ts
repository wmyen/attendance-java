import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as overtimeApi from '../api/overtimes'
import { ElMessage } from 'element-plus'

export const useOvertimeStore = defineStore('overtime', () => {
  const myOvertimes = ref<any[]>([])
  const pendingOvertimes = ref<any[]>([])
  const loading = ref(false)

  async function applyOvertime(data: { startTime: string; endTime: string; reason: string }) {
    loading.value = true
    try {
      await overtimeApi.applyOvertime(data)
      ElMessage.success('加班申請已送出')
    } finally {
      loading.value = false
    }
  }

  async function fetchMyOvertimes() {
    const { data } = await overtimeApi.getMyOvertimes()
    myOvertimes.value = data
  }

  async function fetchPendingOvertimes() {
    const { data } = await overtimeApi.getPendingOvertimes()
    pendingOvertimes.value = data
  }

  async function approveOvertime(id: number) {
    await overtimeApi.approveOvertime(id)
    ElMessage.success('已核准')
    await fetchPendingOvertimes()
  }

  async function rejectOvertime(id: number) {
    await overtimeApi.rejectOvertime(id)
    ElMessage.success('已駁回')
    await fetchPendingOvertimes()
  }

  return { myOvertimes, pendingOvertimes, loading, applyOvertime, fetchMyOvertimes, fetchPendingOvertimes, approveOvertime, rejectOvertime }
})
