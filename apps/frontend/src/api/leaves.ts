import request from './request'

export function applyLeave(data: {
  leaveTypeId: number
  startTime: string
  endTime: string
  reason: string
  agentId?: number
}) {
  return request.post('/leaves', data)
}

export function getMyLeaves() {
  return request.get('/leaves/my')
}

export function getPendingLeaves() {
  return request.get('/leaves/pending')
}

export function approveLeave(id: number) {
  return request.put(`/leaves/${id}/approve`)
}

export function rejectLeave(id: number) {
  return request.put(`/leaves/${id}/reject`)
}

export function getLeaveBalance(params?: { userId?: number }) {
  return request.get('/leaves/balance', { params })
}

export function getLeaveTypes() {
  return request.get('/leaves/types')
}
