import { API_CUSTOMER_CUSTOMER_ADDRESSES } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'

export interface CustomerAddress {
  id?: string
  provinceCity?: string
  district?: string
  wardCommune?: string
  addressDetail?: string
  isDefault?: boolean
  status?: number
}

// Lấy địa chỉ mặc định
export async function getDefaultCustomerAddress(customerId: string) {
  const res = (await request({
    url: `${API_CUSTOMER_CUSTOMER_ADDRESSES(customerId)}/default`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<CustomerAddress | null>>
  return res.data
}

// Lấy danh sách địa chỉ
export function getAddressesByCustomer(customerId: string) {
  return request.get<{ data: CustomerAddress[] }>(API_CUSTOMER_CUSTOMER_ADDRESSES(customerId))
}

// 1. THÊM MỚI (POST)
export function createAddress(customerId: string, address: any) {
  return request.post<{ data: CustomerAddress }>(
    `${API_CUSTOMER_CUSTOMER_ADDRESSES(customerId)}`,
    address,
  )
}

// 2. SỬA (PUT) - Đã sửa lại để nhận đủ customerId và addressId
export function updateAddress(customerId: string, addressId: string, address: any) {
  return request.put<{ data: CustomerAddress }>(
    `${API_CUSTOMER_CUSTOMER_ADDRESSES(customerId)}/${addressId}`,
    address,
  )
}

// 3. XÓA (DELETE) - Đã sửa lại để nhận đủ customerId và addressId
export function deleteAddress(customerId: string, addressId: string) {
  return request.delete<void>(`${API_CUSTOMER_CUSTOMER_ADDRESSES(customerId)}/${addressId}`)
}

// 4. Đặt làm mặc định (PATCH)
export function setDefaultAddress(customerId: string, addressId: string) {
  return request.patch<void>(`${API_CUSTOMER_CUSTOMER_ADDRESSES(customerId)}/${addressId}/set-default`)
}
