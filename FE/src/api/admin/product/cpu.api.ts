import { DefaultResponse, PaginationParams, PaginationResponse } from '@/api/api.common'
import request from '@/api/request'
import { API_ADMIN_PRODUCT_CPU } from '@/constants/url'
import { AxiosResponse } from 'axios'

export type ADProductCPURequest = PaginationParams & {
  brand: string,
  releaseYear: number | undefined,
  generation: string,
  series: string,
}

export type ADProductCPUCreateUpdateRequest = {
  id?: string;
  code: string;
  name: string;
  brand: string;
  description: string;
  generation: string;
  series: string;
  releaseYear: number;
}

export type ADProductCPUResponse = {
  id?: string,
  code: string;
  name: string;
  description: string;
  generation: string;
  series: string;
  brand: string;
  releaseYear: number;
}

export const getCPUs = async (params: ADProductCPURequest) => {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_CPU}`,
    method: 'GET',
    params,
  }) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductCPUResponse>>>>

  return res.data;
}

export const getCPUById = async (id: string) => {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_CPU}/${id}`,
    method: 'GET',
  }) as AxiosResponse<DefaultResponse<ADProductCPUResponse>>

  return res.data;
}

export const modifyCPU = async (data: ADProductCPUCreateUpdateRequest) => {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_CPU}`,
    method: 'POST',
    data
  }) as AxiosResponse<DefaultResponse<ADProductCPUResponse>>

  return res.data;
}
