import axios from 'axios'
import {
    API_ADMIN_CUSTOMER_ADDRESS_DEFAULT,
  API_ADMIN_CUSTOMER_ADDRESSES,
} from '@/constants/url'

export interface Address {
  id?: string
  provinceCity: string
  district: string
  wardCommune: string
  addressDetail: string
  isDefault?: boolean
  createdDate?: number
  modifiedDate?: number
}

export function getAddressesByCustomer(customerId: string) {
  return axios.get<{ data: Address[] }>(API_ADMIN_CUSTOMER_ADDRESSES(customerId))
}

export function createAddress(customerId: string, address: Address) {
  return axios.post<{ data: Address }>(API_ADMIN_CUSTOMER_ADDRESSES(customerId), address)
}

export function updateAddress(customerId: string, addressId: string, address: Address) {
  return axios.put<{ data: Address }>(
    `${API_ADMIN_CUSTOMER_ADDRESSES(customerId)}/${addressId}`,
    address,
  )
}

export function deleteAddress(customerId: string, addressId: string) {
  return axios.delete<void>(`${API_ADMIN_CUSTOMER_ADDRESSES(customerId)}/${addressId}`)
}

export function setDefaultAddress(customerId: string, addressId: string) {
  return axios.put<void>(API_ADMIN_CUSTOMER_ADDRESS_DEFAULT(customerId, addressId))
}
