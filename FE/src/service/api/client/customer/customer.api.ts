import { API_CUSTOMER_CARTS, API_CUSTOMER_CUSTOMERS } from '@/constants/url'
import request from '@/service/request'
import type { DefaultResponse } from '@/typings/api/api.common'
import type { AxiosResponse } from 'axios'

// ------------------------------------------------
// 1. CÁC INTERFACE (Kiểu dữ liệu)
// ------------------------------------------------

export interface ClientCustomerUpdateInformation {
  fullName: string
  phoneNumber: string
  email: string
  avatarUrl?: string
}

export interface ClientChangePasswordRequest {
  matKhauCu: string
  matKhauMoi: string
}

export interface ForgotPasswordRequest {
  email: string
}

// ------------------------------------------------
// 2. CÁC HÀM GỌI API
// ------------------------------------------------

export async function getCartByCustomer(customerId: string) {
  const res = (await request({
    url: `${API_CUSTOMER_CARTS}/${customerId}`,
    method: 'GET',
  })) as AxiosResponse<DefaultResponse<string>>
  return res.data
}

export async function postInformation(data: ClientCustomerUpdateInformation) {
  const res = (await request({
    url: `${API_CUSTOMER_CUSTOMERS}`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<string>>
  return res.data
}

// ==========================================
// API ĐỔI MẬT KHẨU (Dành cho user đã đăng nhập)
// ==========================================
export async function changeCustomerPassword(data: ClientChangePasswordRequest) {
  const res = (await request({
    // Nối thêm /change-password vào đuôi URL mặc định của Customer
    url: `${API_CUSTOMER_CUSTOMERS}/change-password`,
    method: 'PUT',
    data,
  })) as AxiosResponse<DefaultResponse<string>>
  return res.data
}

// ==========================================
// API QUÊN MẬT KHẨU (Dành cho màn hình Đăng nhập)
// ==========================================
export async function forgotCustomerPassword(data: ForgotPasswordRequest) {
  const res = (await request({
    // Nối thêm /forgot-password vào đuôi URL mặc định của Customer
    url: `${API_CUSTOMER_CUSTOMERS}/forgot-password`,
    method: 'POST',
    data,
  })) as AxiosResponse<DefaultResponse<string>>
  return res.data
}
