import { DefaultResponse, PaginationParams, PaginationResponse } from '@/api/api.common'
import request from '@/api/request'
import { API_ADMIN_PRODUCTS } from '@/constants/url'
import { AxiosResponse } from 'axios'

export type ADProductRequest = PaginationParams & {
  idBattery: string
  idBrand: string
  idScreen: string
  idOperatingSystem: string
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
  idBrand: string
  idBattery: string
  idScreen: string
  idOperatingSystem: string
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
