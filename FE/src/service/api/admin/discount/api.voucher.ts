// API Voucher
import type { DefaultResponse } from '@/typings/api/api.common'
import request from '@/service/request'
import { API_ADMIN_DISCOUNTS_VOUCHER } from '@/constants/url'
import axios from 'axios'
import type { Customer } from '@/service/api/admin/users/customer'

export interface ADVoucherQuery {
  page: number
  size: number
  q?: string
  conditions?: number
  status?: string
  startDate?: number
  endDate?: number
}

export interface customer {
  id: string
}

// Adjust interface để match response thực tế (field rename)
export interface ADVoucherResponse {
  id?: string
  code: string
  name: string
  typeVoucher: 'PERCENTAGE' | 'FIXED_AMOUNT'
  targetType: 'INDIVIDUAL' | 'ALL_CUSTOMERS'
  discountValue: number | null
  maxValue: number | null
  quantity: number | null
  remainingQuantity: number | null
  startDate: number | null
  endDate: number | null
  createdDate: number | null
  conditions: number | null
  note: string | null
  status: string | null
  voucherUsers: string[] | null
}

// Hàm getVouchers: Return {content: array, totalElements: number} để component dùng trực tiếp
export async function getVouchers(params: ADVoucherQuery) {
  try {
    const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}`, {
      method: 'GET',
      params,
    })

    const apiResponse = res.data // Root: {status, data: {data: [...], totalPages, currentPage, totalElements}, message, ...}

    let content: ADVoucherResponse[] = []
    const innerData = apiResponse?.data || {} // {data: [...], totalPages: ..., ...}

    if (innerData.data) {
      if (Array.isArray(innerData.data)) {
        content = innerData.data
      }
      else if (typeof innerData.data === 'object' && innerData.data !== null) {
        content = [innerData.data] // Wrap nếu single object
      }
    }

    const totalElements = innerData.totalElements ?? content.length ?? 0

    // Optional: Map field nếu cần (ví dụ: rename trong content)
    content = content.map(item => ({
      ...item,
      conditionOfUse: item.conditions, // Map về tên cũ nếu component dùng
      startTime: item.startDate,
      endTime: item.endDate,
      // Optional: Map voucherDetail sang IDs nếu frontend dùng string[]
      // voucherDetail: item.voucherDetail ? item.voucherDetail.map(detail => detail.customerId) : null,
    }))

    return { content, totalElements }
  }
  catch (error) {
    console.error('Failed to fetch vouchers:', error.response?.data || error.message) // Log chi tiết error 500
    return { content: [], totalElements: 0 }
  }
}

export async function getVoucherById(id: string) {
  const res = await request<DefaultResponse<ADVoucherResponse>>({
    url: `${API_ADMIN_DISCOUNTS_VOUCHER}/${id}`,
    method: 'GET',
  })
  // Optional: Map voucherDetail sang IDs ở đây nếu cần
  return res.data
}

// Hàm update status (adjust theo backend)
export async function updateVoucherStatus(id: string, newStatus: 'ACTIVE' | 'INACTIVE') {
  try {
    const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}/status`, {
      method: 'PATCH',
      data: { status: newStatus },
    })
    return res.data
  }
  catch (error) {
    console.error('Failed to update voucher status:', error)
    throw error
  }
}

export async function deleteVoucher(id: string) {
  return axios.delete(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}`)
}

// Xoá nhiều
export async function deleteVouchers(ids: string[]) {
  return axios.delete(`${API_ADMIN_DISCOUNTS_VOUCHER}`, { data: ids })
}

// Thêm
export function createVoucher(data: Partial<ADVoucherResponse>) {
  return request.post(`${API_ADMIN_DISCOUNTS_VOUCHER}`, data)
}

// Sửa
export function updateVoucher(id: string, data: Partial<ADVoucherResponse>) {
  return request.put(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}`, data)
}

// typings
export interface CustomerLite {
  id: string
  name: string
  email?: string
  phone?: string
}

// api.voucher.ts
export async function getVoucherCustomers(voucherId: string, onlyUsed = false): Promise<Customer[]> {
  const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}/${voucherId}/customers`, {
    method: 'GET',
    params: { onlyUsed },
  })
  const root = res.data
  // Hỗ trợ cả 2 kiểu: {status, data, message} hoặc trả mảng trực tiếp
  const items = (root?.data ?? root) as any
  const list = Array.isArray(items) ? items : (Array.isArray(items?.data) ? items.data : [])
  return (list as any[]).map(x => ({
    id: x.id || x.customerId || x.code,
    customerName: x.customerName || x.name || x.fullName || 'Khách hàng',
    customerEmail: x.customerEmail || x.email,
    customerPhone: x.customerPhone || x.phone,
    customerCode: x.customerCode || x.code,
    customerStatus: x.customerStatus ?? x.status ?? 1,
  }))
}
