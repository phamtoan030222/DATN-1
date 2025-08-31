import { API_ADMIN_HARD_DRIVE } from '@/constants/url'
import { AxiosResponse } from 'axios'
import { DefaultResponse, PaginationParams, ResponseList } from './api.common'
import request from './request'

// Tham số truy vấn
export interface ParamsGetHardDrive extends PaginationParams {
  name?: string
  brand?: string
}

// Kiểu dữ liệu ổ cứng trả về
export interface HardDriveResponse extends ResponseList {
  id: string
  name: string
  brand: string
  type: string
  typeConnect: string
  capacity: number
  readSpeed: number
  writeSpeed: number
  cacheMemory: number
  physicalSize: number
  status: string
  description: string
}

// Payload tạo ổ cứng
export interface CreateHardDriveRequest {
  name: string
  brand: string
  type: string
  typeConnect: string
  capacity: number
  readSpeed: number
  writeSpeed: number
  cacheMemory: number
  physicalSize: number
  description: string
}

// Lấy danh sách ổ cứng
export const getAllHardDrives = async (params: ParamsGetHardDrive) => {
  const queryParams = { ...params }

  const res = (await request({
    url: API_ADMIN_HARD_DRIVE,
    method: 'GET',
    params: queryParams,
  })) as AxiosResponse<
    DefaultResponse<{
      data: HardDriveResponse[]
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

// Cập nhật trạng thái ổ cứng
export const updateHardDriveStatus = async (id: string, status: 'ACTIVE' | 'INACTIVE') => {
  return request({
    url: `${API_ADMIN_HARD_DRIVE}/${id}/status`,
    method: 'PATCH',
    data: { status },
  })
}

// Tạo ổ cứng mới
export const createHardDrive = async (payload: CreateHardDriveRequest) => {
  return request({
    url: `${API_ADMIN_HARD_DRIVE}/add`,
    method: 'POST',
    data: payload,
  })
}

// Cập nhật ổ cứng
export const updateHardDrive = async (id: string, payload: CreateHardDriveRequest) => {
  return request({
    url: `${API_ADMIN_HARD_DRIVE}/${id}`,
    method: 'PUT',
    data: payload,
  })
}
