import { API_CUSTOMER_INVOICE } from "@/constants/url"
import request from "@/service/request"
import { DefaultResponse } from "@/typings/api/api.common"
import { AxiosResponse } from "axios"

// Response interface matching Java interface
export interface ClientInvoiceDetailResponse {
    id: string
    code: string
    invoiceStatus: string
    historyStatusInvoice: string
    totalAmountAfterDecrease: number
    totalQuantity: number
}

// API call to fetch invoice by id
export const getInvoiceById = async (code: string): Promise<any> => {
    const res = (await request({
        url: `${API_CUSTOMER_INVOICE}/${code}`,
        method: 'GET',
    })) as AxiosResponse<DefaultResponse<ClientInvoiceDetailResponse>>

    return res.data
}