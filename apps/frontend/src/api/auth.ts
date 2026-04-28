import request from './request'

export function login(data: { email: string; password: string }) {
  return request.post('/auth/login', data)
}

export function refreshToken(refreshToken: string) {
  return request.post('/auth/refresh', { refreshToken })
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.post('/auth/change-password', data)
}
