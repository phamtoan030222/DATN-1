import type { ProductPropertiesType } from '@/constants/ProductPropertiesType'
import { API_ADMIN_PRODUCT_DETAIL } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse, PaginationParams, PaginationResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'
import { SelectMixedOption } from 'naive-ui/es/select/src/interface'

export type ADProductDetailRequest = PaginationParams & {
  idProduct?: string | null
  idCPU?: string | null
  idGPU?: string | null
  idColor?: string | null
  idRAM?: string | null
  idHardDrive?: string | null
  idMaterial?: string | null
  minPrice: number
  maxPrice: number
}

export interface ADProductDetailCreateUpdateRequest {
  id?: string
  code?: string
  idProduct?: string
  imei: string[]
  idColor: string
  idRAM: string
  idHardDrive: string
  idMaterial: string
  idGPU: string
  idCPU: string
  price: number
  description?: string
  publicId?: string
  urlImage?: string
}

export interface ADProductDetailResponse {
  id: string
  code: string
  product: string
  quantity: number
  color: string
  ram: string
  hardDrive: string
  material: string
  cpu: string
  gpu: string
  price: number
  description: string
  status: string
  urlImage: string
}

export interface ADProductDetailDetailResponse {
  id?: string
  code: string
  name: string
  idCPU: string
  idGPU: string
  idColor: string
  idRAM: string
  idHardDrive: string
  idMaterial: string
  price: number
}

export type ADPRPropertiesComboboxResponse = SelectMixedOption & {
  label: string
  value: string
}

export interface ADPDImeiResponse {
  id: string
  code: string
  name: string
  status: string
}

export interface IMEIExcelResponse {
  readonly imei: string
  readonly isExist: boolean
}

export async function getProductDetails(params: ADProductDetailRequest) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductDetailResponse>>>>

  return res.data
}

export async function getColors() {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/colors`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getCPUs() {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/cpus`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getGPUs() {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/gpus`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getHardDrives() {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/hard-drives`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getRAMs() {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/rams`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getMaterials() {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/materials`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getProductDetailById(id: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<ADProductDetailDetailResponse>>

  return res.data
}

export async function modifyProductDetail(data: ADProductDetailCreateUpdateRequest) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<ADProductDetailResponse>>

  return res.data
}

export async function createProductVariant(idProduct: string, variant: ADProductDetailCreateUpdateRequest) {
  const formData = new FormData()
  formData.append('variant', new Blob([JSON.stringify(variant)], { type: 'application/json' }))
  formData.append('idProduct', new Blob([idProduct], { type: 'application/json' }))
  // images.forEach(image => formData.append('images', image))

  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/variant`,
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })) as AxiosResponse<DefaultResponse<string>>

  return res.data
}

export async function changeProductDetailStatus(id: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/change-status/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}

export async function downloadTemplateImei() {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/imei/download-template`,
    method: 'GET',
    responseType: 'blob',
  })) as AxiosResponse<Blob>

  return res
}

export async function importIMEIExcel(file: any) {
  const formData = new FormData()
  formData.append('file', file.file)

  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/imei/import`,
    method: 'POST',
    data: formData,
  })) as AxiosResponse<DefaultResponse<Array<IMEIExcelResponse>>>

  return res.data
}

export async function checkIMEIExist(ids: Array<string>) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/imei-exists`,
    method: 'POST',
    data: ids,
  })) as AxiosResponse<DefaultResponse<Array<string>>>

  return res.data
}

export async function quickAddProperties(nameProperty: string, type: ProductPropertiesType) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/quick-add`,
    method: 'POST',
    data: {
      nameProperty,
      type,
    },
  })) as AxiosResponse<DefaultResponse<Array<string>>>

  return res.data
}

export async function getMinMaxPrice() {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/min-max-price`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<{ priceMin: number, priceMax: number }>>

  return res.data
}

export async function getImeiProductDetail(idProductDetail: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/imei/${idProductDetail}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPDImeiResponse>>>

  return res.data
}

export async function changeStatusImei(idProductDetail: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/imei/change-status/${idProductDetail}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<string>>

  return res.data
}

export async function saveImage(file: any) {
  const formData = new FormData()

  formData.append('file', file)

  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/save-image`,
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })) as AxiosResponse<DefaultResponse<{ publicId: string, url: string }>>

  return res.data
}
