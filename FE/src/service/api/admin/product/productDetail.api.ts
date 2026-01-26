import type { ProductPropertiesType } from '@/constants/ProductPropertiesType'
import { API_ADMIN_PRODUCT_DETAIL } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse, PaginationParams, PaginationResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'
import type { SelectMixedOption } from 'naive-ui/es/select/src/interface'

export type ADProductDetailRequest = PaginationParams & {
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

export interface ADProductDetailCreateUpdateRequest {
  readonly id?: string
  readonly code?: string
  readonly idProduct?: string
  readonly imei: readonly string[]
  readonly idColor: string
  readonly idRAM: string
  readonly idHardDrive: string
  readonly idMaterial: string
  readonly idGPU: string
  readonly idCPU: string
  readonly price: number
  readonly description?: string
  readonly publicId?: string
  readonly urlImage?: string
}

export interface ADProductDetailResponse {
  readonly id: string
  readonly code: string
  readonly name: string
  readonly product: string
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

export async function createProductVariant(
  idProduct: string,
  variant: ADProductDetailCreateUpdateRequest,
): Promise<DefaultResponse<string>> {
  try {
    const formData = new FormData()

    formData.append(
      'variant',
      new Blob([JSON.stringify(variant)], { type: 'application/json' }),
    )

    formData.append(
      'idProduct',
      new Blob([idProduct], { type: 'application/json' }),
    )

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
  catch (error: any) {
    const backendMessage
      = error?.response?.data?.message
        || 'Có lỗi xảy ra'

    throw new Error(backendMessage)
  }
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

export async function quickAddProperties(nameProperty: string, type: ProductPropertiesType, hex?: string) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/quick-add`,
    method: 'POST',
    data: {
      nameProperty,
      type,
      hex,
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

export async function checkExistVariant(productId: string, listPropertiesVariant: Array<{
  idColor: string
  idRAM: string
  idHardDrive: string
  idMaterial: string
  idCPU: string
  idGPU: string
}>) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/exist-variant`,
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

export async function addSerialNumberToProductDetail(idProductDetail: string, serialNumbers: Array<string>) {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/add-serial-number`,
    method: 'POST',
    data: {
      idProductDetail,
      serialNumbers,
    },
    headers: {
      'Content-Type': 'application/json',
    },
  })) as AxiosResponse<DefaultResponse<Array<boolean>>>

  return res.data
}
