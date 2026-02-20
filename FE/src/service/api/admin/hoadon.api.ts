// src/service/api/admin/hoadon.api.ts
import { PaginationResponse } from "@/service/api.common"
import request from "@/service/request"
import { DefaultResponse, PaginationParams } from "@/typings/api/api.common"

const API_HOA_DON = '/api/v1/admin/hoa-don'

// ==================== INTERFACES ====================

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

// ==================== CHI TIẾT HÓA ĐƠN ====================

export interface ParamsGetHoaDonCT extends PaginationParams {
  maHoaDon?: string | ''
}

export interface LichSuTrangThaiItem {
  id: number
  trangThai: number
  tenTrangThai: string
  thoiGian: string // ISO string
  ghiChu: string
  nhanVien: string | null
  maNhanVien: string | null
}

export interface HoaDonChiTietItem {
  // Thông tin hóa đơn
  maHoaDon: string
  tenHoaDon: string | null
  loaiHoaDon: string
  trangThaiHoaDon: string
  trangThaiText: string
  ngayTao: number
  thoiGianCapNhatCuoi: string | null
  phiVanChuyen: number | null
  tongTienSauGiam: number
  giaTriVoucher: number
  phuongThucThanhToan: string | null
  tenNhanVien: string | null
  
  // Thông tin sản phẩm (có thể null nếu hóa đơn không có sản phẩm)
  maHoaDonChiTiet: string | null
  tenSanPham: string | null
  anhSanPham: string | null
  thuongHieu: string | null
  mauSac: string | null
  size: string | null
  soLuong: number | null
  giaBan: number | null
  thanhTien: number | null
  tongTien: number | null
  
  // Thông tin khách hàng
  tenKhachHang: string | null
  sdtKH: string | null
  email: string | null
  diaChi: string | null
  tenKhachHang2: string | null
  sdtKH2: string | null
  email2: string | null
  diaChi2: string | null
  
  // Thông tin voucher
  maVoucher: string | null
  tenVoucher: string | null
  
  // Lịch sử trạng thái
  lichSuTrangThai: string // JSON string, cần parse
  
  // Các trường khác
  thoiGian: string | null
  duNo: number | null
  xuatSu: string | null
  hoanPhi: number | null
  thanhTienSP: number | null

    danhSachImei?: string | null;
  soLuongImei?: number | null;
  productDetailId?: string | null;
  invoiceId?: string | null;
}

// Thêm interface cho IMEI
export interface IMEIItem {
  id: string;
  code: string;
  imeiCode: string;
  status: number;
  statusText: string;
}

export const parseIMEIList = (data: any): IMEIItem[] => {
  try {
    if (!data) return [];
    
    // Trường hợp 1: data là string JSON
    if (typeof data === 'string') {
      // Xử lý string có thể bị escape
      let jsonString = data;
      if (jsonString.startsWith('"') && jsonString.endsWith('"')) {
        jsonString = jsonString.slice(1, -1);
      }
      
      // Có thể là array JSON hoặc string chứa array JSON
      if (jsonString.startsWith('[') && jsonString.endsWith(']')) {
        return JSON.parse(jsonString) as IMEIItem[];
      }
      
      // Thử parse lại
      const parsed = JSON.parse(jsonString);
      return Array.isArray(parsed) ? parsed : [];
    }
    
    // Trường hợp 2: data là array của string JSON
    if (Array.isArray(data) && data.length > 0) {
      // data[0] là string JSON chứa array IMEI
      const firstItem = data[0];
      if (typeof firstItem === 'string') {
        let jsonString = firstItem;
        if (jsonString.startsWith('"') && jsonString.endsWith('"')) {
          jsonString = jsonString.slice(1, -1);
        }
        return JSON.parse(jsonString) as IMEIItem[];
      }
    }
    
    // Trường hợp 3: data đã là array của object
    if (Array.isArray(data)) {
      return data as IMEIItem[];
    }
    
    return [];
  } catch (error) {
    console.error('Error parsing IMEI list:', error, data);
    return [];
  }
};

