import { DefaultResponse, PaginationParams, PaginationResponse } from '@/api/api.common'
import request from '@/api/request'
import { API_ADMIN_PRODUCT_SCREEN } from '@/constants/url'
import { AxiosResponse } from 'axios'

export type ADProductScreenRequest = PaginationParams & {
  physicalSize: number
  resolution: string
  panelType: string
  technology: string
}

export type ADProductScreenCreateUpdateRequest = {
  id?: string
  code: string
  name: string
  physicalSize: number
  resolution: string
  panelType: string
  technology: string
}

export type ADProductScreenResponse = {
  id?: string
  code: string
  name: string
  physicalSize: number
  resolution: string
  panelType: string
  technology: string
}

export type ADProductScreenDetailResponse = {
  id?: string
  code: string
  name: string
  physicalSize: number
  resolution: string
  panelType: string
  technology: string
}

export type ADProductScreenResolutionResponse = {
  id: string
  code: string
  name: string
}

export const getScreens = async (params: ADProductScreenRequest) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_SCREEN}`,
    method: 'GET',
    params,
  })) as AxiosResponse<DefaultResponse<PaginationResponse<Array<ADProductScreenResponse>>>>

  return res.data
}

export const getScreenById = async (id: string) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_SCREEN}/${id}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<ADProductScreenDetailResponse>>

  return res.data
}

export const modifyScreen = async (data: ADProductScreenCreateUpdateRequest) => {
  const res = (await request({
    url: `${API_ADMIN_PRODUCT_SCREEN}`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<ADProductScreenResponse>>

  return res.data
}
