import request from '@/service/request'

export interface Shift {
  id?: number
  code: string
  name: string
  startTime: string
  endTime: string
  status: number // 1: Active, 0: Inactive
}

export const shiftApi = {
  getAll: () => request.get<Shift[]>('/api/v1/shifts'),
  create: (data: Shift) => request.post<Shift>('/api/v1/shifts', data),
  update: (id: number, data: Shift) => request.put<Shift>(`/api/v1/shifts/${id}`, data),
  delete: (id: number) => request.delete(`/api/v1/shifts/${id}`),
}
