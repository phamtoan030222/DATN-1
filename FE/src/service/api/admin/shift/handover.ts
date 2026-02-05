import request from '@/service/request'

const PREFIX = '/api/v1/staff/shifts'

// Định nghĩa kiểu dữ liệu payload để gợi ý code tốt hơn
export interface EndShiftPayload {
  shiftId: string
  accountId: string
  realCash: number
  note: string
}

export const handoverApi = {
  // 👇 SỬA DÒNG NÀY 👇
  // Cũ (SAI): request.post('/api/v1/shifts', data) -> Gọi nhầm sang API Quản lý
  // Mới (ĐÚNG): Gọi vào API Nhân viên (ShiftHandoverController)
  startShift: (data: any) => request.post(`${PREFIX}/start`, data),

  // 1. Lấy thông tin ca hiện tại
  // Backend: @GetMapping("/current")
  getCurrentShift: (accountId: string) => {
    return request.get(`${PREFIX}/current`, { params: { accountId } })
  },

  // 2. Gửi xác nhận kết ca
  // Backend: @PostMapping("/end")
  endShift: (data: EndShiftPayload) => {
    return request.post(`${PREFIX}/end`, data)
  },
}
