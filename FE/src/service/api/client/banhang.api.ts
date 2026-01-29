import type { AxiosResponse } from 'axios'
import request from '@/service/request'
import { API_CUSTOMER_PRODUCT_DETAIL, API_O, API_ORDER_ONLINE, API_ORDER_ONLINERDER_ONLINE } from '@/constants/url'
import type {
  DefaultResponse,
  PaginationParams,
  PaginationResponse,
  ResponseList,
} from '@/service/api.common'
import type { ADPDImeiResponse, ADProductDetailRequest } from './product/productDetail.api'
import type { ADProductDetailResponse } from './product/product.api'

export interface ParamsGetSanPham extends PaginationParams {
  q?: string | ''
  idSP?: string | undefined
  status?: number | null
}

export interface ParamsPhieuGiamGia {
  invoiceId: string // ID hóa đơn
  tongTien: number // Tổng tiền hiện tại
  customerId?: string | null // Có thể null (bán tại quầy)
}

export interface VoucherApDungDTO {
  voucherId: string
  code: string
  typeVoucher: 'PERCENTAGE' | 'FIXED_AMOUNT'
  discountValue: number
  maxValue?: number
  dieuKien: number
  giamGiaThucTe: number // TÊN FIELD ĐÃ THAY ĐỔI: giamGiaThucTe thay vì giaTriGiamThucTe
  ten?: string // Có thể không có
}

export interface VoucherTotHonDTO {
  voucherId: string
  code: string
  dieuKien: number
  giamGiaMoi: number
  canMuaThem: number
  giamThem: number
  hieuQua: number
}

export interface GoiYVoucherResponse {
  voucherApDung: VoucherApDungDTO[]
  voucherTotHon?: VoucherTotHonDTO[]
  voucherHienTai?: VoucherApDungDTO // Có thể không có
  tongTienSauGiam?: number
  // Thêm các field khác nếu có
}

export interface ParamsGetHoaDon extends PaginationParams {
  q?: string | ''
  status?: number | null
}

export interface ParamsXoaSP {
  idHD?: string
  idSP?: string
}

export interface ParamsPTTT {
  idHD?: string
  pttt: string
}

export interface ParamsThanhCong {
  // Các field hiện tại (giữ nguyên)
  idHD?: string
  tongTien: string

  // Thêm các field mới để khớp với backend
  idNV?: string | null
  tienHang?: number
  tienShip?: number
  giamGia?: number
  ten?: string
  sdt?: string
  diaChi?: string
  phuongThucThanhToan?: string
  idPGG?: string
  check?: number
  isDeliveryEnabled?: boolean

  // THÊM CÁC FIELD IMEI (QUAN TRỌNG)
  loaiHoaDon?: 'TAI_QUAY' | 'GIAO_HANG'
  danhSachImei?: Array<{
    idHoaDonChiTiet: string
    danhSachImei: string[]
  }>
  daXacNhanImei?: boolean
}

// NEW: Type for delivery information
export type ThongTinGiaoHangResponse = ResponseList & {
  id: string
  hoTenNguoiNhan: string
  sdtNguoiNhan: string
  diaChiGiaoHang: string
  ghiChu?: string
  // Add other relevant fields for delivery information
  // Example:
  // maHoaDon?: string;
  // trangThaiGiaoHang?: string;
}

// NEW: Request payload for adding/updating delivery information
export interface ThemThongTinGiaoHangRequest {
  idHD: string // Assuming delivery info is tied to a specific invoice
  hoTenNguoiNhan: string
  sdtNguoiNhan: string
  diaChiGiaoHang: string
  ghiChu?: string
}

export type KhachHangResponse = ResponseList & {
  id: string
  ten: string
  sdt: string
}

export type thanhToanResponse = ResponseList & {
  id: string
  ten: string
  sdt: string
}

export type PhuongThucThanhToanResponse = ResponseList & {
  id: string
  soTien: number
  phuongThucThanhToan: string
}

export type tongTienResponse = ResponseList & {
  tongTien: number
}

export interface ADThemSanPhamRequest {
  invoiceId: string
  productDetailId: string
  imeiIds: string[]
}

export type PhieuGiamGiaResponse = ResponseList & {
  id: string
  ma: string
  giaTriGiam: number // Giá trị giảm (tiền hoặc %)
  laPhanTram: boolean // true nếu là %, false nếu là tiền
  giaTriGiamThucTe: number // Giá trị giảm thực tế (đã tính dựa trên tongTien)
}

export interface PhieuGiamGiaRequest {
  idHD: number
  idKH: string
}

export type BanHangResponse = ResponseList & {
  ma: string
  ten: string
  status: string
}

export interface themKHResponse {
  idHD: string
  idKH: string
}

export interface XoaSPResponse {
  idHD: string
  idHDCT: string
}

export type SanPhamResponse = ResponseList & {
  id: string
  ma: string
  ten: string
  moTa: string
  mau: string
  idSP: string
  idCL: string
  idLG: string
  idLD: string
  idXX: string
  idMau: string
  idSize: string
  status: string
}

