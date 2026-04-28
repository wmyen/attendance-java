import request from './request'

export function clockIn() {
  return request.post('/attendance/clock-in')
}

export function clockOut() {
  return request.post('/attendance/clock-out')
}

export function getToday() {
  return request.get('/attendance/today')
}

export function getMonthly(params: { year: number; month: number; userId?: number }) {
  return request.get('/attendance/monthly', { params })
}
