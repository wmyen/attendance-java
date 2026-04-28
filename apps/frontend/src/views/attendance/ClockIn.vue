<template>
  <div>
    <h2>打卡</h2>
    <el-card style="margin-top: 20px; text-align: center; padding: 40px;">
      <div style="font-size: 48px; font-weight: bold; margin-bottom: 20px;">
        {{ currentTime }}
      </div>
      <div style="margin-bottom: 30px;">
        <el-tag v-if="todayRecord" :type="statusTagType(todayRecord.status)" size="large">
          {{ todayRecord.status }}
        </el-tag>
      </div>
      <div style="display: flex; justify-content: center; gap: 20px;">
        <el-button type="primary" size="large" :disabled="!!todayRecord?.clockIn" :loading="loading" @click="handleClockIn">
          上班打卡
        </el-button>
        <el-button type="success" size="large" :disabled="!todayRecord?.clockIn || !!todayRecord?.clockOut" :loading="loading" @click="handleClockOut">
          下班打卡
        </el-button>
      </div>
      <div v-if="todayRecord" style="margin-top: 30px; color: #606266;">
        <p>上班時間: {{ formatTime(todayRecord.clockIn) }}</p>
        <p>下班時間: {{ formatTime(todayRecord.clockOut) }}</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAttendanceStore } from '../../stores/attendance'

const attendanceStore = useAttendanceStore()
const loading = computed(() => attendanceStore.loading)
const todayRecord = computed(() => attendanceStore.todayRecord)

const currentTime = ref('')
let timer: number | undefined

function updateTime() {
  currentTime.value = new Date().toLocaleTimeString('zh-TW', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function formatTime(dt: string | null) {
  if (!dt) return '-'
  return new Date(dt).toLocaleTimeString('zh-TW', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function statusTagType(status: string) {
  const map: Record<string, string> = { NORMAL: 'success', LATE: 'warning', EARLY_LEAVE: 'danger', ABSENT: 'danger' }
  return map[status] || 'info'
}

async function handleClockIn() {
  await attendanceStore.clockIn()
}

async function handleClockOut() {
  await attendanceStore.clockOut()
}

onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
  attendanceStore.fetchToday().catch(() => {})
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
