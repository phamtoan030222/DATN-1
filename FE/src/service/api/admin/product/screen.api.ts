import { API_ADMIN_PRODUCT_SCREEN } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse, PaginationParams, PaginationResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'

export type ADProductScreenRequest = PaginationParams & {
  physicalSize?: number
  resolution?: string
  panelType?: string
  technology?: string
  status?: string
}

export interface ADProductScreenCreateUpdateRequest {
  id?: string
  code: string
  name: string
  physicalSize: number
  resolution: string
  panelType: string
  technology: string
}

export interface ADProductScreenResponse {
  id?: string
  code: string
  name: string
  status?: string
  physicalSize: number
  resolution: string
  panelType: string
  technology: string
}

export interface ADProductScreenDetailResponse {
  id?: string
  code: string
  name: string
  physicalSize: number
  resolution: string
  panelType: string
  technology: string
}

export interface ADProductScreenResolutionResponse {
  id: string
  code: string
  name: string
}

export async function getScreens(params: ADProductScreenRequest) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_SCREEN}`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductScreenResponse>>>>

  return res.data
}

export async function getScreenById(id: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_SCREEN}/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<ADProductScreenDetailResponse>>

  return res.data
}

export async function modifyScreen(data: ADProductScreenCreateUpdateRequest) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_SCREEN}`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<ADProductScreenResponse>>

  return res.data
}

export async function changeScreenStatus(id: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_SCREEN}/change-status/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}
