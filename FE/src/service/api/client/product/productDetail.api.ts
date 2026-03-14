import { API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse, PaginationParams, PaginationResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'
import type { SelectMixedOption } from 'naive-ui/es/select/src/interface'

export type ADProductDetailRequest = PaginationParams & {
  readonly q?: string | null // Thêm trường tìm kiếm
  readonly idBrand?: string | null // Thêm trường hãng
  readonly idScreen?: string | null // Thêm trường màn hình
  readonly idProduct?: string | null
  readonly idCPU?: string | null
  readonly idGPU?: string | null
  readonly idColor?: string | null
  readonly idRAM?: string | null
  readonly idHardDrive?: string | null
  readonly idMaterial?: string | null
  readonly minPrice: number
  readonly maxPrice: number
}

export interface ADProductDetailResponse {
  readonly id: string
  readonly code: string
  readonly name: string
  readonly quantity: number
  readonly color: string
  readonly ram: string
  readonly hardDrive: string
  readonly material: string
  readonly cpu: string
  readonly gpu: string
  readonly price: number
  readonly description: string
  readonly status: string
  readonly urlImage: string
  readonly percentage?: number
  readonly productName: string
  readonly screenName?: string
  readonly brandName?: string
  readonly batteryName?: string
  readonly operatingSystemName?: string
  readonly endDate?: number
  readonly startDate?: number
}

export interface ADProductDetailDetailResponse {
  readonly id?: string
  readonly code: string
  readonly name: string
  readonly idCPU: string
  readonly idGPU: string
  readonly idColor: string
  readonly idRAM: string
  readonly idHardDrive: string
  readonly idMaterial: string
  readonly price: number
  readonly percentage?: number
  readonly cpuName?: string
  readonly ramName?: string
  readonly hardDriveName?: string
  readonly screenName?: string
  readonly colorName?: string
  readonly productName?: string
  readonly operatingName?: string
  readonly brandName?: string
  readonly batteryName?: string
  readonly gpuName?: string
  readonly endDate?: number
}

export type ADPRPropertiesComboboxResponse = Readonly<SelectMixedOption> & {
  readonly label: string
  readonly value: string
}

export interface ADPDImeiResponse {
  readonly id: string
  readonly code: string
  readonly name: string
  readonly status: string
  readonly imeiStatus: string
}

export interface IMEIExcelResponse {
  readonly imei: string
  readonly isExist: boolean
}

// Dùng chung PaginationParams hoặc tạo type riêng nếu SanPhamChiTietGiamGiaRepuest có thêm field khác
export type DiscountProductRequest = PaginationParams & {
  // Nếu request có tìm kiếm hay filter gì thêm thì khai báo ở đây
  // readonly q?: string
}

// Interface hứng dữ liệu từ ClientDiscountProductProjection của Backend
export interface DiscountProductResponse {
  readonly discountName: string
  readonly startDate: number
  readonly endDate: number
  readonly percentage: number
  readonly name: string
  readonly productDetailId: string
  readonly cpu?: string
  readonly gpu?: string
  readonly ram?: string
  readonly hardDrive?: string
  readonly originalPrice: number
  readonly salePrice: number
  readonly urlImage: string
}

export async function getProductDetails(params: ADProductDetailRequest) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductDetailResponse>>>>

  return res.data
}

export async function getColors() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/colors`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getCPUs() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/cpus`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getGPUs() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/gpus`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getHardDrives() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/hard-drives`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getRAMs() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/rams`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getMaterials() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/materials`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getProductDetailById(id: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<ADProductDetailDetailResponse>>

  return res.data
}

export async function changeProductDetailStatus(id: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/change-status/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}

export async function downloadTemplateImei() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/imei/download-template`,
    method: 'GET',
    responseType: 'blob',
  })) as AxiosResponse<Blob>

  return res
}

export async function importIMEIExcel(file: any) {
  const formData = new FormData()
  formData.append('file', file.file)

  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/imei/import`,
    method: 'POST',
    data: formData,
  })) as AxiosResponse<DefaultResponse<Array<IMEIExcelResponse>>>

  return res.data
}

export async function checkIMEIExist(ids: Array<string>) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/imei-exists`,
    method: 'POST',
    data: ids,
  })) as AxiosResponse<DefaultResponse<Array<string>>>

  return res.data
}

export async function getMinMaxPrice() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/min-max-price`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<{ priceMin: number, priceMax: number }>>

  return res.data
}

export async function getImeiProductDetail(idProductDetail: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/imei/${idProductDetail}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPDImeiResponse>>>

  return res.data
}

export async function checkExistVariant(productId: string, listPropertiesVariant: Array<{
  idColor: string
  idRAM: string
  idHardDrive: string
  idMaterial: string
  idCPU: string
  idGPU: string
}>) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/exist-variant`,
    method: 'POST',
    data: {
      productId,
      listPropertiesVariant,
    },
    headers: {
      'Content-Type': 'application/json',
    },
  })) as AxiosResponse<DefaultResponse<Array<boolean>>>

  return res.data
}

// Lấy sản phẩm theo đợt giảm giá
export async function getOngoingDiscounts(params: DiscountProductRequest) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/a/ongoing`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<DiscountProductResponse>>>>

  return res.data
}

export async function getNearestUpcomingDiscounts(params: DiscountProductRequest) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/a/upcoming`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<DiscountProductResponse>>>>

  return res.data
}

export async function getColorsByPD(pdId: string) {
  const res = (await request({
    // Đảm bảo URL này khớp với @GetMapping("/{id}/colors") ở Controller Backend
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/${pdId}/colors`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getCpuByPD(pdId: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/${pdId}/cpus`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getGpuByPD(pdId: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/${pdId}/gpus`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getRamByPD(pdId: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/${pdId}/rams`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getHardDriveByPD(pdId: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/${pdId}/hardDrives`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

// 1. Sửa lại Interface cho khớp 100% với class LayThuocTinhRequest trong Java
export interface RequestGetProductDetails {
  readonly idProduct: string
  readonly idColor?: string | null
  readonly idRam?: string | null
  readonly idHardDrive?: string | null
  readonly idCpu?: string | null
  readonly idGpu?: string | null
}

// 2. Sửa lại hàm gọi API để trả về đúng 1 Object (ADProductDetailDetailResponse)
export async function getSPCTBy(params: RequestGetProductDetails) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL}/variant`,
    method: 'GET',
    params, // Truyền thẳng params xuống Axios
  })) as AxiosResponse<DefaultResponse<ADProductDetailDetailResponse>>

  return res.data
}
