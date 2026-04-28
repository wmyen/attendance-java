<template>
  <div>
    <h2>假別餘額</h2>
    <el-table :data="balances" stripe border style="margin-top: 20px;">
      <el-table-column prop="leaveTypeName" label="假別" width="120" />
      <el-table-column prop="year" label="年度" width="80" />
      <el-table-column prop="totalDays" label="總天數" width="100" />
      <el-table-column prop="usedDays" label="已用天數" width="100" />
      <el-table-column label="剩餘天數" width="100">
        <template #default="{ row }">
          <el-tag :type="row.remainingDays > 0 ? 'success' : 'danger'">{{ row.remainingDays }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useLeaveStore } from '../../stores/leave'

const leaveStore = useLeaveStore()
const balances = computed(() => leaveStore.balances)

onMounted(() => leaveStore.fetchBalance())
</script>
