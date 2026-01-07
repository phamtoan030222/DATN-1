import { API_ADMIN_COLOR } from '@/constants/url'
import request from '@/service/request'
import type { AxiosResponse } from 'axios'
import type {
  DefaultResponse,
  PaginationParams,
  ResponseList,
} from '@/typings/api/api.common'

// ===== 1. Interfaces =====

export interface ParamsGetColors extends PaginationParams {
  q?: string
  status?: string | null
}

export interface AdColorResponse extends ResponseList {
  id: string
  code: string
  name: string
  status: string
}

export interface AdColorRequest {
  id?: string
  code: string
  name: string
  status?: string
}

export interface ColorCreateUpdateRequest {
  id?: string
  code: string
  name: string
  status?: string
}

// ===== 2. API Methods =====

export async function getAllColors(params: ParamsGetColors) {
  const res = (await request({
    url: API_ADMIN_COLOR,
    method: 'GET',
    params,
  })) as AxiosResponse<
    DefaultResponse<{
      data: AdColorResponse[]
      totalPages: number
      totalElements: number
      currentPage: number
    }>
  >

  return {
    data: res.data?.data?.data || [],
    totalPages: res.data?.data?.totalPages || 0,
    totalElements: res.data?.data?.totalElements || 0,
    currentPage: res.data?.data?.currentPage || 1,
  }
}

export async function createColor(payload: ColorCreateUpdateRequest) {
  return request({
    url: `${API_ADMIN_COLOR}/add`,
    method: 'POST',
    data: payload,
  })
}

export async function updateColor(id: string, payload: ColorCreateUpdateRequest) {
  return request({
    url: `${API_ADMIN_COLOR}/${id}`,
    method: 'PUT',
    data: payload,
  })
}

export async function updateColorStatus(id: string) {
  return request({
    url: `${API_ADMIN_COLOR}/${id}/status`,
    method: 'PATCH',
  })
}
