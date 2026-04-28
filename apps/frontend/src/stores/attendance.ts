import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as attendanceApi from '../api/attendance'
import { ElMessage } from 'element-plus'

export const useAttendanceStore = defineStore('attendance', () => {
  const todayRecord = ref<any>(null)
  const monthlyRecords = ref<any[]>([])
  const loading = ref(false)

  async function clockIn() {
    loading.value = true
    try {
      const { data } = await attendanceApi.clockIn()
      todayRecord.value = data
      ElMessage.success('上班打卡成功')
    } finally {
      loading.value = false
    }
  }

  async function clockOut() {
    loading.value = true
    try {
      const { data } = await attendanceApi.clockOut()
      todayRecord.value = data
      ElMessage.success('下班打卡成功')
    } finally {
      loading.value = false
    }
  }

  async function fetchToday() {
    const { data } = await attendanceApi.getToday()
    todayRecord.value = data || null
  }

  async function fetchMonthly(year: number, month: number, userId?: number) {
    const { data } = await attendanceApi.getMonthly({ year, month, userId })
    monthlyRecords.value = data.records || []
  }

  return { todayRecord, monthlyRecords, loading, clockIn, clockOut, fetchToday, fetchMonthly }
})
