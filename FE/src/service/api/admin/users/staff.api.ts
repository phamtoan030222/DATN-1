import { API_ADMIN_STAFF } from '@/constants/url'
import type { AxiosResponse } from 'axios'
import type {
  DefaultResponse,
  PaginationParams,
  ResponseList,
} from '@/typings/api/api.common'
import request from '@/service/request'

export interface ParamGetStaff extends PaginationParams {
  key?: string
  name?: string
  role?: string
  status?: string
}

export type StaffResponse = ResponseList & {
  id: string
  fullName: string
  code?: string
  status: string // "ACTIVE", "INACTIVE"
  email: string
  role: string // "QUAN_LY", ...
  birthday?: number // timestamp
  citizenIdentifyCard?: string
  hometown: string
  provinceCode: string
  districtCode: string
  communeCode: string
  avatarUrl: string
  phone: string
  gender: boolean // backend trả true/false
}

export interface CreateStaffRequest {
  fullName: string
  role: string
  birthday?: number
  citizenIdentifyCard?: string
  hometown: string
  avatarUrl: string
  phone: string
  email: string
  gender: boolean // gửi true/false
  provinceCode: string
  districtCode: string
  communeCode: string
}

export async function getAllStaff(params: ParamGetStaff) {
  const res = (await request({
    url: API_ADMIN_STAFF,
    method: 'GET',
    params,
  })) as AxiosResponse<
    DefaultResponse<{
      data: StaffResponse[]
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

export async function createStaff(data: CreateStaffRequest) {
  const res = await request({
    url: `${API_ADMIN_STAFF}/add`,
    method: 'POST',
    data,
  })
  return res.data.data
}
export async function updateStaff(id: string, data: CreateStaffRequest) {
  const res = await request({
    url: `${API_ADMIN_STAFF}/${id}`,
    method: 'PUT',
    data,
  })
  return res.data.data
}

// Cập nhật trạng thái nhân viên (ACTIVE / INACTIVE)
export async function updateStaffStatus(id: string, status: 'ACTIVE' | 'INACTIVE') {
  const res = await request({
    url: `${API_ADMIN_STAFF}/${id}/status`,
    method: 'PATCH',
    data: { status },
  })
  return res.data.data
}

// Xóa 1 nhân viên
export async function deleteStaff(id: string) {
  const res = await request({
    url: `${API_ADMIN_STAFF}/${id}`,
    method: 'DELETE',
  })
  return res.data.data
}

// Xóa nhiều nhân viên
export async function deleteManyStaff(ids: string[]) {
  const res = await request({
    url: `${API_ADMIN_STAFF}/delete-many`,
    method: 'POST', // hoặc DELETE tuỳ backend định nghĩa
    data: { ids },
  })
  return res.data.data
}
