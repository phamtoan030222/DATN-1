import request from '@/service/request' // Kiểm tra lại đường dẫn import request của bạn cho đúng

const PREFIX = '/api/v1/staff/shifts'

export interface EndShiftPayload {
  shiftId: string
  accountId: string
  realCash: number
  note: string
}

export const handoverApi = {
  // 1. Mở ca (Start Shift)
  startShift: (data: any) => request.post(`${PREFIX}/start`, data),

  // 2. Lấy thông tin ca hiện tại của nhân viên
  getCurrentShift: (accountId: string) => {
    return request.get(`${PREFIX}/current`, { params: { accountId } })
  },

  // 3. Kết ca (End Shift)
  endShift: (data: EndShiftPayload) => {
    return request.post(`${PREFIX}/end`, data)
  },

  // 4. [MỚI] Lấy thông tin ca đã đóng gần nhất (để đối chiếu tiền)
  getLastClosedShift: () => {
    return request.get(`${PREFIX}/last-closed`)
  },

  getHistory: (params: any) => {
    return request.get('/api/v1/admin/shift-history', { params })
  },
}
