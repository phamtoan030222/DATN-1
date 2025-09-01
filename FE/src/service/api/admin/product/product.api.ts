import { API_ADMIN_PRODUCTS } from '@/constants/url'
import request from '@/service/request'
import { DefaultResponse, PaginationParams, PaginationResponse } from '@/typings/api/api.common'
import { AxiosResponse } from 'axios'

export type ADProductRequest = PaginationParams & {
  idBattery: string
  idBrand: string
  idScreen: string
  idOperatingSystem: string
  minPrice: number
  maxPrice: number
}

export type ADProductCreateUpdateRequest = {
  id?: string
  code: string
  name: string
  idBrand: string
  idBattery: string
  idScreen: string
  idOperatingSystem: string
}

export type ADProductResponse = {
  id?: string
  code: string
  name: string
  status?: string
  band: string
  battery: string
  screen: string
  operatingSystem: string
  minPrice: number,
  maxPrice: number,
  quantity: number
}

export type ADProductDetailResponse = {
  id?: string
  code: string
  name: string
  idBrand: string
  idBattery: string
  idScreen: string
  idOperatingSystem: string
}

export type ADPRPropertiesComboboxResponse = {
  label: string
  value: string
}

export const getProducts = async (params: ADProductRequest) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCTS}`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductResponse>>>>

  return res.data
}

export const getScreens = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCTS}/screens`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getBatteries = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCTS}/batteries`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getBrands = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCTS}/brands`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getOperatingSystems = async () => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCTS}/operating-systems`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<Array<ADPRPropertiesComboboxResponse>>>

  return res.data
}

export const getProductById = async (id: string) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCTS}/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<ADProductDetailResponse>>

  return res.data
}

export const modifyProduct = async (data: ADProductCreateUpdateRequest) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCTS}`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<ADProductResponse>>

  return res.data
}

export const changeProductStatus = async (id: string) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCTS}/change-status/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<null>>

  return res.data
}