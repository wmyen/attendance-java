<template>
  <div>
    <h2>月度出缺勤</h2>
    <div style="margin: 20px 0; display: flex; gap: 12px; align-items: center;">
      <el-date-picker v-model="selectedMonth" type="month" placeholder="選擇月份" format="YYYY/MM" value-format="YYYY-MM" @change="fetchData" />
    </div>
    <el-table :data="records" stripe border>
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column label="上班" width="100">
        <template #default="{ row }">{{ formatTime(row.clockIn) }}</template>
      </el-table-column>
      <el-table-column label="下班" width="100">
        <template #default="{ row }">{{ formatTime(row.clockOut) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="狀態" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAttendanceStore } from '../../stores/attendance'

const attendanceStore = useAttendanceStore()
const records = computed(() => attendanceStore.monthlyRecords)
const selectedMonth = ref('')

function formatTime(dt: string | null) {
  if (!dt) return '-'
  return new Date(dt).toLocaleTimeString('zh-TW', { hour: '2-digit', minute: '2-digit' })
}

function statusTagType(status: string) {
  const map: Record<string, string> = { NORMAL: 'success', LATE: 'warning', EARLY_LEAVE: 'danger', ABSENT: 'danger' }
  return map[status] || 'info'
}

async function fetchData() {
  const [year, month] = (selectedMonth.value || new Date().toISOString().slice(0, 7)).split('-')
  await attendanceStore.fetchMonthly(Number(year), Number(month))
}

onMounted(() => {
  const now = new Date()
  selectedMonth.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  fetchData()
})
</script>
