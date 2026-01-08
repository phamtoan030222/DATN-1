// File: admin/users/customer/address.ts

// 1. Thay thế import axios bằng request (đã cấu hình Token)
import request from '@/service/request'
import {
  API_ADMIN_CUSTOMER_ADDRESS_DEFAULT,
  API_ADMIN_CUSTOMER_ADDRESSES,
} from '@/constants/url'

export interface Address {
  id?: string
  provinceCity: string
  district: string
  wardCommune: string
  addressDetail: string
  isDefault?: boolean
  createdDate?: number
  modifiedDate?: number
}

export function getAddressesByCustomer(customerId: string) {
  // 2. Đổi axios.get -> request.get
  return request.get<{ data: Address[] }>(API_ADMIN_CUSTOMER_ADDRESSES(customerId))
}

export function createAddress(customerId: string, address: Address) {
  // 3. Đổi axios.post -> request.post
  return request.post<{ data: Address }>(API_ADMIN_CUSTOMER_ADDRESSES(customerId), address)
}

export function updateAddress(customerId: string, addressId: string, address: Address) {
  // 4. Đổi axios.put -> request.put
  return request.put<{ data: Address }>(
    `${API_ADMIN_CUSTOMER_ADDRESSES(customerId)}/${addressId}`,
    address,
  )
}

export function deleteAddress(customerId: string, addressId: string) {
  // 5. Đổi axios.delete -> request.delete
  return request.delete<void>(`${API_ADMIN_CUSTOMER_ADDRESSES(customerId)}/${addressId}`)
}

export function setDefaultAddress(customerId: string, addressId: string) {
  // 6. Đổi axios.put -> request.put
  return request.put<void>(API_ADMIN_CUSTOMER_ADDRESS_DEFAULT(customerId, addressId))
}
