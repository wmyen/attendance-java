<template>
  <div>
    <h2>請假申請</h2>
    <el-card style="max-width: 600px; margin-top: 20px;">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" @submit.prevent="handleSubmit">
        <el-form-item label="假別" prop="leaveTypeId">
          <el-select v-model="form.leaveTypeId" placeholder="請選擇假別" style="width: 100%;">
            <el-option v-for="lt in leaveTypes" :key="lt.id" :label="lt.name" :value="lt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="開始時間" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="選擇開始時間" style="width: 100%;" value-format="YYYY-MM-DDTHH:mm" />
        </el-form-item>
        <el-form-item label="結束時間" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="選擇結束時間" style="width: 100%;" value-format="YYYY-MM-DDTHH:mm" />
        </el-form-item>
        <el-form-item label="事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="請輸入請假事由" />
        </el-form-item>
        <el-form-item label="代理人">
          <el-select v-model="form.agentId" placeholder="選擇代理人（選填）" clearable style="width: 100%;">
            <el-option v-for="u in agents" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">送出申請</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useLeaveStore } from '../../stores/leave'
import type { FormInstance } from 'element-plus'
import request from '../../api/request'

const leaveStore = useLeaveStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const leaveTypes = ref<any[]>([])
const agents = ref<any[]>([])

const form = reactive({
  leaveTypeId: null as number | null,
  startTime: '',
  endTime: '',
  reason: '',
  agentId: null as number | null,
})

const rules = {
  leaveTypeId: [{ required: true, message: '請選擇假別', trigger: 'change' }],
  startTime: [{ required: true, message: '請選擇開始時間', trigger: 'change' }],
  endTime: [{ required: true, message: '請選擇結束時間', trigger: 'change' }],
  reason: [{ required: true, message: '請輸入事由', trigger: 'blur' }],
}

async function handleSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await leaveStore.applyLeave({
      leaveTypeId: form.leaveTypeId!,
      startTime: form.startTime,
      endTime: form.endTime,
      reason: form.reason,
      agentId: form.agentId || undefined,
    })
    formRef.value?.resetFields()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const [, deptRes] = await Promise.all([
    request.get('/departments'),
    request.get('/users', { params: { page: 0, size: 100 } }),
  ])
  agents.value = deptRes.data.content || []
})
</script>
