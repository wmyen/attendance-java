import request from './request'

export function getDepartments() {
  return request.get('/departments')
}

export function createDepartment(data: { name: string }) {
  return request.post('/departments', data)
}

export function updateDepartment(id: number, data: { name: string }) {
  return request.put(`/departments/${id}`, data)
}
