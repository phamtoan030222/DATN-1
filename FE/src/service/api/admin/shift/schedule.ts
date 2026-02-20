import request from '@/service/request'

export interface WorkSchedule {
  id?: number
  shiftId: number
  staffId: number
  workDate: string // Format: YYYY-MM-DD
  staffName?: string // Để hiển thị cho dễ
}

export const scheduleApi = {
  // Lấy lịch làm việc theo tuần (Gửi lên ngày bắt đầu và kết thúc tuần)
  getWeekly: (fromDate: string, toDate: string) => {
    return request.get<WorkSchedule[]>('/api/v1/admin/schedules', { params: { fromDate, toDate } })
  },

  // Gán nhân viên vào ca
  assign: (data: WorkSchedule) => {
    return request.post<WorkSchedule>('/api/v1/admin/schedules', data)
  },

  // Xóa nhân viên khỏi ca
  delete: (id: number) => {
    return request.delete(`/api/v1/admin/schedules/${id}`)
  },

  // Xếp lịch nâng cao (Đã thêm đúng tiền tố /api/v1)
  assignBulk: (data: any) => {
    return request.post('/api/v1/admin/schedules/bulk', data)
  },
}
