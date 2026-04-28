<template>
  <div>
    <h2>使用者管理</h2>
    <div style="margin: 20px 0; display: flex; gap: 12px;">
      <el-input v-model="search" placeholder="搜尋姓名或 Email" clearable style="width: 300px;" @clear="fetchData" @keyup.enter="fetchData" />
      <el-button type="primary" @click="showDialog = true">新增使用者</el-button>
    </div>
    <el-table :data="users" stripe border v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="email" label="Email" width="200" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="role" label="角色" width="100" />
      <el-table-column prop="deptName" label="部門" width="120" />
      <el-table-column prop="managerName" label="主管" width="100" />
      <el-table-column label="狀態" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">{{ row.isActive ? '啟用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">編輯</el-button>
          <el-button size="small" type="danger" :disabled="!row.isActive" @click="handleDeactivate(row.id)">停用</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top: 20px; justify-content: center;" v-model:current-page="page" :page-size="10" :total="totalElements" layout="prev, pager, next" @current-change="fetchData" />

    <el-dialog v-model="showDialog" :title="editingUser ? '編輯使用者' : '新增使用者'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="Email" v-if="!editingUser">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%;">
            <el-option label="員工" value="EMPLOYEE" />
            <el-option label="主管" value="MANAGER" />
            <el-option label="管理者" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="部門">
          <el-select v-model="form.deptId" clearable placeholder="選擇部門" style="width: 100%;">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { getDepartments } from '../../api/departments'

const userStore = useUserStore()
const users = computed(() => userStore.users)
const loading = computed(() => userStore.loading)
const totalElements = computed(() => userStore.totalElements)

const search = ref('')
const page = ref(1)
const showDialog = ref(false)
const editingUser = ref<any>(null)
const departments = ref<any[]>([])

const form = reactive({
  email: '',
  name: '',
  role: 'EMPLOYEE',
  deptId: null as number | null,
})

function fetchData() {
  userStore.fetchUsers(page.value - 1, 10, search.value || undefined)
}

function openEdit(row: any) {
  editingUser.value = row
  form.name = row.name
  form.role = row.role
  form.deptId = row.deptId
  showDialog.value = true
}

async function handleSave() {
  if (editingUser.value) {
    await userStore.updateUser(editingUser.value.id, { name: form.name, role: form.role, deptId: form.deptId || undefined })
  } else {
    await userStore.createUser({ email: form.email, name: form.name, role: form.role, deptId: form.deptId || undefined })
  }
  showDialog.value = false
  editingUser.value = null
}

async function handleDeactivate(id: number) {
  await userStore.deactivateUser(id)
}

onMounted(async () => {
  const { data } = await getDepartments()
  departments.value = data
  fetchData()
})
</script>
