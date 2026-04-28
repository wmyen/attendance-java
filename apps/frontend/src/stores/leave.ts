import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as leaveApi from '../api/leaves'
import { ElMessage } from 'element-plus'

export const useLeaveStore = defineStore('leave', () => {
  const myLeaves = ref<any[]>([])
  const pendingLeaves = ref<any[]>([])
  const balances = ref<any[]>([])
  const loading = ref(false)

  async function applyLeave(data: { leaveTypeId: number; startTime: string; endTime: string; reason: string; agentId?: number }) {
    loading.value = true
    try {
      await leaveApi.applyLeave(data)
      ElMessage.success('請假申請已送出')
    } finally {
      loading.value = false
    }
  }

  async function fetchMyLeaves() {
    const { data } = await leaveApi.getMyLeaves()
    myLeaves.value = data
  }

  async function fetchPendingLeaves() {
    const { data } = await leaveApi.getPendingLeaves()
    pendingLeaves.value = data
  }

  async function approveLeave(id: number) {
    await leaveApi.approveLeave(id)
    ElMessage.success('已核准')
    await fetchPendingLeaves()
  }

  async function rejectLeave(id: number) {
    await leaveApi.rejectLeave(id)
    ElMessage.success('已駁回')
    await fetchPendingLeaves()
  }

  async function fetchBalance(userId?: number) {
    const params = userId ? { userId } : undefined
    const { data } = await leaveApi.getLeaveBalance(params)
    balances.value = data
  }

  return { myLeaves, pendingLeaves, balances, loading, applyLeave, fetchMyLeaves, fetchPendingLeaves, approveLeave, rejectLeave, fetchBalance }
})
