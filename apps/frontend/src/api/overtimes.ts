import request from './request'

export function applyOvertime(data: {
  startTime: string
  endTime: string
  reason: string
}) {
  return request.post('/overtimes', data)
}

export function getMyOvertimes() {
  return request.get('/overtimes/my')
}

export function getPendingOvertimes() {
  return request.get('/overtimes/pending')
}

export function approveOvertime(id: number) {
  return request.put(`/overtimes/${id}/approve`)
}

export function rejectOvertime(id: number) {
  return request.put(`/overtimes/${id}/reject`)
}
