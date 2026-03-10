import { API_ADMIN_BATTERY } from '@/constants/url'
import request from '@/service/request'
import type { AxiosResponse } from 'axios'
import type {
  DefaultResponse,
  PaginationParams,
  ResponseList,
} from '@/typings/api/api.common'

export enum TechnolyCharging {
  SMART = 'SMART',
  REVERSE = 'REVERSE',
  FAST = 'FAST',
  STANDARD = 'STANDARD',
  WIRELESS = 'WIRELESS',
}

export enum TypeBattery {
  LI_ION = 'LI_ION',
  LI_PO = 'LI_PO',
  NIMH = 'NIMH',
  SOLID_STATE = 'SOLID_STATE',
}

export interface ParamsGetBattery extends PaginationParams {
  key?: string
  status?: string | null
}

export interface BatteryResponse extends ResponseList {
  id: string
  code: string
  name: string
  brand: string
  technolyCharging: TechnolyCharging
  capacity: number
  removeBattery: boolean
  typeBattery: TypeBattery 
  status: string
}

export interface CreateBatteryRequest {
  code?: string
  name: string
  brand: string
  technolyCharging: TechnolyCharging | string
  capacity: number
  removeBattery: boolean
  type: TypeBattery | string 
}

export async function getAllBattery(params: ParamsGetBattery) {
  const res = (await request({
    url: API_ADMIN_BATTERY,
    method: 'GET',
    params,
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

export async function updateBatteryStatus(id: string, status: 'ACTIVE' | 'INACTIVE') {
  return request({
    url: `${API_ADMIN_BATTERY}/${id}/status`,
    method: 'PATCH',
    data: { status },
  })
}

export async function createBattery(payload: CreateBatteryRequest) {
  return request({
    url: `${API_ADMIN_BATTERY}/add`,
    method: 'POST',
    data: payload,
  })
}

export async function updateBattery(id: string, payload: CreateBatteryRequest) {
  return request({
    url: `${API_ADMIN_BATTERY}/${id}`,
    method: 'PUT',
    data: payload,
  })
}