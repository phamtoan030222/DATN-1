import { API_ADMIN_COLOR } from '@/constants/url'
import { DefaultResponse, PaginationParams, ResponseList } from '@/types/api.common'
import { AxiosResponse } from 'axios'
import request from './request'

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

export const getAllColors = async (params: ParamsGetColor) => {
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

export const createColor = async (data: CreateColorRequest) => {
  const res = await request({
    url: `${API_ADMIN_COLOR}/add`,
    method: 'POST',
    data,
  })
  return res.data.data
}

export const updateColor = async () => {}

export const updateColorStatus = async () => {}
