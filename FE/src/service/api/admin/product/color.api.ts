import { API_ADMIN_COLOR } from '@/constants/url'
import type { AxiosResponse } from 'axios'
import type {
  DefaultResponse,
  PaginationParams,
  ResponseList,
} from '@/typings/api/api.common'
import request from '@/service/request'

export interface ParamsGetColor extends PaginationParams {
  q?: string | ''
  colorName?: string
  colorStatus?: string
}

export type ColorResponse = ResponseList & {
  colorName: string
  colorCode: string
  colorStatus: string
  createdDate: number
}

export interface ColorRequest {
  colorName: string
}

export interface CreateColorRequest {
  colorName: string
  colorCode: string
}

export async function getAllColors(params: ParamsGetColor) {
  const queryParams = {
    ...params,
    colorName: params.q, // map q → colorName
  }

  const res = (await request({
    url: API_ADMIN_COLOR,
    method: 'GET',
    params: queryParams,
  })) as AxiosResponse<
    DefaultResponse<{
      data: ColorResponse[]
      totalPages: number
      currentPage: number
      totalElements: number
    }>
  >

  return {
    items: res.data.data.data || [],
    totalItems: res.data.data.totalElements || 0,
    totalPages: res.data.data.totalPages || 0,
    currentPage: res.data.data.currentPage || 1,
  }
}

export async function createColor(data: CreateColorRequest) {
  const res = await request({
    url: `${API_ADMIN_COLOR}/add`,
    method: 'POST',
    data,
  })
  return res.data.data
}

export async function updateColor() {}

export async function updateColorStatus() {}