export interface HoaDonChiTietResponse {
  content: HoaDonChiTietItem[]
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

// ==================== API FUNCTIONS ====================

export const GetHoaDons = async (params: ParamsGetHoaDon = {}): Promise<DefaultResponse<HoaDonResponse>> => {
  try {
    const page = params.page !== undefined ? Math.max(1, params.page) : 1
    const size = params.size !== undefined ? Math.max(1, params.size) : 10
    
    const apiParams: any = {
      page: page,
      size: size,
    }
    
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
    
    console.log('📤 GetHoaDons Params:', apiParams)
    
    const res = await request({
      url: API_HOA_DON,
      method: 'GET',
      params: apiParams
    })
    
    console.log('✅ GetHoaDons Response:', res.data)
    return res.data
  } catch (error) {
    console.error('❌ Error fetching hoa dons:', error)
    
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

export const getHoaDonChiTiets = async (
  params: ParamsGetHoaDonCT
): Promise<DefaultResponse<HoaDonChiTietResponse>> => {
  try {
    const page = params.page !== undefined ? Math.max(1, params.page) : 1
    const size = params.size !== undefined ? Math.max(1, params.size) : 100

    const apiParams: any = {
      maHoaDon: params.maHoaDon,
      page,
      size,
    }

    console.log('📤 getHoaDonChiTiets Params:', apiParams)

    const res = await request({
      url: `${API_HOA_DON}/all`,
      method: 'GET',
      params: apiParams
    })

    console.log('✅ getHoaDonChiTiets Response:', res.data)
    return res.data as DefaultResponse<HoaDonChiTietResponse>
  } catch (error: any) {
    console.error('❌ Error fetching hoa don chi tiets:', error)
    
    const errorResponse: DefaultResponse<HoaDonChiTietResponse> = {
      status: "ERROR",
      data: {
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
      message: error instanceof Error ? error.message : "Lỗi không xác định",
      timestamp: new Date().toISOString(),
      success: false
    }
    
    return errorResponse
  }
}

// ==================== HELPER FUNCTIONS ====================

/**
 * Parse JSON lịch sử trạng thái từ string
 */
export const parseLichSuTrangThai = (jsonString: string): LichSuTrangThaiItem[] => {
  try {
    if (!jsonString || jsonString.trim() === '') {
      return []
    }
    
    // Kiểm tra xem có phải JSON string không
    if (jsonString.startsWith('[') && jsonString.endsWith(']')) {
      return JSON.parse(jsonString) as LichSuTrangThaiItem[]
    }
    
    // Nếu không phải JSON hợp lệ, thử parse lại
    try {
      const parsed = JSON.parse(jsonString)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  } catch (error) {
    console.error('❌ Error parsing lich su trang thai:', error)
    return []
  }
}

/**
 * Lấy thông tin tổng hợp từ danh sách chi tiết hóa đơn
 */
export interface HoaDonSummary {
  maHoaDon: string
  trangThaiHoaDon: string
  trangThaiText: string
  ngayTao: number
  loaiHoaDon: string
  tongTienSauGiam: number
  phiVanChuyen: number
  giaTriVoucher: number
  tenKhachHang: string
  sdtKH: string
  email: string
  diaChi: string
  lichSuTrangThai: LichSuTrangThaiItem[]
  thoiGianCapNhatCuoi: string | null
}

export const extractHoaDonSummary = (
  items: HoaDonChiTietItem[]
): HoaDonSummary | null => {
  if (!items || items.length === 0) {
    return null
  }

  const firstItem = items[0]
  
  return {
    maHoaDon: firstItem.maHoaDon,
    trangThaiHoaDon: firstItem.trangThaiHoaDon,
    trangThaiText: firstItem.trangThaiText,
    ngayTao: firstItem.ngayTao,
    loaiHoaDon: firstItem.loaiHoaDon,
    tongTienSauGiam: firstItem.tongTienSauGiam,
    phiVanChuyen: firstItem.phiVanChuyen || 0,
    giaTriVoucher: firstItem.giaTriVoucher || 0,
    tenKhachHang: firstItem.tenKhachHang || firstItem.tenKhachHang2 || 'Khách lẻ',
    sdtKH: firstItem.sdtKH || firstItem.sdtKH2 || '',
    email: firstItem.email || firstItem.email2 || '',
    diaChi: firstItem.diaChi || firstItem.diaChi2 || '',
    lichSuTrangThai: parseLichSuTrangThai(firstItem.lichSuTrangThai),
    thoiGianCapNhatCuoi: firstItem.thoiGianCapNhatCuoi
  }
}

/**
 * Lấy danh sách sản phẩm từ chi tiết hóa đơn
 */
export interface SanPhamTrongHoaDon {
  maHoaDonChiTiet: string
  tenSanPham: string
  anhSanPham: string
  thuongHieu: string
  mauSac: string
  size: string
  soLuong: number
  giaBan: number
  thanhTien: number
  tongTien: number
}

export const extractSanPhamList = (
  items: HoaDonChiTietItem[]
): SanPhamTrongHoaDon[] => {
  return items
    .filter(item => item.tenSanPham && item.soLuong)
    .map(item => ({
      maHoaDonChiTiet: item.maHoaDonChiTiet || '',
      tenSanPham: item.tenSanPham || '',
      anhSanPham: item.anhSanPham || '',
      thuongHieu: item.thuongHieu || '',
      mauSac: item.mauSac || '',
      size: item.size || '',
      soLuong: item.soLuong || 0,
      giaBan: item.giaBan || 0,
      thanhTien: item.thanhTien || 0,
      tongTien: item.tongTien || 0
    }))
}

// ==================== STATUS MANAGEMENT ====================

export interface ADChangeStatusRequest {
  maHoaDon: string
  statusTrangThaiHoaDon: number // Số ordinal (0, 1, 2...)
  note?: string
  idNhanVien?: string // Có thể để null
}

export interface ADChangeStatusResponse {
  maHoaDon: string
  trangThaiCu: string
  trangThaiMoi: string
  ngayCapNhat: number
  thoiGianCapNhat: string
  idNhanVien: string
  paymentDate?: number
  ghiChu?: string
}

export const changeOrderStatus = async (
  requestData: ADChangeStatusRequest
): Promise<DefaultResponse<ADChangeStatusResponse>> => {
  try {
    console.log('📤 Gửi request cập nhật trạng thái:', requestData)
    
    const res = await request({
      url: `${API_HOA_DON}/change-status`,
      method: 'PUT',
      data: requestData,
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    console.log('✅ Response cập nhật trạng thái:', res.data)
    return res.data
  } catch (error: any) {
    console.error('❌ Lỗi khi cập nhật trạng thái:', error)
    
    const errorResponse: DefaultResponse<ADChangeStatusResponse> = {
      status: "ERROR",
      data: null as any,
      message: error instanceof Error ? error.message : "Lỗi không xác định",
      timestamp: new Date().toISOString(),
      success: false
    }
    
    return errorResponse
  }
}

// ==================== STATUS MAPPING ====================

export const TrangThaiHoaDonMap = {
  'CHO_XAC_NHAN': 0,
  'DA_XAC_NHAN': 1,
  'CHO_GIAO': 2,
  'DANG_GIAO': 3,
  'HOAN_THANH': 4,
  'DA_HUY': 5
} as const

export type EntityTrangThaiHoaDon = keyof typeof TrangThaiHoaDonMap

export function convertTrangThaiToNumber(trangThai: string): number {
  return TrangThaiHoaDonMap[trangThai as EntityTrangThaiHoaDon] ?? 0
}

export function convertNumberToTrangThai(num: number): EntityTrangThaiHoaDon {
  const entries = Object.entries(TrangThaiHoaDonMap)
  const entry = entries.find(([_, value]) => value === num)
  return entry?.[0] as EntityTrangThaiHoaDon ?? 'CHO_XAC_NHAN'
}

export function getTrangThaiText(trangThai: string | number): string {
  const statusMap: Record<number, string> = {
    0: 'Chờ xác nhận',
    1: 'Đã xác nhận',
    2: 'Chờ giao hàng',
    3: 'Đang giao hàng',
    4: 'Hoàn thành',
    5: 'Đã hủy'
  }
  
  if (typeof trangThai === 'string') {
    const num = parseInt(trangThai)
    return statusMap[num] || 'Không xác định'
  }
  
  return statusMap[trangThai] || 'Không xác định'
}

// export function updateTrangThaiHoaDon()