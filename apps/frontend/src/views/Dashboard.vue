<template>
  <div>
    <h2>總覽</h2>
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日打卡" :value="todayRecord ? '已打卡' : '未打卡'" />
          <div v-if="todayRecord" style="margin-top: 8px; font-size: 13px; color: #909399;">
            上班: {{ formatTime(todayRecord.clockIn) }}
            <span v-if="todayRecord.clockOut"> | 下班: {{ formatTime(todayRecord.clockOut) }}</span>
            <el-tag :type="statusTagType(todayRecord.status)" size="small" style="margin-left: 8px;">{{ todayRecord.status }}</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="我的假單" :value="myLeaveCount" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="我的加班" :value="myOvertimeCount" />
        </el-card>
      </el-col>
      <el-col :span="6" v-if="isManagerOrAdmin">
        <el-card shadow="hover">
          <el-statistic title="待簽核" :value="pendingCount" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useAttendanceStore } from '../stores/attendance'
import { useLeaveStore } from '../stores/leave'
import { useOvertimeStore } from '../stores/overtime'

const attendanceStore = useAttendanceStore()
const leaveStore = useLeaveStore()
const overtimeStore = useOvertimeStore()

const todayRecord = computed(() => attendanceStore.todayRecord)
const myLeaveCount = computed(() => leaveStore.myLeaves.length)
const myOvertimeCount = computed(() => overtimeStore.myOvertimes.length)
const pendingCount = computed(() => leaveStore.pendingLeaves.length + overtimeStore.pendingOvertimes.length)

const role = localStorage.getItem('userRole') || ''
const isManagerOrAdmin = ['MANAGER', 'ADMIN'].includes(role)

function formatTime(dt: string | null) {
  if (!dt) return '-'
  return new Date(dt).toLocaleTimeString('zh-TW', { hour: '2-digit', minute: '2-digit' })
}

function statusTagType(status: string) {
  const map: Record<string, string> = { NORMAL: 'success', LATE: 'warning', EARLY_LEAVE: 'danger', ABSENT: 'danger' }
  return map[status] || 'info'
}

onMounted(async () => {
  await Promise.all([
    attendanceStore.fetchToday().catch(() => {}),
    leaveStore.fetchMyLeaves().catch(() => {}),
    overtimeStore.fetchMyOvertimes().catch(() => {}),
    ...(isManagerOrAdmin ? [
      leaveStore.fetchPendingLeaves().catch(() => {}),
      overtimeStore.fetchPendingOvertimes().catch(() => {}),
    ] : []),
  ])
})
</script>
