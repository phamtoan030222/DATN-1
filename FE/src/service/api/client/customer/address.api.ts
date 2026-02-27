import { API_CUSTOMER_CUSTOMER_ADDRESSES } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'

export interface CustomerAddress {
  id: string
  provinceCity: string
  district: string
  wardCommune: string
  addressDetail: string
  isDefault: boolean
}

export async function getDefaultCustomerAddress (customerId: string) {
  const res = (await request({
    url: `${API_CUSTOMER_CUSTOMER_ADDRESSES(customerId)}/default`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<CustomerAddress | null>>

  return res.data
}
