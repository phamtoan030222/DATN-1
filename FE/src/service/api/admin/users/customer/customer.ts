// admin/users/customer/customer.ts

import { API_ADMIN_CUSTOMERS } from '@/constants/url'
// 1. Sửa import: Dùng request đã cấu hình thay vì axios gốc
import request from '@/service/request'

// -----------------------------
// Types
// -----------------------------

export interface Customer {
  id: string
  customerName: string
  customerPhone: string
  customerEmail: string
  customerAvatar?: string
  customerIdAccount?: string
  customerCode?: string
  customerDescription?: string
  customerStatus?: number
  customerGender?: boolean
  customerBirthday?: number
  customerCreatedBy?: number
  customerCreatedDate?: number
  customerModifiedBy?: number
  customerModifiedDate?: number
}

export interface CustomerFilterParams {
  page?: number
  size?: number
  keyword?: string
  customerGender?: number | null
  customerStatus?: number | null
}

// -----------------------------
// CRUD APIs
// -----------------------------
export async function getCustomers(params: CustomerFilterParams = {}) {
  const page = Math.max(1, Number(params.page) || 1)
  const size = Math.max(1, Number(params.size) || 10)

  const query: Record<string, any> = { page, size }

  if (params.keyword?.trim())
    query.keyword = params.keyword.trim()
  if (params.customerGender != null)
    query.customerGender = Number(params.customerGender)
  if (params.customerStatus != null)
    query.customerStatus = Number(params.customerStatus)

  // 2. Thay axios.get thành request.get
  return request.get(API_ADMIN_CUSTOMERS, { params: query })
}

export function getCustomerById(id: string) {
  // 3. Thay axios.get thành request.get
  return request.get<{ data: Customer }>(`${API_ADMIN_CUSTOMERS}/${id}`)
}

export function createCustomer(customer: Customer) {
  // 4. Thay axios.post thành request.post
  return request.post<{ data: Customer }>(API_ADMIN_CUSTOMERS, customer)
}

export function updateCustomer(id: string, customer: Customer) {
  // 5. Thay axios.put thành request.put
  return request.put<{ data: Customer }>(`${API_ADMIN_CUSTOMERS}/${id}`, customer)
}

export function deleteCustomer(id: string) {
  // 6. Thay axios.delete thành request.delete
  return request.delete<void>(`${API_ADMIN_CUSTOMERS}/${id}`)
}

// -----------------------------
// Upload Avatar
// -----------------------------
export async function uploadAvatar(file: File): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    // 7. Thay axios.post thành request.post để có Token (nếu API upload cần quyền)
    // Lưu ý: Đường dẫn http://localhost:2345... đang bị hardcode,
    // bạn nên cân nhắc dùng biến môi trường hoặc dùng request để tự nối Base URL
    const response = await request.post('http://localhost:2345/api/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    const url = response.data?.url
    if (!url)
      throw new Error('Upload failed: server không trả về URL hợp lệ')

    return { url }
  }
  catch (error) {
    console.error('Upload avatar error:', error)
    throw error
  }
}
