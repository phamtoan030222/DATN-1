import { DefaultResponse, PaginationParams, PaginationResponse } from '@/api/api.common'
import request from '@/api/request'
import { API_ADMIN_PRODUCT_GPU } from '@/constants/url'
import { AxiosResponse } from 'axios'

export type ADProductGPURequest = PaginationParams & {
  brand: string,
  releaseYear: number | undefined,
  generation: string,
  series: string,
}

export type ADProductGPUCreateUpdateRequest = {
  id?: string;
  code: string;
  name: string;
  brand: string;
  description: string;
  generation: string;
  series: string;
  releaseYear: number;
}

export type ADProductGPUResponse = {
  id?: string,
  code: string;
  name: string;
  description: string;
  generation: string;
  series: string;
  brand: string;
  releaseYear: number;
}

export const getGPUs = async (params: ADProductGPURequest) => {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_GPU}`,
    method: 'GET',
    params,
  }) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductGPUResponse>>>>

  return res.data;
}

export const getGPUById = async (id: string) => {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_GPU}/${id}`,
    method: 'GET',
  }) as AxiosResponse<DefaultResponse<ADProductGPUResponse>>

  return res.data;
}

export const modifyGPU = async (data: ADProductGPUCreateUpdateRequest) => {
  const res = await request({
    url: `${API_ADMIN_PRODUCT_GPU}`,
    method: 'POST',
    data
  }) as AxiosResponse<DefaultResponse<ADProductGPUResponse>>

  return res.data;
}
