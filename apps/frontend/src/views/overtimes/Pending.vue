<template>
  <div>
    <h2>待簽核加班</h2>
    <el-table :data="pending" stripe border style="margin-top: 20px;">
      <el-table-column label="申請人" width="100">
        <template #default="{ row }">{{ row.userName || '-' }}</template>
      </el-table-column>
      <el-table-column label="開始" width="160">
        <template #default="{ row }">{{ formatDt(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="結束" width="160">
        <template #default="{ row }">{{ formatDt(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="事由" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button type="success" size="small" @click="handleApprove(row.id)">核准</el-button>
          <el-button type="danger" size="small" @click="handleReject(row.id)">駁回</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useOvertimeStore } from '../../stores/overtime'

const overtimeStore = useOvertimeStore()
const pending = computed(() => overtimeStore.pendingOvertimes)

function formatDt(dt: string) {
  return new Date(dt).toLocaleString('zh-TW')
}

async function handleApprove(id: number) {
  await overtimeStore.approveOvertime(id)
}

async function handleReject(id: number) {
  await overtimeStore.rejectOvertime(id)
}

onMounted(() => overtimeStore.fetchPendingOvertimes())
</script>
