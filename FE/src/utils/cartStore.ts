import { ref } from 'vue'
import { GetGioHang } from '@/service/api/client/banhang.api'
import { warn } from 'node:console'

const INVOICE_KEY = 'GUEST_INVOICE_ID'

const count = ref(0)

export const CartStore = {
  count,

  getInvoiceId: () => {
    return localStorage.getItem(INVOICE_KEY)
  },

  setInvoiceId: (id: string) => {
    if (!id) {
      localStorage.removeItem(INVOICE_KEY)
      count.value = 0
      return
    }
    localStorage.setItem(INVOICE_KEY, id)
    CartStore.updateCount()
  },

  clearCart: () => {
    localStorage.removeItem(INVOICE_KEY)
    count.value = 0
  },

  // --- HÀM ĐƯỢC SỬA ĐỔI ---
  async updateCount() {
    const id = localStorage.getItem(INVOICE_KEY)

    // Nếu không có ID thì thôi, reset về 0
    if (!id) {
      count.value = 0
      return
    }

    try {
      const res = await GetGioHang(id)

      // Kiểm tra kỹ cấu trúc trả về
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
      else {
        // Trường hợp API trả về 200 OK nhưng body null hoặc rỗng lạ thường
        // -> Coi như giỏ hàng rỗng
        list = []
      }

      const total = list.reduce((sum: number, item: any) => sum + (Number(item.soLuong) || 0), 0)
      count.value = total
    }
    catch (e: any) {
      console.error('Lỗi cập nhật giỏ hàng (ID cũ có thể không còn tồn tại):', e)

      // QUAN TRỌNG: Cơ chế tự sửa lỗi
      // Nếu API trả về 400, 404 hoặc 500, nghĩa là ID hiện tại trong LocalStorage là RÁC
      // Ta cần xóa nó đi để lần sau code tự tạo ID mới.

      localStorage.removeItem(INVOICE_KEY)
      count.value = 0
      console.warn('Đã tự động xóa ID hóa đơn hỏng khỏi LocalStorage.')
    }
  },
}