export async function GetHoaDons(params: ParamsGetHoaDon) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/list-hoa-don`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<BanHangResponse>>>>

  return res.data
}

export interface CreateHoaDonRequest {
  ma?: string // FE gửi mã TẠM (BE có thể bỏ qua)
}

export async function getProductDetails(params: ADProductDetailRequest) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/san-pham-chi-tiet`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductDetailResponse>>>>

  return res.data
}

export async function getImeiProductDetail(idProductDetail: string) {
  const res = (await request({
    url: `${API_CUSTOMER_PRODUCT_DETAIL}/imei/${idProductDetail}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPDImeiResponse>>>

  return res.data
}

export async function getCreateHoaDon(maHoaDon: CreateHoaDonRequest) {
  const res = await request({
    url: `${API_ORDER_ONLINE}/create-hoa-don`,
    method: 'POST',
    data: maHoaDon,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  return res.data as DefaultResponse<BanHangResponse>
}
export async function huyHoaDon(maHoaDon: ADThemSanPhamRequest) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/huy`,
    method: 'POST',
    data: maHoaDon,
  })) as AxiosResponse<DefaultResponse<BanHangResponse>>
  return res.data
}

export async function themSanPham(data: ADThemSanPhamRequest) {
  const res = await request({
    url: `${API_ORDER_ONLINE}/them-san-pham`,
    method: 'POST',
    data,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  return res.data
}

export async function GetGioHang(id: string) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/list-gio-hang/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<BanHangResponse>>>>

  return res.data
}

export async function themSL(data: ParamsXoaSP) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/them-so-luong `,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<XoaSPResponse>>

  return res.data
}

export async function xoaSL(data: ParamsXoaSP) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/xoa-so-luong `,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<XoaSPResponse>>
  return res.data // Added return statement
}

export async function GeOneKhachHang(id: string) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/khach-hang/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<KhachHangResponse>>>>

  return res.data
}

export async function getThanhToan(id: string) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/thanh-toan/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<thanhToanResponse>>>>

  return res.data
}

export async function getPhuongThucThanhToan(id: string) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/phuong-thuc-thanh-toan/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<thanhToanResponse>>>>

  return res.data
}

export async function themPTTT(data: ParamsPTTT) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/cap-nhat-phuong-thuc-thanh-toan`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<XoaSPResponse>>
  return res.data // Added return statement
}

export async function thanhToanThanhCong(data: ParamsThanhCong) {
  // Sửa để gửi JSON thay vì FormData
  const res = (await request({
    url: `${API_ORDER_ONLINE}/thanh-toan-thanh-cong`,
    method: 'POST',
    data, // Gửi trực tiếp object JSON
    headers: {
      'Content-Type': 'application/json', // Quan trọng
    },
  })) as AxiosResponse<DefaultResponse<any>>

  return res.data
}

export async function themMoiKhachHang(data: themKHResponse) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/them-moi-khach-hang`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<KhachHangResponse>>>>

  return res.data
}

export async function themKhachHang(data: themKHResponse) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/them-khach-hang`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<KhachHangResponse>>>>

  return res.data
}

export async function GetSanPhams(params: ParamsGetSanPham) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/list-san-pham`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<SanPhamResponse>>>>

  return res.data
}

export async function GetKhachHang(data: ParamsXoaSP) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/list-khach-hang`,
    method: 'GET',
    params: data,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<KhachHangResponse>>>>

  return res.data
}

export async function getMaGiamGia(data: ParamsPhieuGiamGia): Promise<GoiYVoucherResponse> {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/goi-y`,
    method: 'POST',
    data,
    headers: {
      'Content-Type': 'application/json',
    },
  })) as AxiosResponse<GoiYVoucherResponse>

  return res.data
}

export async function getMaGiamGiaKoDu(data: ParamsPhieuGiamGia) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/danh-sach-phieu-giam-gia-ko_du`,
    method: 'GET',
    params: data,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<PhieuGiamGiaResponse>>>>

  return res.data
}

export async function suaGiaoHang(id: string) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/giao-hang/${id}`,
    method: 'POST',
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<PhieuGiamGiaResponse>>>>

  return res.data
}
// NEW: API call to add/update delivery information

export async function xoaSP(data: ParamsXoaSP) {
  const res = (await request({
    url: `${API_ORDER_ONLINE}/delete-sp`, // Đã xóa dấu cách thừa
    method: 'DELETE',
    params: { idHDCT: data.idSP }, // Backend nhận @RequestParam("idSP")
  })) as AxiosResponse<DefaultResponse<XoaSPResponse>>
  return res.data
}

// API Tăng số lượng
export function tangSoLuong(idHDCT: string) {
  return request.post(`${API_ORDER_ONLINE}/tang-so-luong?idHDCT=${idHDCT}`)
}

// API Giảm số lượng
export function giamSoLuong(idHDCT: string) {
  return request.post(`${API_ORDER_ONLINE}/giam-so-luong?idHDCT=${idHDCT}`)
}
