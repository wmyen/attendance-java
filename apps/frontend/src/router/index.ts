import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('../views/ChangePassword.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
      },
      // 員工
      {
        path: 'attendance/clock-in',
        name: 'ClockIn',
        component: () => import('../views/attendance/ClockIn.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER', 'ADMIN'] },
      },
      {
        path: 'attendance/monthly',
        name: 'MonthlyAttendance',
        component: () => import('../views/attendance/Monthly.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER', 'ADMIN'] },
      },
      {
        path: 'leaves/apply',
        name: 'LeaveApply',
        component: () => import('../views/leaves/Apply.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER', 'ADMIN'] },
      },
      {
        path: 'leaves/my',
        name: 'MyLeaves',
        component: () => import('../views/leaves/My.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER', 'ADMIN'] },
      },
      {
        path: 'leaves/balance',
        name: 'LeaveBalance',
        component: () => import('../views/leaves/Balance.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER', 'ADMIN'] },
      },
      {
        path: 'overtimes/apply',
        name: 'OvertimeApply',
        component: () => import('../views/overtimes/Apply.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER', 'ADMIN'] },
      },
      {
        path: 'overtimes/my',
        name: 'MyOvertimes',
        component: () => import('../views/overtimes/My.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER', 'ADMIN'] },
      },
      // 主管
      {
        path: 'leaves/pending',
        name: 'PendingLeaves',
        component: () => import('../views/leaves/Pending.vue'),
        meta: { roles: ['MANAGER', 'ADMIN'] },
      },
      {
        path: 'overtimes/pending',
        name: 'PendingOvertimes',
        component: () => import('../views/overtimes/Pending.vue'),
        meta: { roles: ['MANAGER', 'ADMIN'] },
      },
      // 管理者
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('../views/admin/Users.vue'),
        meta: { roles: ['ADMIN'] },
      },
      {
        path: 'admin/departments',
        name: 'AdminDepartments',
        component: () => import('../views/admin/Departments.vue'),
        meta: { roles: ['ADMIN'] },
      },
      {
        path: 'admin/attendance',
        name: 'AdminAttendance',
        component: () => import('../views/admin/Attendance.vue'),
        meta: { roles: ['ADMIN'] },
      },
      {
        path: 'admin/leave-balances',
        name: 'AdminLeaveBalances',
        component: () => import('../views/admin/LeaveBalances.vue'),
        meta: { roles: ['ADMIN'] },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('accessToken')
  const role = localStorage.getItem('userRole')

  if (to.meta.public) {
    return token && to.name === 'Login' ? next('/dashboard') : next()
  }

  if (!token) {
    return next('/login')
  }

  if (to.meta.roles && role && !(to.meta.roles as string[]).includes(role)) {
    return next('/dashboard')
  }

  next()
})

export default router
