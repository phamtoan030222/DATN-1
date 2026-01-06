import { API_ADMIN_MATERIAL } from '@/constants/url'
import type { AxiosResponse } from 'axios'
import type {
  DefaultResponse,
  PaginationParams,
  ResponseList,
} from '@/typings/api/api.common'
import request from '@/service/request'

export interface ParamsGetMaterial extends PaginationParams {
  key?: string
  status?: string | null
}

export interface MaterialResponse extends ResponseList {
  id: string
  code: string
  status: string
  topCaseMaterial?: string
  bottomCaseMaterial?: string
  keyboardMaterial?: string
}

export interface CreateMaterialRequest {
  id?: string
  code?: string
  topCaseMaterial?: string
  bottomCaseMaterial?: string
  keyboardMaterial?: string
  status?: string
}

// ===== API Methods =====

export async function getAllMaterials(params: ParamsGetMaterial) {
  const queryParams = { ...params }

  const res = (await request({
    url: API_ADMIN_MATERIAL,
    method: 'GET',
    params: queryParams,
  })) as AxiosResponse<
    DefaultResponse<{
      data: MaterialResponse[]
      totalPages: number
      currentPage: number
      totalElements: number
    }>
  >

  return {
    data: res.data?.data?.data || [],
    totalElements: res.data?.data?.totalElements || 0,
    totalPages: res.data?.data?.totalPages || 0,
    currentPage: res.data?.data?.currentPage || 1,
  }
}

// ĐÃ XÓA hàm getMaterialById

export async function createMaterial(payload: CreateMaterialRequest) {
  return request({
    url: `${API_ADMIN_MATERIAL}/add`,
    method: 'POST',
    data: payload,
  })
}

export async function updateMaterial(id: string, payload: CreateMaterialRequest) {
  return request({
    url: `${API_ADMIN_MATERIAL}/${id}`,
    method: 'PUT',
    data: payload,
  })
}

export async function updateMaterialStatus(id: string) {
  return request({
    url: `${API_ADMIN_MATERIAL}/${id}/status`,
    method: 'PATCH',
  })
}
