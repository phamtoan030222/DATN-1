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

  // Xếp lịch nâng cao
  assignBulk: (data: any) => {
    return request.post('/api/v1/admin/schedules/bulk', data)
  },

  // 👇 THÊM MỚI: Import Excel lịch làm việc 👇
  importExcel: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)

    // Lưu ý: Đảm bảo URL này khớp với URL Controller ở Backend của bạn
    // Ví dụ: '/api/v1/admin/schedules/import' hoặc '/api/v1/work-schedules/import'
    return request.post('/api/v1/admin/schedules/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  downloadTemplate: () => {
    // responseType: 'blob' rất quan trọng để Axios hiểu đây là file
    return request.get('/api/v1/admin/schedules/template', { responseType: 'blob' })
  },
}
