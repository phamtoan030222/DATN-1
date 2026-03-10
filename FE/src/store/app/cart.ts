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

  const { userInfoDatn } = useAuthStore();

  const fetchCartId = async () => {
    try {
      if (userInfoDatn?.rolesCodes?.includes('KHACH_HANG')) {
        const res = await getCartByCustomer(userInfoDatn.userId as string)
        cartId.value = res.data || null
        if (cartId.value) {
          localStorageAction.set(CUSTOMER_CART_ID, cartId.value)
        }
      } else {
        localStorageAction.remove(CUSTOMER_CART_ID)
      }
    } catch (error) {
      console.error('Lỗi khi lấy thông tin giỏ hàng:', error)
    }
  }

  const fetchCartItem = async () => {
    try {
      if (cartId.value) {
        const res = await GetGioHang(cartId.value as string)
        cartItems.value = res.data || []
      } else {
        const cartItemStorage = localStorageAction.get(CUSTOMER_CART_ITEM)
        if (!cartItemStorage) return;

        const res = await getProductDetailCart(Object.keys(cartItemStorage))
        cartItems.value = (res.data || []).map((item: any) => {
          return { ...item, quantity: cartItemStorage[item.id] || 0, productDetailId: item.id }
        })
      }
    } catch (error) {
      console.error('Lỗi khi lấy thông tin giỏ hàng:', error)
    }
  }
  // Methods
  /**
   * Thêm sản phẩm vào giỏ hàng
   */
  const updateQuantityLocalStorage = (idProductDetail: string, quantity: number) => {
    const cart = localStorageAction.get(CUSTOMER_CART_ITEM) ?? {}
    if (quantity !== 0) {
      cart[idProductDetail] = quantity
    } else {
      delete cart[idProductDetail]
    }
    localStorageAction.set(CUSTOMER_CART_ITEM, cart)
  }

  const addToCart = async (idProductDetail: string, quantity: number, option?: {
    buyNow?: boolean
  }) => {
    try {
      const { buyNow } = option || {}

      if (buyNow) {
        const productResponse = await getProductDetailCart([idProductDetail])
        cartItemBuyNow.value = { ...productResponse.data[0], productDetailId: idProductDetail, quantity }
        return true;
      }

      const existingItem = cartItems.value.find(
        (cartItem) => cartItem.productDetailId === idProductDetail
      )

      if (existingItem) {
        if (cartId.value) {
          await themSanPham({
            cartId: cartId.value as string,
            productDetailId: idProductDetail,
            quantity
          })
        } else {
          updateQuantityLocalStorage(idProductDetail, quantity)
        }
        existingItem.quantity = quantity
      } else {
        // Nếu không tồn tại, thêm sản phẩm mới
        if (cartId.value) {
          await themSanPham({
            cartId: cartId.value as string,
            productDetailId: idProductDetail,
            quantity
          })
        } else {
          updateQuantityLocalStorage(idProductDetail, quantity)
        }
        const productResponse = await getProductDetailCart([idProductDetail])

        cartItems.value.push({ ...productResponse.data[0], productDetailId: idProductDetail, quantity })
      }
      return true;
    } catch (error) {
      console.error('Lỗi khi thêm sản phẩm vào giỏ hàng:', error)
      return false;
    }
  }
  /**
   * Xóa sản phẩm khỏi giỏ hàng
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
          quantity: 0
        })
      } else {
        updateQuantityLocalStorage(idProductDetail, 0)
      }
      fetchCartItem()
      return true;
    } catch (error) {
      console.error('Lỗi khi xóa sản phẩm khỏi giỏ hàng:', error)
      return false;
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
    removeCart,
  }
})
