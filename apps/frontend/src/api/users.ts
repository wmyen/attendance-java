import request from './request'

export function getUsers(params: { page: number; size: number; search?: string }) {
  return request.get('/users', { params })
}

export function getUser(id: number) {
  return request.get(`/users/${id}`)
}

export function createUser(data: {
  email: string
  name: string
  role: string
  deptId?: number
  managerId?: number
  agentId?: number
}) {
  return request.post('/users', data)
}

export function updateUser(id: number, data: {
  name?: string
  role?: string
  deptId?: number
  managerId?: number
  agentId?: number
}) {
  return request.put(`/users/${id}`, data)
}

export function deactivateUser(id: number) {
  return request.delete(`/users/${id}`)
}
