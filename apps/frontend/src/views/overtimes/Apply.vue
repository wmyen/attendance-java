<template>
  <div>
    <h2>加班申請</h2>
    <el-card style="max-width: 600px; margin-top: 20px;">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" @submit.prevent="handleSubmit">
        <el-form-item label="開始時間" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="選擇開始時間" style="width: 100%;" value-format="YYYY-MM-DDTHH:mm" />
        </el-form-item>
        <el-form-item label="結束時間" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="選擇結束時間" style="width: 100%;" value-format="YYYY-MM-DDTHH:mm" />
        </el-form-item>
        <el-form-item label="事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="請輸入加班事由" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">送出申請</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useOvertimeStore } from '../../stores/overtime'
import type { FormInstance } from 'element-plus'

const overtimeStore = useOvertimeStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ startTime: '', endTime: '', reason: '' })

const rules = {
  startTime: [{ required: true, message: '請選擇開始時間', trigger: 'change' }],
  endTime: [{ required: true, message: '請選擇結束時間', trigger: 'change' }],
  reason: [{ required: true, message: '請輸入事由', trigger: 'blur' }],
}

async function handleSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await overtimeStore.applyOvertime({ startTime: form.startTime, endTime: form.endTime, reason: form.reason })
    formRef.value?.resetFields()
  } finally {
    loading.value = false
  }
}
</script>
