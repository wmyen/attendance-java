<template>
  <div>
    <h2>我的加班</h2>
    <el-table :data="overtimes" stripe border style="margin-top: 20px;">
      <el-table-column label="開始" width="160">
        <template #default="{ row }">{{ formatDt(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="結束" width="160">
        <template #default="{ row }">{{ formatDt(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="事由" />
      <el-table-column prop="status" label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useOvertimeStore } from '../../stores/overtime'

const overtimeStore = useOvertimeStore()
const overtimes = computed(() => overtimeStore.myOvertimes)

function formatDt(dt: string) {
  return new Date(dt).toLocaleString('zh-TW')
}

function statusType(s: string) {
  const map: Record<string, string> = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[s] || 'info'
}

function statusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待簽核', APPROVED: '已核准', REJECTED: '已駁回' }
  return map[s] || s
}

onMounted(() => overtimeStore.fetchMyOvertimes())
</script>
