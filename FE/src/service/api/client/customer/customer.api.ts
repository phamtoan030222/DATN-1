import { API_CUSTOMER_CARTS } from "@/constants/url"
import request from "@/service/request"
import { DefaultResponse } from "@/typings/api/api.common"
import { AxiosResponse } from "axios"

export const getCartByCustomer = async (customerId: string) => {
    const res = (await request({
        url: `${API_CUSTOMER_CARTS}/${customerId}`,
        method: 'GET',
    })) as AxiosResponse<DefaultResponse<String>>
    return res.data
}