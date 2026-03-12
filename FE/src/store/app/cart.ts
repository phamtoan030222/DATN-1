import { CUSTOMER_CART_ID, CUSTOMER_CART_ITEM } from '@/constants/storageKey'
import { getCartByCustomer } from '@/service/api/client/customer/customer.api'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from '../auth'
import { localStorageAction } from '@/utils/storage.helper'
import { GetGioHang, getProductDetailCart, themSanPham } from '@/service/api/client/banhang.api'

export interface CardItem {
  id: string
  quantity: number
  price: number
  percentage: number
  imageUrl: string
  cpu: string
  ram: string
  hardDrive: string
  gpu: string
  color: string
  material: string
  productDetailId: string
  name: string
}

export const useCartStore = defineStore('cart', () => {
  const cartId = ref<string | null | undefined>(undefined)
  const cartItems = ref<CardItem[]>([])

  const cartItemBuyNow = ref<CardItem | null>(null)

  const { userInfoDatn } = useAuthStore()

  const fetchCartId = async () => {
    try {
      if (userInfoDatn?.rolesCodes?.includes('KHACH_HANG')) {
        const res = await getCartByCustomer(userInfoDatn.userId as string)
        cartId.value = res.data || null
        if (cartId.value) {
          localStorageAction.set(CUSTOMER_CART_ID, cartId.value)
        }
      }
      else {
        localStorageAction.remove(CUSTOMER_CART_ID)
      }
    }
    catch (error) {
      console.error('Lỗi khi lấy thông tin giỏ hàng:', error)
    }
  }

  // --- HÀM HELPER LỌC TRÙNG LẶP SẢN PHẨM ---
  const mergeDuplicateCartItems = (rawItems: any[]) => {
    const uniqueItems = new Map<string, any>()

    rawItems.forEach((item: any) => {
      // Dùng productDetailId (hoặc id nếu chưa được map) làm key
      const keyId = item.productDetailId || item.id

      if (uniqueItems.has(keyId)) {
        const existing = uniqueItems.get(keyId)
        // Nếu trùng, giữ lại cái có phần trăm giảm giá cao hơn
        if ((item.percentage || 0) > (existing.percentage || 0)) {
          existing.percentage = item.percentage
        }
      }
      else {
        uniqueItems.set(keyId, { ...item })
      }
    })

    return Array.from(uniqueItems.values())
  }

  const fetchCartItem = async () => {
    try {
      if (cartId.value) {
        // Trường hợp ĐÃ ĐĂNG NHẬP (Lấy từ DB)
        const res = await GetGioHang(cartId.value as string)
        const rawData = res.data || []
        cartItems.value = mergeDuplicateCartItems(rawData)
      }
      else {
        // Trường hợp CHƯA ĐĂNG NHẬP (Lấy từ LocalStorage)
        const cartItemStorage = localStorageAction.get(CUSTOMER_CART_ITEM)
        if (!cartItemStorage)
          return

        const res = await getProductDetailCart(Object.keys(cartItemStorage))
        const rawData = (res.data || []).map((item: any) => {
          return { ...item, quantity: cartItemStorage[item.id] || 0, productDetailId: item.id }
        })

        // Lọc trùng lặp do Backend trả ra 2 đợt khuyến mãi
        cartItems.value = mergeDuplicateCartItems(rawData)
      }
    }
    catch (error) {
      console.error('Lỗi khi lấy thông tin giỏ hàng:', error)
    }
  }

  const updateQuantityLocalStorage = (idProductDetail: string, quantity: number) => {
    const cart = localStorageAction.get(CUSTOMER_CART_ITEM) ?? {}
    if (quantity !== 0) {
      cart[idProductDetail] = quantity
    }
    else {
      delete cart[idProductDetail]
    }
    localStorageAction.set(CUSTOMER_CART_ITEM, cart)
  }

  /**
   * 1. Thêm sản phẩm vào giỏ hàng (Dùng cho nút "Thêm vào giỏ" ở trang Chi Tiết SP)
   * CƠ CHẾ: CỘNG DỒN số lượng mới vào số lượng cũ
   */
  const addToCart = async (idProductDetail: string, quantity: number, option?: {
    buyNow?: boolean
  }) => {
    try {
      const { buyNow } = option || {}
      const MAX_TOTAL_AMOUNT = 500000000 // Chốt chặn 500 triệu

      if (buyNow) {
        const productResponse = await getProductDetailCart([idProductDetail])
        // Gộp nếu API trả 2 bản ghi
        const mergedProducts = mergeDuplicateCartItems(productResponse.data || [])
        const product = mergedProducts[0]

        if (!product)
          return false

        const price = product.percentage ? product.price * (1 - product.percentage / 100) : product.price

        if (price * quantity > MAX_TOTAL_AMOUNT) {
          return Promise.reject(new Error('Giá trị đơn mua ngay không được vượt quá 500.000.000đ'))
        }

        cartItemBuyNow.value = { ...product, productDetailId: idProductDetail, quantity }
        return true
      }

      const currentTotal = cartItems.value.reduce((total, item) => {
        const itemPrice = item.percentage ? item.price * (1 - item.percentage / 100) : item.price
        return total + (itemPrice * item.quantity)
      }, 0)

      const existingItem = cartItems.value.find(
        cartItem => cartItem.productDetailId === idProductDetail,
      )

      let expectedTotal = 0
      let productDataToAdd: any = null

      // Tính toán số lượng mục tiêu (CỘNG DỒN)
      let targetQuantity = quantity

      if (existingItem) {
        targetQuantity = existingItem.quantity + quantity // Cộng dồn

        const itemPrice = existingItem.percentage ? existingItem.price * (1 - existingItem.percentage / 100) : existingItem.price
        expectedTotal = currentTotal - (itemPrice * existingItem.quantity) + (itemPrice * targetQuantity)
      }
      else {
        const productResponse = await getProductDetailCart([idProductDetail])
        // Lọc trùng lặp
        const mergedProducts = mergeDuplicateCartItems(productResponse.data || [])
        productDataToAdd = mergedProducts[0]

        if (!productDataToAdd)
          return false

        const itemPrice = productDataToAdd.percentage ? productDataToAdd.price * (1 - productDataToAdd.percentage / 100) : productDataToAdd.price

        expectedTotal = currentTotal + (itemPrice * targetQuantity)
      }

      if (expectedTotal > MAX_TOTAL_AMOUNT) {
        return Promise.reject(new Error('Tổng giá trị giỏ hàng không được vượt quá 500.000.000đ'))
      }

      if (existingItem) {
        if (cartId.value) {
          await themSanPham({
            cartId: cartId.value as string,
            productDetailId: idProductDetail,
            quantity: targetQuantity,
          })
        }
        else {
          updateQuantityLocalStorage(idProductDetail, targetQuantity)
        }
        existingItem.quantity = targetQuantity
      }
      else {
        if (cartId.value) {
          await themSanPham({
            cartId: cartId.value as string,
            productDetailId: idProductDetail,
            quantity: targetQuantity,
          })
        }
        else {
          updateQuantityLocalStorage(idProductDetail, targetQuantity)
        }
        cartItems.value.push({ ...productDataToAdd, productDetailId: idProductDetail, quantity: targetQuantity })
      }
      return true
    }
    catch (error: any) {
      console.error('Lỗi khi thêm sản phẩm vào giỏ hàng:', error)
      return Promise.reject(error)
    }
  }

  /**
   * 2. Cập nhật CHÍNH XÁC số lượng (Dùng cho ô nhập số lượng có nút +/- ở trang Giỏ Hàng)
   * CƠ CHẾ: SET TRỰC TIẾP con số mới (không cộng dồn), có tự động rollback nếu API lỗi
   */
  const updateCartQuantity = async (idProductDetail: string, newQuantity: number) => {
    try {
      const MAX_TOTAL_AMOUNT = 500000000
      const existingItem = cartItems.value.find(
        cartItem => cartItem.productDetailId === idProductDetail,
      )

      if (!existingItem)
        return false

      // LƯU LẠI SỐ LƯỢNG CŨ TRƯỚC KHI ĐỔI (Dùng để giật lùi số lượng lại nếu bị lỗi tồn kho/tối đa 5sp)
      const oldQuantity = existingItem.quantity

      const currentTotal = cartItems.value.reduce((total, item) => {
        const itemPrice = item.percentage ? item.price * (1 - item.percentage / 100) : item.price
        return total + (itemPrice * item.quantity)
      }, 0)

      const itemPrice = existingItem.percentage ? existingItem.price * (1 - existingItem.percentage / 100) : existingItem.price
      const expectedTotal = currentTotal - (itemPrice * oldQuantity) + (itemPrice * newQuantity)

      if (expectedTotal > MAX_TOTAL_AMOUNT) {
        return Promise.reject(new Error('Tổng giá trị giỏ hàng không được vượt quá 500.000.000đ'))
      }

      // 1. Cập nhật UI ngay lập tức để cảm giác mượt mà
      existingItem.quantity = newQuantity

      // 2. Gọi API để lưu
      try {
        if (cartId.value) {
          await themSanPham({
            cartId: cartId.value as string,
            productDetailId: idProductDetail,
            quantity: newQuantity, // Gửi chính xác con số đang hiển thị trên ô input
          })
        }
        else {
          updateQuantityLocalStorage(idProductDetail, newQuantity)
        }
        return true
      }
      catch (apiError: any) {
        // QUAN TRỌNG: NẾU BACKEND BÁO LỖI -> Trả con số hiển thị về lại như cũ
        existingItem.quantity = oldQuantity
        return Promise.reject(apiError)
      }
    }
    catch (error) {
      console.error('Lỗi khi cập nhật giỏ hàng:', error)
      return Promise.reject(error)
    }
  }

  /**
   * 3. Xóa sản phẩm khỏi giỏ hàng
   */
  const removeCart = async (idProductDetail: string, option?: { buyNow?: boolean }) => {
    try {
      const { buyNow } = option || {}

      if (buyNow) {
        cartItemBuyNow.value = null
        return true
      }

      if (cartId.value) {
        await themSanPham({
          cartId: cartId.value as string,
          productDetailId: idProductDetail,
          quantity: 0,
        })
      }
      else {
        updateQuantityLocalStorage(idProductDetail, 0)
      }
      fetchCartItem()
      return true
    }
    catch (error) {
      console.error('Lỗi khi xóa sản phẩm khỏi giỏ hàng:', error)
      return false
    }
  }

  return {
    // State
    cartId,
    cartItems,
    cartItemBuyNow,
    // Methods
    fetchCartId,
    fetchCartItem,
    addToCart,
    updateCartQuantity, // Đã export hàm mới
    removeCart,
  }
})
