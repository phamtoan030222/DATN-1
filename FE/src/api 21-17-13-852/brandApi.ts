import { API_ADMIN_PRODUCT_BRAND } from '@/constants/url'
import { AxiosResponse } from 'axios'
import { DefaultResponse, PaginationParams, ResponseList } from './api.common'
import request from './request'

// Tham số truy vấn
export interface ParamsGetBrand extends PaginationParams {
  name?: string
}

// Kiểu dữ liệu Brand trả về
export interface BrandResponse extends ResponseList {
  id: string
  code: string
  name: string
  status: string
}

// Payload tạo Brand
export interface CreateBrandRequest {
  name: string
  code: string
}

// Lấy danh sách Brand
export const getAllBrands = async (params: ParamsGetBrand) => {
  const queryParams = { ...params }

  const res = (await request({
    url: API_ADMIN_PRODUCT_BRAND,
    method: 'GET',
    params: queryParams,
  })) as AxiosResponse<
    DefaultResponse<{
      data: BrandResponse[]
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

// Cập nhật trạng thái Brand
export const updateBrandStatus = async (id: string, status: 'ACTIVE' | 'INACTIVE') => {
  return request({
    url: `${API_ADMIN_PRODUCT_BRAND}/${id}/status`,
    method: 'PATCH',
    data: { status },
  })
}

// Tạo Brand mới
export const createBrand = async (payload: CreateBrandRequest) => {
  return request({
    url: `${API_ADMIN_PRODUCT_BRAND}/add`,
    method: 'POST',
    data: payload,
  })
}

// Cập nhật Brand
export const updateBrand = async (id: string, payload: CreateBrandRequest) => {
  return request({
    url: `${API_ADMIN_PRODUCT_BRAND}/${id}`,
    method: 'PUT',
    data: payload,
  })
}
