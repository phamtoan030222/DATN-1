import { reactive } from 'vue'

// [QUAN TRỌNG] Key mới để lưu danh sách sản phẩm, không lưu ID hóa đơn nữa
const CART_KEY = 'MY_SHOP_CART_ITEMS'

// Định nghĩa cấu trúc 1 sản phẩm trong giỏ
export interface CartItem {
  productDetailId: string
  name: string
  image: string
  price: number // Giá bán hiện tại
  originalPrice: number // Giá gốc
  quantity: number
  // Các thuộc tính hiển thị (RAM, Màu...)
  ram?: string
  hardDrive?: string
  color?: string
  cpu?: string
  gpu?: string
  screen?: string
  brand?: string
  battery?: string
  operatingSystem?: string
}

// State reactive để Vue tự động cập nhật giao diện
const state = reactive({
  items: [] as CartItem[],
})

// Hàm load dữ liệu từ LocalStorage khi mới vào web
function loadFromStorage() {
  const json = localStorage.getItem(CART_KEY)
  if (json) {
    try {
      state.items = JSON.parse(json)
    }
    catch (e) {
      console.error('Lỗi parse giỏ hàng', e)
      state.items = []
    }
  }
}

// Chạy ngay lập tức
loadFromStorage()

export const CartStore = {
  // Lấy danh sách sản phẩm
  getCartItems() {
    return state.items
  },

  // Lưu xuống LocalStorage
  save() {
    localStorage.setItem(CART_KEY, JSON.stringify(state.items))
    // Bắn event để Header cập nhật số lượng (nếu bạn có icon giỏ hàng trên menu)
    window.dispatchEvent(new Event('cart-updated'))
  },

  // 1. Thêm vào giỏ
  addToCart(newItem: CartItem) {
    // Kiểm tra xem sản phẩm đã có trong giỏ chưa
    const existingItem = state.items.find(i => i.productDetailId === newItem.productDetailId)

    if (existingItem) {
      // Nếu có rồi -> Cộng dồn số lượng
      existingItem.quantity += newItem.quantity
    }
    else {
      // Nếu chưa -> Thêm mới
      state.items.push(newItem)
    }
    this.save()
  },

  // 2. Cập nhật số lượng (+ hoặc -)
  updateQuantity(productDetailId: string, delta: number) {
    const item = state.items.find(i => i.productDetailId === productDetailId)
    if (item) {
      const newQty = item.quantity + delta
      if (newQty >= 1) {
        item.quantity = newQty
        this.save()
      }
    }
  },

  // 3. Xóa sản phẩm
  removeFromCart(productDetailId: string) {
    const index = state.items.findIndex(i => i.productDetailId === productDetailId)
    if (index !== -1) {
      state.items.splice(index, 1)
      this.save()
    }
  },

  // 4. Xóa sạch giỏ hàng (Sau khi thanh toán xong)
  clearCart() {
    state.items = []
    localStorage.removeItem(CART_KEY)
    window.dispatchEvent(new Event('cart-updated'))
  },

  // 5. Đếm tổng số lượng (Để hiển thị Badge số trên Icon giỏ hàng)
  getTotalQuantity() {
    return state.items.reduce((total, item) => total + item.quantity, 0)
  },
}
