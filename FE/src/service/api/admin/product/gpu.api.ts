import { API_ADMIN_PRODUCT_GPU } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse, PaginationParams, PaginationResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'

export type ADProductGPURequest = PaginationParams & {
  brand: string
  releaseYear: number | undefined
  generation: string
  series: string
}

export interface ADProductGPUCreateUpdateRequest {
  id?: string
  code?: string
  name: string
  brand: string
  description: string
  generation: string
  series: string
  releaseYear?: number
  status?: string
}

export interface ADProductGPUResponse {
  id?: string
  code: string
  name: string
  status?: string
  description: string
  generation: string
  series: string
  brand: string
  releaseYear: number
}

export async function getGPUs(params: ADProductGPURequest) {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_GPU}`,
    method: 'GET',
    params,
  }) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductGPUResponse>>>>

  return res.data
}

export async function getGPUById(id: string) {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_GPU}/${id}`,
    method: 'GET',
  }) as AxiosResponse<DefaultResponse<ADProductGPUResponse>>

  return res.data
}

export async function modifyGPU(data: ADProductGPUCreateUpdateRequest) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_GPU}`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}

export async function changeGPUStatus(id: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_GPU}/change-status/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}
