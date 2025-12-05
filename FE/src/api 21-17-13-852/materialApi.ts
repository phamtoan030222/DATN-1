import { API_ADMIN_MATERIAL } from '@/constants/url'
import { AxiosResponse } from 'axios'
import { DefaultResponse, PaginationParams, ResponseList } from './api.common'
import request from './request'

// ===== Tham số truy vấn =====
export interface ParamsGetMaterial extends PaginationParams {
  name?: string
}

// ===== Kiểu dữ liệu Material trả về =====
export interface MaterialResponse extends ResponseList {
  id: string
  code: string
  status: string
  topCaseMaterial?: string
  bottomCaseMaterial?: string
  keyboardMaterial?: string
}

// ===== Payload tạo Material =====
export interface CreateMaterialRequest {
  code?: string
  topCaseMaterial?: string
  bottomCaseMaterial?: string
  keyboardMaterial?: string
}

// ===== API =====

// Lấy danh sách Material
export const getAllMaterials = async (params: ParamsGetMaterial) => {
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
    items: res.data.data.data || [],
    totalItems: res.data.data.totalElements || 0,
    totalPages: res.data.data.totalPages || 0,
    currentPage: res.data.data.currentPage || 1,
  }
}

// Tạo Material mới
export const createMaterial = async (payload: CreateMaterialRequest) => {
  return request({
    url: `${API_ADMIN_MATERIAL}/add`,
    method: 'POST',
    data: payload,
  })
}

// Cập nhật Material
export const updateMaterial = async (id: string, payload: CreateMaterialRequest) => {
  return request({
    url: `${API_ADMIN_MATERIAL}/${id}`,
    method: 'PUT',
    data: payload,
  })
}

// Cập nhật trạng thái ACTIVE/INACTIVE
export const updateMaterialStatus = async (id: string, status: 'ACTIVE' | 'INACTIVE') => {
  return request({
    url: `${API_ADMIN_MATERIAL}/${id}/status`,
    method: 'PATCH',
    data: { status },
  })
}
