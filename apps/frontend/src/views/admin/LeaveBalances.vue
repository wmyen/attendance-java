<template>
  <div>
    <h2>假別餘額管理</h2>
    <div style="margin: 20px 0; display: flex; gap: 12px; align-items: center;">
      <el-select v-model="selectedUserId" placeholder="選擇員工" clearable @change="fetchData" style="width: 250px;">
        <el-option v-for="u in userList" :key="u.id" :label="`${u.name} (${u.email})`" :value="u.id" />
      </el-select>
    </div>
    <el-table :data="balances" stripe border v-if="selectedUserId">
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
    <el-empty v-else description="請選擇員工查看餘額" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLeaveBalance } from '../../api/leaves'
import { getUsers } from '../../api/users'

const selectedUserId = ref<number | undefined>(undefined)
const balances = ref<any[]>([])
const userList = ref<any[]>([])

async function fetchData() {
  if (!selectedUserId.value) {
    balances.value = []
    return
  }
  const { data } = await getLeaveBalance({ userId: selectedUserId.value })
  balances.value = data
}

onMounted(async () => {
  const { data } = await getUsers({ page: 0, size: 100 })
  userList.value = data.content || []
})
</script>
