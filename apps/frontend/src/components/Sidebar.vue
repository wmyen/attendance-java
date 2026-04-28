<template>
  <el-aside width="220px" style="background: #304156;">
    <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold;">
      考勤系統
    </div>
    <el-menu
      :default-active="activeMenu"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      router
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataBoard /></el-icon>
        <span>總覽</span>
      </el-menu-item>

      <el-sub-menu index="attendance" v-if="showEmployee">
        <template #title><el-icon><Clock /></el-icon><span>出缺勤</span></template>
        <el-menu-item index="/attendance/clock-in">打卡</el-menu-item>
        <el-menu-item index="/attendance/monthly">月度紀錄</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="leaves" v-if="showEmployee">
        <template #title><el-icon><Calendar /></el-icon><span>請假</span></template>
        <el-menu-item index="/leaves/apply">請假申請</el-menu-item>
        <el-menu-item index="/leaves/my">我的假單</el-menu-item>
        <el-menu-item index="/leaves/balance">假別餘額</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="overtimes" v-if="showEmployee">
        <template #title><el-icon><Timer /></el-icon><span>加班</span></template>
        <el-menu-item index="/overtimes/apply">加班申請</el-menu-item>
        <el-menu-item index="/overtimes/my">我的加班</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="approval" v-if="showManager">
        <template #title><el-icon><Stamp /></el-icon><span>簽核</span></template>
        <el-menu-item index="/leaves/pending">待簽核假單</el-menu-item>
        <el-menu-item index="/overtimes/pending">待簽核加班</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="admin" v-if="showAdmin">
        <template #title><el-icon><Setting /></el-icon><span>系統管理</span></template>
        <el-menu-item index="/admin/users">使用者管理</el-menu-item>
        <el-menu-item index="/admin/departments">部門管理</el-menu-item>
        <el-menu-item index="/admin/attendance">出缺勤管理</el-menu-item>
        <el-menu-item index="/admin/leave-balances">假別餘額管理</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
const role = localStorage.getItem('userRole') || ''

const showEmployee = ['EMPLOYEE', 'MANAGER', 'ADMIN'].includes(role)
const showManager = ['MANAGER', 'ADMIN'].includes(role)
const showAdmin = role === 'ADMIN'
</script>
