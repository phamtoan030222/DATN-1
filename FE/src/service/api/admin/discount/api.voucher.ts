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

// Dữ liệu dùng cho UI (voucherUsers là mảng id KH để bind selection)
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
  voucherUsers?: string[] | null // <-- chỉ id KH cho UI
}

// Payload gửi BE khi tạo/sửa
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
  quantity?: number | null // ALL_CUSTOMERS
  voucherUsers?: { customer: { id: string } }[] // INDIVIDUAL
}

/* ===================== API ===================== */

// List vouchers (trả {content, totalElements} cho table)
export async function getVouchers(params: ADVoucherQuery) {
  try {
    const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}`, {
      method: 'GET',
      params,
    })
    const apiResponse = res.data // { status, data: { data, totalElements, ... }, message }
    const innerData = apiResponse?.data || {}

    let content: ADVoucherResponse[] = []
    if (Array.isArray(innerData.data))
      content = innerData.data
    else if (innerData.data && typeof innerData.data === 'object')
      content = [innerData.data]

    const totalElements = innerData.totalElements ?? content.length ?? 0

    // có thể map thêm field nếu BE khác tên:
    content = content.map(item => ({
      ...item,
      // ví dụ alias nếu FE cũ dùng tên khác:
      // conditionOfUse: item.conditions,
      // startTime: item.startDate,
      // endTime: item.endDate,
    }))

    return { content, totalElements }
  }
  catch (error: any) {
    console.error('Failed to fetch vouchers:', error?.response?.data || error?.message)
    return { content: [], totalElements: 0 }
  }
}

// Detail
export async function getVoucherById(id: string) {
  const res = await request<DefaultResponse<ADVoucherResponse>>({
    url: `${API_ADMIN_DISCOUNTS_VOUCHER}/${id}`,
    method: 'GET',
  })
  return res.data
}

// Toggle status (BE của bạn PATCH /{id}/status tự đảo trạng thái → không cần body)
export async function updateVoucherStatus(id: string) {
  const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}/status`, {
    method: 'PATCH',
  })
  return res.data
}

// Xoá 1
export async function deleteVoucher(id: string) {
  return axios.delete(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}`)
}

// Xoá nhiều
export async function deleteVouchers(ids: string[]) {
  return axios.delete(`${API_ADMIN_DISCOUNTS_VOUCHER}`, { data: ids })
}

// Thêm
export function createVoucher(data: ADVoucherUpsertPayload) {
  return request.post(`${API_ADMIN_DISCOUNTS_VOUCHER}`, data)
}

// Sửa
export function updateVoucher(id: string, data: ADVoucherUpsertPayload) {
  return request.put(`${API_ADMIN_DISCOUNTS_VOUCHER}/${id}`, data)
}

/* ====== Lấy khách hàng của một voucher (cho màn sửa) ====== */
// Trả về mảng Customer chuẩn hoá để FE hiển thị
export async function getVoucherCustomers(voucherId: string, onlyUsed = false): Promise<Customer[]> {
  const res = await request(`${API_ADMIN_DISCOUNTS_VOUCHER}/${voucherId}/customers`, {
    method: 'GET',
    params: { onlyUsed },
  })
  const root = res.data
  const raw = Array.isArray(root?.data) ? root.data : (Array.isArray(root) ? root : [])
  return raw
    .map((x: any) => ({
      id: String(x.id ?? x.customerId ?? x.code ?? ''),
      customerName: x.customerName ?? x.name ?? 'Khách hàng',
      customerEmail: x.customerEmail ?? x.email ?? '',
      customerPhone: x.customerPhone ?? x.phone ?? '',
      customerCode: x.customerCode ?? x.code ?? '',
      customerStatus: x.customerStatus ?? x.status ?? 1,
    }))
    .filter((c: { id: string }) => c.id !== '')
}
