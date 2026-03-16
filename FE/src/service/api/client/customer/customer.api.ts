import { API_CUSTOMER_CARTS, API_CUSTOMER_CUSTOMERS } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'

export type ClientCustomerUpdateInformation = {
  fullName: string
  phoneNumber: string
  email: string
}

export const getCartByCustomer = async (customerId: string) => {
    const res = (await request({
        url: `${API_CUSTOMER_CARTS}/${customerId}`,
        method: 'GET',
    })) as AxiosResponse<DefaultResponse<string>>
    return res.data
}

export const postInformation = async (data: ClientCustomerUpdateInformation) => {
    const res = (await request({
        url: `${API_CUSTOMER_CUSTOMERS}`,
        method: 'POST',
        data: data
    })) as AxiosResponse<DefaultResponse<string>>
    return res.data
}