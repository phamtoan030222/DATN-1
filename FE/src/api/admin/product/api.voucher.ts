import type { DefaultResponse, PaginationParams } from '@/api/api.common'
import request from '@/api/request'
import { API_ADMIN_PRODUCT_VOUCHER } from '@/constants/url'
import type { AxiosResponse } from 'axios'

export type ADVoucherRequest = PaginationParams & {
  typeVoucher: 'PERCENTAGE' | 'VND'
  voucherScope: 'PUBLIC' | 'PERSONAL'
  customerEmail: string
  decreaseUnit: number
  maximumReduction: number
  startTime: number
  endTime: number
  conditionOfUse: number
}

export interface ADVoucherCreateUpdateRequest {
  id?: string
  code: string
  name: string
  typeVoucher: 'PERCENTAGE' | 'VND'
  voucherScope: 'PUBLIC' | 'PERSONAL'
  customerEmail: string
  decreaseUnit: number
  maximumReduction: number
  startTime: number
  endTime: number
  conditionOfUse: number
}

export interface ADVoucherResponse {
  id?: string
  code: string
  name: string
  typeVoucher: 'PERCENTAGE' | 'VND'
  voucherScope: 'PUBLIC' | 'PERSONAL'
  customerEmail: string
  decreaseUnit: number
  maximumReduction: number
  startTime: number
  endTime: number
  conditionOfUse: number
}
// type cho query params phân trang
export interface ADVoucherQuery {
  page: number
  size: number
}

// API get voucher list
export async function getVouchers(params: ADVoucherQuery) {
  const res = await request(`${API_ADMIN_PRODUCT_VOUCHER}`, {
    method: 'GET',
    params, // hoặc query: params, tùy lib bạn dùng
  })
  return res.data
}

export async function getVoucherById(id: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_VOUCHER}/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<ADVoucherResponse>>

  return res.data
}
