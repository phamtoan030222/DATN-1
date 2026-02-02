import { API_PREFIX_ORDER_ONLINE_PRODUCT } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse, PaginationParams, PaginationResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'
import type { SelectMixedOption } from 'naive-ui/es/select/src/interface'

export type ADProductRequest = PaginationParams & {
  idBattery: string
  idBrand: string
  idScreen: string
  idOperatingSystem: string
  minPrice: number
  maxPrice: number
}

export interface ADProductResponse {
  id?: string
  code: string
  name: string
  status?: string
  brand: string
  battery: string
  screen: string
  operatingSystem: string
  minPrice: number
  maxPrice: number
  quantity: number
  urlImage: string
  percentage?: number
}

export interface ADProductDetailResponse {
  id?: string
  code: string
  name: string
  idBrand: string
  idBattery: string
  idScreen: string
  idOperatingSystem: string
}

export type ADPRPropertiesComboboxResponse = SelectMixedOption & {
  readonly label: string
  readonly value: string
  readonly code?: string
}

export async function getProducts(params: ADProductRequest) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductResponse>>>>

  return res.data
}

export async function getScreens() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}/screens`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getBatteries() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}/batteries`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getBrands() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}/brands`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getOperatingSystems() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}/operating-systems`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export async function getProductById(id: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<ADProductDetailResponse>>

  return res.data
}

export async function modifyProduct(data: ADProductCreateUpdateRequest, images: any) {
  const formData = new FormData()
  formData.append('request', new Blob([JSON.stringify(data)], { type: 'application/json' }))
  if (Array.isArray(images)) {
    images.forEach(image => formData.append('images', image))
  }
  else {
    formData.append('images', images)
  }

  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}`,
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })) as AxiosResponse<DefaultResponse<string>>

  return res.data
}

export async function changeProductStatus(id: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}/change-status/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}

export async function uploadImages(data: FormData, id: string) {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}/upload-images/${id}`,
    method: 'POST',
    data,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}

export async function getProductsCombobox() {
  const res = (await request({
    url: `${API_PREFIX_ORDER_ONLINE_PRODUCT}/combobox`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}
