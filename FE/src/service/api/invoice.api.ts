import { API_INVOICES_ORDER_ONLINE } from "@/constants/url"
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
    giaGoc: number
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
    idInvoice: string
    idProductDetail: string
    createdDate: number,
}

export interface LichSuTrangThaiHoaDonResponse {
    id: string
    note: string
    trangThai: number
    thoiGian: string
    idStaff: string,
    nameStaff: string,
}

export type ClientInvoiceCancelRequest = {
    id: string;
    note: string;
}

export interface ClientPutInvoiceDetailRequest {
  updateProductDetails: UpdateInvoiceDetail[];
  totalAmount: number;
}

export interface UpdateInvoiceDetail {
  idProductDetail: string;
  idInvoiceDetail: string;
  quantity: number;
  price: number;
  giaGoc: number;
  totalAmount: number;
}

// API call to fetch invoice by id
export const getInvoiceById = async (code: string): Promise<any> => {
    const res = (await request({
        url: `${API_INVOICES_ORDER_ONLINE}/${code}`,
        method: 'GET',
    })) as AxiosResponse<DefaultResponse<ClientInvoiceDetailResponse>>

    return res.data
}

export const getInvoicesByUser = async (): Promise<any> => {
    const res = (await request({
        url: `${API_INVOICES_ORDER_ONLINE}/user`,
        method: 'GET',
    })) as AxiosResponse<DefaultResponse<Array<ClientInvoiceDetailResponse>>>

    return res.data
}

export const getHistoryStatusInvoice = async (id: string): Promise<DefaultResponse<Array<LichSuTrangThaiHoaDonResponse>>> => {
    const res = (await request({
        url: `${API_INVOICES_ORDER_ONLINE}/${id}/histories`,
        method: 'GET',
    })) as AxiosResponse<DefaultResponse<Array<LichSuTrangThaiHoaDonResponse>>>

    return res.data
}

// fetch detailed items of invoice
export const getInvoiceDetails = async (ids: Array<string>): Promise<DefaultResponse<ClientInvoiceDetailsResponse[]>> => {
    const res = (await request({
        url: `${API_INVOICES_ORDER_ONLINE}/invoices-detail`,
        method: 'GET',
        params: {
            ids: ids.join(','),
        }
    })) as AxiosResponse<DefaultResponse<ClientInvoiceDetailsResponse[]>>

    return res.data
}

export const putInvoiceCancel = async (data: ClientInvoiceCancelRequest) => {
    const res = (await request({
        url: `${API_INVOICES_ORDER_ONLINE}/cancel`,
        method: 'PUT',
        data,
    })) as AxiosResponse<DefaultResponse<string>>

    return res.data 
}

export const putReceiver = async (id: string, data: any) => {
    const res = (await request({
        url: `${API_INVOICES_ORDER_ONLINE}/${id}/receiver`,
        method: 'PUT',
        data,
    })) as AxiosResponse<DefaultResponse<string>>

    return res.data 
}

export const putInvoiceDetail = async (id: string, data: ClientPutInvoiceDetailRequest) => {
    const res = (await request({
        url: `${API_INVOICES_ORDER_ONLINE}/${id}/invoices-detail`,
        method: 'PUT',
        data,
    })) as AxiosResponse<DefaultResponse<string>>

    return res.data 
}
 