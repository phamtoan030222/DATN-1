// src/service/api/admin/hoadon.api.ts
import request from "@/service/request"
import { DefaultResponse, PaginationParams } from "@/typings/api/api.common"
import { AxiosResponse } from "axios"

const API_HOA_DON = '/api/v1/admin/hoa-don'

export interface ParamsGetHoaDon extends PaginationParams {
  q?: string
  status?: string | null
  startDate?: number | null
  endDate?: number | null
  sort?: string
  loaiHoaDon?: string | null
}

export interface HoaDonItem {
  id: string
  maHoaDon: string | null
  tenKhachHang: string | null
  sdtKhachHang: string | null
  maNhanVien: string
  tenNhanVien: string | null
  tongTien: number
  loaiHoaDon: string
  createdDate: number
  status: string
}

export interface HoaDonResponse {
  page: {
    content: HoaDonItem[]
    pageable: {
      pageNumber: number
      pageSize: number
      sort: {
        empty: boolean
        sorted: boolean
        unsorted: boolean
      }
      offset: number
      paged: boolean
      unpaged: boolean
    }
    last: boolean
    totalElements: number
    totalPages: number
    first: boolean
    size: number
    number: number
    sort: {
      empty: boolean
      sorted: boolean
      unsorted: boolean
    }
    numberOfElements: number
    empty: boolean
  }
  countByStatus: Record<string, number>
}

export const GetHoaDons = async (params: ParamsGetHoaDon = {}): Promise<DefaultResponse<HoaDonResponse>> => {
  try {
    // Đảm bảo page không nhỏ hơn 0
    const page = params.page !== undefined ? Math.max(1, params.page) : 1
    const size = params.size !== undefined ? Math.max(1, params.size) : 10
    
    const apiParams: any = {
      page: page,
      size: size,
    }
    
    // Thêm các tham số tùy chọn
    if (params.q && params.q.trim() !== '') {
      apiParams.search = params.q.trim()
    }
    
    if (params.status) {
      apiParams.status = params.status
    }
    
    if (params.sort) {
      apiParams.sort = params.sort
    }
    
    if (params.loaiHoaDon) {
      apiParams.loaiHoaDon = params.loaiHoaDon
    }
    
    if (params.startDate) {
      apiParams.startDate = params.startDate
    }
    
    if (params.endDate) {
      apiParams.endDate = params.endDate
    }
    
    console.log('API Request Params:', apiParams)
    
    const res = await request({
      url: API_HOA_DON,
      method: 'GET',
      params: apiParams
    })
    
    console.log('API Response:', res.data)
    return res.data
  } catch (error) {
    console.error('Error fetching hoa dons:', error)
    
    // Nếu có lỗi, trả về response mặc định
    const errorResponse: DefaultResponse<HoaDonResponse> = {
      status: "ERROR",
      data: {
        page: {
          content: [],
          pageable: {
            pageNumber: 0,
            pageSize: 10,
            sort: { empty: false, sorted: true, unsorted: false },
            offset: 0,
            paged: true,
            unpaged: false
          },
          last: true,
          totalElements: 0,
          totalPages: 0,
          first: true,
          size: 10,
          number: 0,
          sort: { empty: false, sorted: true, unsorted: false },
          numberOfElements: 0,
          empty: true
        },
        countByStatus: {}
      },
      message: error instanceof Error ? error.message : "Lỗi không xác định",
      timestamp: new Date().toISOString(),
      success: false
    }
    
    return errorResponse
  }
}