import { API_ADMIN_RAM } from '@/constants/url'
import { AxiosResponse } from 'axios'
import { DefaultResponse, PaginationParams, ResponseList } from './api.common'
import request from './request'

// Tham số truy vấn
export interface ParamsGetRam extends PaginationParams {
  name?: string
}

// Kiểu dữ liệu RAM trả về
export interface RamResponse extends ResponseList {
  id: string
  name: string
  brand: string
  type: string
  capacity: number
  busSpeed: number
  slotConFig: number
  maxSupported: number
  status: string
  description: string
}

// Payload tạo RAM
export interface CreateRamRequest {
  name: string
  brand: string
  type: string
  capacity: number
  busSpeed: number
  slotConFig: number
  maxSupported: number
  description: string
}

// Lấy danh sách RAM
export const getAllRams = async (params: ParamsGetRam) => {
  const queryParams = { ...params }

  const res = (await request({
    url: API_ADMIN_RAM,
    method: 'GET',
    params: queryParams,
  })) as AxiosResponse<
    DefaultResponse<{
      data: RamResponse[]
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

// Cập nhật trạng thái RAM
export const updateRamStatus = async (id: string, status: 'ACTIVE' | 'INACTIVE') => {
  return request({
    url: `${API_ADMIN_RAM}/${id}/status`,
    method: 'PATCH',
    data: { status },
  })
}

// Tạo RAM mới
export const createRam = async (payload: CreateRamRequest) => {
  return request({
    url: `${API_ADMIN_RAM}/add`,
    method: 'POST',
    data: payload,
  })
}

// Cập nhật RAM
export const updateRam = async (id: string, payload: CreateRamRequest) => {
  return request({
    url: `${API_ADMIN_RAM}/${id}`,
    method: 'PUT',
    data: payload,
  })
}
