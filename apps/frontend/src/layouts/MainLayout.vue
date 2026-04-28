<template>
  <el-container style="min-height: 100vh">
    <Sidebar />
    <el-container>
      <el-header style="display: flex; justify-content: space-between; align-items: center; background: #fff; border-bottom: 1px solid #e6e6e6;">
        <span style="font-size: 18px; font-weight: 600;">出缺勤管理系統</span>
        <div style="display: flex; align-items: center; gap: 12px;">
          <span>{{ userName }} ({{ userRole }})</span>
          <el-button type="danger" size="small" @click="handleLogout">登出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Sidebar from '../components/Sidebar.vue'

const router = useRouter()
const authStore = useAuthStore()

const userName = localStorage.getItem('userName') || ''
const userRole = localStorage.getItem('userRole') || ''

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
