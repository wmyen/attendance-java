<template>
  <div>
    <h2>部門管理</h2>
    <div style="margin: 20px 0;">
      <el-button type="primary" @click="showDialog = true">新增部門</el-button>
    </div>
    <el-table :data="departments" stripe border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="部門名稱" />
      <el-table-column prop="createdAt" label="建立時間" width="180">
        <template #default="{ row }">{{ formatDt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">編輯</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editing ? '編輯部門' : '新增部門'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名稱">
          <el-input v-model="form.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDepartments, createDepartment, updateDepartment } from '../../api/departments'
import { ElMessage } from 'element-plus'

const departments = ref<any[]>([])
const showDialog = ref(false)
const editing = ref<any>(null)

const form = reactive({ name: '' })

async function fetchData() {
  const { data } = await getDepartments()
  departments.value = data
}

function openEdit(row: any) {
  editing.value = row
  form.name = row.name
  showDialog.value = true
}

async function handleSave() {
  if (editing.value) {
    await updateDepartment(editing.value.id, { name: form.name })
    ElMessage.success('更新成功')
  } else {
    await createDepartment({ name: form.name })
    ElMessage.success('建立成功')
  }
  showDialog.value = false
  editing.value = null
  form.name = ''
  await fetchData()
}

function formatDt(dt: string) {
  return dt ? new Date(dt).toLocaleString('zh-TW') : '-'
}

onMounted(fetchData)
</script>
