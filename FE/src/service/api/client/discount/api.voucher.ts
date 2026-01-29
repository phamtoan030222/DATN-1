// API Voucher
import type { DefaultResponse } from '@/typings/api/api.common'
import request from '@/service/request'
import { API_CUSTOMER_DISCOUNTS_VOUCHER } from '@/constants/url'
import axios from 'axios'

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
    const res = await request(`${API_CUSTOMER_DISCOUNTS_VOUCHER}`, {
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
    url: `${API_CUSTOMER_DISCOUNTS_VOUCHER}/${id}`,
    method: 'GET',
  })
  return res.data
}
