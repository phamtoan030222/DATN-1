import type { DefaultResponse, PaginationParams } from '@/api/api.common'
import request from '@/api/request'
import { API_ADMIN_PRODUCT_VOUCHER } from '@/constants/url'
import type { AxiosResponse } from 'axios'

export interface ADVoucherQuery {
  page: number
  size: number
}

// Adjust interface để match response thực tế (field rename)
export interface ADVoucherResponse {
  id?: string
  code: string
  name: string
  typeVoucher: 'PERCENTAGE' | 'FIXED_AMOUNT'
  targetType: 'LIMITED_BY_CONDITION' | 'INDIVIDUAL' | 'ALL_CUSTOMERS'
  discountValue: number
  maxValue: number
  quantity: number
  remainingQuantity: number
  startDate: number // Rename từ startTime
  endDate: number // Rename từ endTime
  conditions: number // Rename từ conditionOfUse
  note: string | null
  status: string | null
}

// Hàm getVouchers: Return {content: array, totalElements: number} để component dùng trực tiếp
export async function getVouchers(params: ADVoucherQuery) {
  try {
    const res = await request(`${API_ADMIN_PRODUCT_VOUCHER}`, {
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
    url: `${API_ADMIN_PRODUCT_VOUCHER}/${id}`,
    method: 'GET',
  })
  return res.data
}

// ... code hiện tại giữ nguyên

// Hàm update status (adjust theo backend)
export async function updateVoucherStatus(id: string, newStatus: 'ACTIVE' | 'INACTIVE') {
  try {
    const res = await request(`${API_ADMIN_PRODUCT_VOUCHER}/${id}/status`, {  // Adjust endpoint nếu khác, ví dụ: /voucher/{id}
      method: 'PUT',  // Hoặc PATCH nếu backend dùng
      data: { status: newStatus },
    });
    return res.data;  // Return updated voucher nếu cần
  } catch (error) {
    console.error('Failed to update voucher status:', error);
    throw error;  // Để component catch
  }
}