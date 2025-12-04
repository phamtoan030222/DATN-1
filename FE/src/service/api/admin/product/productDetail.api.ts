import { API_ADMIN_PRODUCT_DETAIL } from '@/constants/url'
import request from '@/service/request'
import { DefaultResponse, PaginationParams, PaginationResponse } from '@/typings/api/api.common'
import { AxiosResponse } from 'axios'

export type ADProductDetailRequest = PaginationParams & {
  idProduct?: string,
  idCPU?: string
  idGPU?: string
  idColor?: string
  idRAM?: string
  idHardDrive?: string
  idMaterial?: string
  minPrice: number,
  maxPrice: number
}

export type ADProductDetailCreateUpdateRequest = {
  id?: string;
  code?: string;
  idProduct?: string;
  imei: string[];
  idColor: string;
  idRAM: string;
  idHardDrive: string;
  idMaterial: string;
  idGPU: string;
  idCPU: string;
  price: number;
  description?: string;
}

export type ADProductDetailResponse = {
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
  urlImage: string,
}

export type ADProductDetailDetailResponse = {
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

export type ADPRPropertiesComboboxResponse = {
  label: string
  value: string
}

export type IMEIExcelResponse = {
  readonly imei: string;
  readonly isExist: boolean;
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

export const createProductVariant = async (idProduct: string, variant: ADProductDetailCreateUpdateRequest, images: any[]) => {

  const formData = new FormData();
  formData.append('variant', new Blob([JSON.stringify(variant)], { type: 'application/json' }));
  formData.append('idProduct', new Blob([idProduct], { type: 'application/json' }));
  images.forEach(image => formData.append('images', image))

  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/variant`,
    method: 'POST',
    data: formData,
    headers: {
      "Content-Type": "multipart/form-data"
    }
  })) as AxiosResponse<DefaultResponse<string>>

  return res.data
}

export const changeProductDetailStatus = async (id: string) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/change-status/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}

export const downloadTemplateImei = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/imei/download-template`,
    method: 'GET',
    responseType: 'blob'
  })) as AxiosResponse<Blob>

  return res;
}

export const importIMEIExcel = async (file: any) => {
  const formData = new FormData();
  formData.append('file', file.file);

  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/imei/import`,
    method: 'POST',
    data: formData
  })) as AxiosResponse<DefaultResponse<Array<IMEIExcelResponse>>>

  return res.data;
}

export const checkIMEIExist = async (ids: Array<string>) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_DETAIL}/imei-exists`,
    method: 'POST',
    data: ids
  })) as AxiosResponse<DefaultResponse<Array<string>>>

  return res.data
}