import { API_ADMIN_BATTERY } from '@/constants/url'
import { AxiosResponse } from 'axios'
import { DefaultResponse, PaginationParams, ResponseList } from './api.common'
import request from './request'

// Tham số truy vấn
export interface ParamsGetBattery extends PaginationParams {
  name?: string
  brand?: string
}

// Kiểu dữ liệu Battery trả về
export interface BatteryResponse extends ResponseList {
  id: string
  name: string
  brand: string
  type: 'LI_ION' | 'LI_PO'
  capacity: number
  voltage: number
  chargingCycles: number
  technolyCharging: 'STANDARD' | 'FAST' | 'WIRELESS' | 'REVERSE' | 'SMART'
  status: string
  description: string
}

// Payload tạo Battery
export interface CreateBatteryRequest {
  name: string
  brand: string
  type: 'LI_ION' | 'LI_PO'
  capacity: number
  voltage: number
  chargingCycles: number
  technolyCharging: 'STANDARD' | 'FAST' | 'WIRELESS' | 'REVERSE' | 'SMART'
  description: string
}

// Lấy danh sách Battery
export const getAllBatteries = async (params: ParamsGetBattery) => {
  const queryParams = { ...params }

  const res = (await request({
    url: API_ADMIN_BATTERY,
    method: 'GET',
    params: queryParams,
  })) as AxiosResponse<
    DefaultResponse<{
      data: BatteryResponse[]
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

// Cập nhật trạng thái Battery
export const updateBatteryStatus = async (id: string, status: 'ACTIVE' | 'INACTIVE') => {
  return request({
    url: `${API_ADMIN_BATTERY}/${id}/status`,
    method: 'PATCH',
    data: { status },
  })
}

// Tạo Battery mới
export const createBattery = async (payload: CreateBatteryRequest) => {
  return request({
    url: `${API_ADMIN_BATTERY}/add`,
    method: 'POST',
    data: payload,
  })
}

// Cập nhật Battery
export const updateBattery = async (id: string, payload: CreateBatteryRequest) => {
  return request({
    url: `${API_ADMIN_BATTERY}/${id}`,
    method: 'PUT',
    data: payload,
  })
}
