import { API_ADMIN_PRODUCT_OPERATING_SYSTEM } from '@/constants/url'
import { AxiosResponse } from 'axios'
import { DefaultResponse, PaginationParams, ResponseList } from './api.common'
import request from './request'

// Tham số truy vấn
export interface ParamsGetOperatingSystem extends PaginationParams {
  name?: string
}

// Kiểu dữ liệu OperatingSystem trả về
export interface OperatingSystemResponse extends ResponseList {
  id: string
  code: string
  name: string
  version: string
  description: string
  status: string
}

// Payload tạo OperatingSystem
export interface CreateOperatingSystemRequest {
  name: string
  code: string
  version: string
  description: string
}

// Lấy danh sách OperatingSystem
export const getAllOperatingSystems = async (params: ParamsGetOperatingSystem) => {
  const queryParams = { ...params }

  const res = (await request({
    url: API_ADMIN_PRODUCT_OPERATING_SYSTEM,
    method: 'GET',
    params: queryParams,
  })) as AxiosResponse<
    DefaultResponse<{
      data: OperatingSystemResponse[]
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

// Cập nhật trạng thái OperatingSystem
export const updateOperatingSystemStatus = async (id: string, status: 'ACTIVE' | 'INACTIVE') => {
  return request({
    url: `${API_ADMIN_PRODUCT_OPERATING_SYSTEM}/${id}/status`,
    method: 'PATCH',
    data: { status },
  })
}

// Tạo OperatingSystem mới
export const createOperatingSystem = async (payload: CreateOperatingSystemRequest) => {
  return request({
    url: `${API_ADMIN_PRODUCT_OPERATING_SYSTEM}/add`,
    method: 'POST',
    data: payload,
  })
}

// Cập nhật OperatingSystem
export const updateOperatingSystem = async (id: string, payload: CreateOperatingSystemRequest) => {
  return request({
    url: `${API_ADMIN_PRODUCT_OPERATING_SYSTEM}/${id}`,
    method: 'PUT',
    data: payload,
  })
}
