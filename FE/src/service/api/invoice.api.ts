import { API_CUSTOMER_INVOICE } from "@/constants/url"
import request from "@/service/request"
import { DefaultResponse } from "@/typings/api/api.common"
import { AxiosResponse } from "axios"

export interface ClientInvoiceDetailResponse {
    id: string
    nameReceiver: string
    email: string
    addressReceiver: string
    phoneReceiver: string
    description: string
    code: string
    invoiceStatus: number
    totalAmountAfterDecrease: number
    totalAmount: number
    createDate: number
    shippingFee: number
    typePayment: string
    trangThaiThanhToan: string
}

// converted from Java interface ClientInvoiceDetailsResponse
export interface ClientInvoiceDetailsResponse {
    id: string
    code: string
    price: number
    quantity: number
    totalAmount: number
    nameProductDetail: string
    urlImage: string
    color: string
    cpu: string
    gpu: string
    hardDrive: string
    material: string
    product: string
    ram: string
}

export interface LichSuTrangThaiHoaDonResponse {
    id: string
    note: string
    trangThai: number
    thoiGian: string
}

// API call to fetch invoice by id
export const getInvoiceById = async (code: string): Promise<any> => {
    const res = (await request({
        url: `${API_CUSTOMER_INVOICE}/${code}`,
        method: 'GET',
    })) as AxiosResponse<DefaultResponse<ClientInvoiceDetailResponse>>

    return res.data
}

export const getHistoryStatusInvoice = async (id: string): Promise<DefaultResponse<Array<LichSuTrangThaiHoaDonResponse>>> => {
    const res = (await request({
        url: `${API_CUSTOMER_INVOICE}/${id}/histories`,
        method: 'GET',
    })) as AxiosResponse<DefaultResponse<Array<LichSuTrangThaiHoaDonResponse>>>

    return res.data
}

// fetch detailed items of invoice
export const getInvoiceDetails = async (id: string): Promise<DefaultResponse<ClientInvoiceDetailsResponse[]>> => {
    const res = (await request({
        url: `${API_CUSTOMER_INVOICE}/${id}/invoices-detail`,
        method: 'GET',
    })) as AxiosResponse<DefaultResponse<ClientInvoiceDetailsResponse[]>>

    return res.data
}