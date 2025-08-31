import { DefaultResponse, PaginationParams, PaginationResponse } from '@/api/api.common'
import request from '@/api/request'
import { API_ADMIN_PRODUCT_DETAIL } from '@/constants/url'
import { AxiosResponse } from 'axios'

export type ADProductDetailRequest = PaginationParams & {
  idProduct: string,
  idCPU: string
  idGPU: string
  idColor: string
  idRAM: string
  idHardDrive: string
  idMaterial: string
}

export type ADProductDetailCreateUpdateRequest = {
  id?: string
  code: string
  name: string
  idProduct: string
  idCPU: string
  idGPU: string
  idColor: Array<string>
  idRAM: string
  idHardDrive: string
  idMaterial: string
}

export type ADProductDetailResponse = {
  code: string
  product: string
  imei: string
  color: string
  ram: string
  hardDrive: string
  material: string
  cpu: string
  gpu: string
  price: number
  description: string
}

export type ADProductDetailDetailResponse = {
  id?: string
  code: string
  name: string
  idCPU: string
  idGPU: string
  idColor: Array<string>
  idRAM: string
  idHardDrive: string
  idMaterial: string
  price: number
  description: string
}

export type ADPRPropertiesComboboxResponse = {
  label: string
  value: string
}

export const getProductDetails = async (params: ADProductDetailRequest) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductDetailResponse>>>>

  return res.data
}

export const getColors = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/colors`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getCPUs = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/cpus`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getGPUs = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/gpus`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getHardDrives = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/hard-drives`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getRAMs = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/rams`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getMaterials = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/materials`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getProductDetailById = async (id: string) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<ADProductDetailDetailResponse>>

  return res.data
}

export const modifyProductDetail = async (data: ADProductDetailCreateUpdateRequest) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<ADProductDetailResponse>>

  return res.data
}
