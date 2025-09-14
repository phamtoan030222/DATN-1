// API customer

import { API_ADMIN_CUSTOMERS } from '@/constants/url'
import axios from 'axios'

// -----------------------------
// Types
// -----------------------------

export interface Customer {
  id?: string
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
  customerName?: string
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

  if (params.customerName?.trim())
    query.customerName = params.customerName.trim()
  if (params.customerGender != null)
    query.customerGender = Number(params.customerGender)
  if (params.customerStatus != null)
    query.customerStatus = Number(params.customerStatus)

  return axios.get(API_ADMIN_CUSTOMERS, { params: query })
}

export function getCustomerById(id: string) {
  return axios.get<{ data: Customer }>(`${API_ADMIN_CUSTOMERS}/${id}`)
}

export function createCustomer(customer: Customer) {
  return axios.post<{ data: Customer }>(API_ADMIN_CUSTOMERS, customer)
}

export function updateCustomer(id: string, customer: Customer) {
  return axios.put<{ data: Customer }>(`${API_ADMIN_CUSTOMERS}/${id}`, customer)
}

export function deleteCustomer(id: string) {
  return axios.delete<void>(`${API_ADMIN_CUSTOMERS}/${id}`)
}

// -----------------------------
// Upload Avatar
// -----------------------------
export async function uploadAvatar(file: File): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await axios.post('http://localhost:2345/api/upload', formData, {
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
