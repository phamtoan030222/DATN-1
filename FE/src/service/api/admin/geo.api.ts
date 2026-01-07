import { API_ADMIN_GEO } from '@/constants/url'
import request from '@/service/request'
import type { AxiosResponse } from 'axios'

// ---- Province ----
export interface Province {
  code: number
  name: string
  division_type: string
  codename: string
  phone_code?: number
  districts?: District[] // Có thể kèm danh sách huyện
}

// ---- District ----
export interface District {
  code: number
  name: string
  division_type?: string
  codename?: string
  province_code?: number
  wards?: Ward[]
}

// ---- Ward ----
export interface Ward {
  code: number
  name: string
  division_type?: string
  codename?: string
  district_code?: number
}

// ---- API ----
export async function getProvinces(): Promise<Province[]> {
  const res = (await request({
    url: `${API_ADMIN_GEO}/provinces`,
    method: 'GET',
  })) as AxiosResponse<Province[]>
  return res.data
}

export async function getDistricts(provinceCode: string): Promise<District[]> {
  const res = (await request({
    url: `${API_ADMIN_GEO}/districts/${provinceCode}`,
    method: 'GET',
  })) as AxiosResponse<District[]>
  return res.data
}

export async function getCommunes(districtCode: string): Promise<Ward[]> {
  const res = (await request({
    url: `${API_ADMIN_GEO}/communes/${districtCode}`,
    method: 'GET',
  })) as AxiosResponse<Ward[]>
  return res.data
}
