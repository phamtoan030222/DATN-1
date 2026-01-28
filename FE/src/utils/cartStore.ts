import { ref } from 'vue'
// Đảm bảo đường dẫn import đúng tới file api của bạn
import { GetGioHang } from '@/service/api/client/banhang.api'

const INVOICE_KEY = 'GUEST_INVOICE_ID'

// State reactive để lưu số lượng (Dùng chung cho cả App)
const count = ref(0)

export const CartStore = {
  // Biến count để hiển thị trên Badge
  count,

  getInvoiceId: () => localStorage.getItem(INVOICE_KEY),

  setInvoiceId: (id: string) => {
    localStorage.setItem(INVOICE_KEY, id)
    // Khi có ID mới -> cập nhật luôn số lượng
    CartStore.updateCount()
  },

  clearCart: () => {
    localStorage.removeItem(INVOICE_KEY)
    count.value = 0
  },

  // HÀM QUAN TRỌNG: Gọi API để đếm lại số lượng
  async updateCount() {
    const id = localStorage.getItem(INVOICE_KEY)
    if (!id) {
      count.value = 0
      return
    }
    try {
      const res = await GetGioHang(id)

      // Xử lý dữ liệu trả về (Array hoặc Object)
      let list: any[] = []
      if (Array.isArray(res)) {
        list = res
      }
      else if (res && (res as any).data && Array.isArray((res as any).data)) {
        list = (res as any).data
      }
      else if (res && (res as any).content) {
        list = (res as any).content
      }

      // Tính tổng số lượng (Cộng dồn field 'soLuong' của từng item)
      const total = list.reduce((sum: number, item: any) => sum + (Number(item.soLuong) || 0), 0)
      count.value = total
    }
    catch (e) {
      console.error('Lỗi cập nhật số lượng giỏ hàng:', e)
      count.value = 0
    }
  },
}
