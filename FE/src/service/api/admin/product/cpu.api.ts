import { API_ADMIN_PRODUCT_CPU } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse, PaginationParams, PaginationResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'

export type ADProductCPURequest = PaginationParams & {
  brand: string
  releaseYear: number | null
  generation: string
  series: string
  status: string
}

export interface ADProductCPUCreateUpdateRequest {
  id?: string
  code: string
  name: string
  brand: string
  description: string
  generation: string
  series: string
  releaseYear: number
}

export interface ADProductCPUResponse {
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

export async function getCPUs(params: ADProductCPURequest) {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_CPU}`,
    method: 'GET',
    params,
  }) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductCPUResponse>>>>

  return res.data
}

export async function getCPUById(id: string) {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_CPU}/${id}`,
    method: 'GET',
  }) as AxiosResponse<DefaultResponse<ADProductCPUResponse>>

  return res.data
}

export async function modifyCPU(data: ADProductCPUCreateUpdateRequest) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_CPU}`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<ADProductCPUResponse>>

  return res.data
}

export async function changeCPUStatus(id: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_CPU}/change-status/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}
