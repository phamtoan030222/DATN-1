export const { VITE_BASE_URL_SERVER } = import.meta.env || {}
export const { VITE_BASE_URL_CLIENT } = import.meta.env || {}

export const API_URL = `${VITE_BASE_URL_SERVER}/api/v1` as string

export const PREFIX_API_ADMIN = `${API_URL}/admin` as string

export const API_ADMIN_GEO = `${API_URL}/geo` as string
export const API_ADMIN_STAFF = `${PREFIX_API_ADMIN}/staff` as string

export const API_ADMIN_PRODUCTS = `${PREFIX_API_ADMIN}/products` as string
export const API_ADMIN_COLOR = `${API_ADMIN_PRODUCTS}/color` as string
export const API_ADMIN_RAM = `${API_ADMIN_PRODUCTS}/ram` as string
export const API_ADMIN_HARD_DRIVE = `${API_ADMIN_PRODUCTS}/harddrive` as string
export const API_ADMIN_MATERIAL = `${API_ADMIN_PRODUCTS}/material` as string

export const API_ADMIN_PRODUCT_CPU = `${API_ADMIN_PRODUCTS}/cpu` as string
export const API_ADMIN_PRODUCT_GPU = `${API_ADMIN_PRODUCTS}/gpu` as string
export const API_ADMIN_PRODUCT_SCREEN
  = `${API_ADMIN_PRODUCTS}/screen` as string
export const API_ADMIN_PRODUCT_DETAIL
  = `${API_ADMIN_PRODUCTS}/product-detail` as string
export const API_ADMIN_PRODUCT_BRAND = `${API_ADMIN_PRODUCTS}/brand` as string
export const API_ADMIN_PRODUCT_OPERATING_SYSTEM
  = `${API_ADMIN_PRODUCTS}/operating` as string
export const API_ADMIN_BATTERY = `${API_ADMIN_PRODUCTS}/battery` as string
export const API_ADMIN_DISCOUNT
  = `${PREFIX_API_ADMIN}/discounts/discount` as string

export const API_ADMIN_PRODUCT_VOUCHER
  = `${API_ADMIN_PRODUCTS}/voucher` as string
export const API_ADMIN_CUSTOMERS = `${PREFIX_API_ADMIN}/customers` as string

export const API_ADMIN_CUSTOMER_ADDRESSES = (customerId: string) => `${API_ADMIN_CUSTOMERS}/${customerId}/addresses`

export const API_ADMIN_CUSTOMER_ADDRESS_DEFAULT = (customerId: string, addressId: string) => `${API_ADMIN_CUSTOMERS}/${customerId}/addresses/${addressId}/set-default`

export const API_ADMIN_DISCOUNTS = `${PREFIX_API_ADMIN}/discounts` as string

export const API_ADMIN_DISCOUNTS_VOUCHER
  = `${API_ADMIN_DISCOUNTS}/voucher` as string

// api/v1/admin/discouts/voucher
