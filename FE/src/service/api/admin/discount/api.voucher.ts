// API Voucher
import type { DefaultResponse } from '@/typings/api/api.common'
import request from '@/service/request'
import { API_ADMIN_DISCOUNTS_VOUCHER } from '@/constants/url'
import axios from 'axios'
import type { Customer } from '@/service/api/admin/users/customer/customer'

/* ===================== Types ===================== */
export interface ADVoucherQuery {
  page: number
  size: number
  q?: string
  conditions?: number
  status?: string
  startDate?: number
  endDate?: number
}

// Dữ liệu dùng cho UI
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
  startDate: number | undefined | null
  endDate: number | undefined | null
  createdDate: number | null
  conditions: number | null
  note: string | null
  status: string | null
  voucherUsers?: string[] | null
}

// Payload gửi BE
export interface ADVoucherUpsertPayload {
  name: string
  typeVoucher: 'PERCENTAGE' | 'FIXED_AMOUNT'
  targetType: 'INDIVIDUAL' | 'ALL_CUSTOMERS'
  discountValue: number
  maxValue?: number | null
  conditions?: number | null
  startDate: number
  endDate: number
  note?: string | null
  quantity?: number | null
  status?: string | null
  voucherUsers?: { customer: { id: string } }[]
}

/* ===================== API ===================== */

export async function getVouchers(params: ADVoucherQuery) {
  try {
    const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}`, {
      method: 'GET',
      params,
    })
    const apiResponse = res.data
    const innerData = apiResponse?.data || {}

    let content: ADVoucherResponse[] = []
    if (Array.isArray(innerData.data))
      content = innerData.data
    else if (innerData.data && typeof innerData.data === 'object')
      content = [innerData.data]

    const totalElements = innerData.totalElements ?? content.length ?? 0

    content = content.map(item => ({
      ...item,
    }))

    return { content, totalElements }
  }
  catch (error: any) {
    console.error('Failed to fetch vouchers:', error?.response?.data || error?.message)
    return { content: [], totalElements: 0 }
  }
}

export async function getVoucherById(id: string) {
  const res = await request<DefaultResponse<ADVoucherResponse>>({
    url: `${API_ADMIN_DISCOUNTS_VOUCHER}/${id}`,
    method: 'GET',
  })
  return res.data
}

export async function updateVoucherStatus(id: string) {
  const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}/status`, {
    method: 'PATCH',
  })
  return res.data
}

export async function deleteVoucher(id: string) {
  return axios.delete(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}`)
}

export async function deleteVouchers(ids: string[]) {
  return axios.delete(`${API_ADMIN_DISCOUNTS_VOUCHER}`, { data: ids })
}

export function createVoucher(data: ADVoucherUpsertPayload) {
  return request.post(`${API_ADMIN_DISCOUNTS_VOUCHER}`, data)
}

export function updateVoucher(id: string, data: ADVoucherUpsertPayload) {
  return request.put(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}`, data)
}

// 2 API Start/End
export async function updateVoucherToStart(id: string) {
  const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}/start`, { method: 'PATCH' })
  return res.data
}

export async function updateVoucherToEnd(id: string) {
  const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}/end`, { method: 'PATCH' })
  return res.data
}

/* ====== Lấy khách hàng của một voucher (cho màn sửa) ====== */
export async function getVoucherCustomers(voucherId: string, onlyUsed = false): Promise<Customer[]> {
  const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}/${voucherId}/customers`, {
    method: 'GET',
    params: { onlyUsed },
  })
  const root = res.data

  // --- SỬA LOGIC LẤY DỮ LIỆU TẠI ĐÂY ---
  let raw: any[] = []

  // Trường hợp 1: Backend trả về PageableObject (có data.data là mảng)
  if (root?.data?.data && Array.isArray(root.data.data)) {
    raw = root.data.data
  }
  // Trường hợp 2: Backend trả về List trực tiếp trong data
  else if (Array.isArray(root?.data)) {
    raw = root.data
  }
  // Trường hợp 3: Backend trả về mảng ngay root (ít gặp với format response của bạn)
  else if (Array.isArray(root)) {
    raw = root
  }

  return raw
    .map((x: any) => ({
      // Map linh hoạt các trường ID
      id: String(x.id ?? x.customerId ?? x.code ?? ''),
      // Map linh hoạt tên
      customerName: x.customerName ?? x.name ?? 'Khách hàng',
      customerEmail: x.customerEmail ?? x.email ?? '',
      customerPhone: x.customerPhone ?? x.phone ?? '',
      customerCode: x.customerCode ?? x.code ?? '',
      customerStatus: x.customerStatus ?? x.status ?? 1,
    }))
    .filter((c: { id: string }) => c.id !== '')
}
