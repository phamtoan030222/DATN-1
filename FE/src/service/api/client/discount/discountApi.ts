import { API_CUSTOMER_DISCOUNT, API_CUSTOMER_DISCOUNTS } from '@/constants/url'
import type { DefaultResponse, PaginationParams } from '@/typings/api/api.common'
import request from '@/service/request'

export interface ParamsGetDiscount extends PaginationParams {
  q?: string | ''
  discountName?: string
  discountCode?: string
  discountStatus?: number
}

export interface DiscountResponse {
  id: string
  discountName: string
  discountCode: string
  createdDate: number
  startTime: number
  endTime: number
  percentage: number
  description: string

  productCount?: number
}

export interface CreateDiscountRequest {
  discountName: string
  discountCode: string
  percentage: number
  startDate: number
  endDate: number
  description?: string
}

export interface UpdateDiscountRequest {
  discountName: string
  discountCode: string
  percentage: number
  startDate: number
  endDate: number
  description?: string
}

export interface ProductDetailResponse {
  id: string
  productCode: string
  image: string
  productDetailCode: string
  productName: string
  colorName?: string
  ramName?: string
  hardDriveName?: string
  materialName?: string
  gpuName?: string
  cpuName?: string
  price: number
  description?: string
}

export interface AppliedProductResponse {
  id: string
  productCode: string
  image: string
  productDetailCode: string
  productName: string
  discountName: string
  discountCode: string
  percentageDiscount: number
  startTime: number
  endTime: number
  description?: string
  price: number
  colorName?: string
  ramName?: string
  hardDriveName?: string
  gpuName?: string
  cpuName?: string
}

export interface ApplyDiscountRequest {
  productDetailIds: string[]
  discountId: string
  originalPrice: number
  salePrice: number
  description?: string
}

export async function applySingleProductToDiscount(data: ApplyDiscountRequest) {
  try {
    const res = await request.post(`${API_CUSTOMER_DISCOUNTS}/detail/applyProducts`, data)
    return res.data
  }
  catch (error) {
    console.error('Error applying product to discount:', error)
    throw error
  }
}

export async function applyMultipleProducts(discountId: string, products: { productDetailId: string, originalPrice: number, salePrice: number, description?: string }[]) {
  try {
    const results = []

    for (const product of products) {
      const requestData: ApplyDiscountRequest = {
        productDetailIds: [product.productDetailId],
        discountId,
        originalPrice: product.originalPrice,
        salePrice: product.salePrice,
        description: product.description || 'Áp dụng sản phẩm',
      }

      console.log('Applying product:', requestData)
      const result = await applySingleProductToDiscount(requestData)
      results.push(result)
    }

    return results
  }
  catch (error) {
    console.error('Error applying multiple products:', error)
    throw error
  }
}

export async function getAppliedProducts(discountId: string, params: PaginationParams) {
  console.log('🚀 Fetching applied products for discount:', discountId)
  console.log('📤 API params:', params)

  const page = (params.page && params.page > 0) ? params.page - 1 : 0
  const size = params.size || 10

  const queryParams: any = {
    discountId,
    page,
    size,
  }

  if (params.q?.trim()) {
    queryParams.q = params.q.trim()
  }

  try {
    const res = await request.get<DefaultResponse<any>>(
      `${API_CUSTOMER_DISCOUNTS}/detail/applied-products`,
      { params: queryParams },
    )
    console.log('📥 Applied products API response:', res.data)
    const responseData = res.data.data
    if (!responseData) {
      console.warn('⚠️ No responseData found')
      return {
        items: [],
        totalItems: 0,
        totalPages: 0,
        currentPage: 1,
      }
    }

    const items = Array.isArray(responseData.data)
      ? responseData.data
      : Array.isArray(responseData.content)
        ? responseData.content
        : Array.isArray(responseData) ? responseData : []

    console.log('✅ Applied products items:', items)

    return {
      items,
      totalItems: responseData.totalElements || responseData.total || items.length || 0,
      totalPages: responseData.totalPages || Math.ceil((responseData.totalElements || items.length) / size) || 0,
      currentPage: (responseData.currentPage ?? responseData.number ?? 0) + 1,
    }
  }
  catch (error) {
    console.error('❌ Error fetching applied products:', error)
    throw error
  }
}

export async function getUnappliedProducts(discountId: string, params: PaginationParams) {
  console.log('🚀 Fetching unapplied products for discount:', discountId)
  console.log('📤 API params:', params)

  const page = (params.page && params.page > 0) ? params.page - 1 : 0
  const size = params.size || 10

  const queryParams: any = {
    discountId,
    page,
    size,
  }

  if (params.q?.trim()) {
    queryParams.q = params.q.trim()
  }

  try {
    const res = await request.get<DefaultResponse<any>>(
      `${API_CUSTOMER_DISCOUNTS}/detail/not-applied-products`,
      { params: queryParams },
    )

    console.log('📥 Unapplied products API response:', res.data)

    const responseData = res.data.data
    if (!responseData) {
      console.warn('⚠️ No responseData found')
      return {
        items: [],
        totalItems: 0,
        totalPages: 0,
        currentPage: 1,
      }
    }

    const items = Array.isArray(responseData.data)
      ? responseData.data
      : Array.isArray(responseData.content)
        ? responseData.content
        : Array.isArray(responseData) ? responseData : []

    console.log('✅ Unapplied products items:', items)

    return {
      items,
      totalItems: responseData.totalElements || responseData.total || items.length || 0,
      totalPages: responseData.totalPages || Math.ceil((responseData.totalElements || items.length) / size) || 0,
      currentPage: (responseData.currentPage ?? responseData.number ?? 0) + 1,
    }
  }
  catch (error) {
    console.error('❌ Error fetching unapplied products:', error)
    throw error
  }
}

// Áp dụng nhiều sản phẩm vào discount
export async function applyProducts(discountId: string, products: { productDetailId: string, originalPrice: number, salePrice: number, description?: string }[]) {
  try {
    const res = await request.post(`${API_CUSTOMER_DISCOUNTS}/detail/applyProducts`, {
      discountId,
      products,
    })
    return res.data
  }
  catch (error) {
    console.error('Error applying products to discount:', error)
    throw error
  }
}

export async function removeProductsFromDiscount(id: string) {
  const res = await request.put(`${API_CUSTOMER_DISCOUNTS}/detail/updateStatus/${id}`)
  return res.data
}

export async function getAllDiscounts(params: ParamsGetDiscount) {
  const queryParams: any = {
    page: params.page || 1,
    size: params.size || 10,
  }

  if (params.q && params.q.trim()) {
    queryParams.discountName = params.q.trim()
  }
  else if (params.discountName && params.discountName.trim()) {
    queryParams.discountName = params.discountName.trim()
  }

  if (params.discountStatus !== undefined && params.discountStatus !== null) {
    queryParams.discountStatus = params.discountStatus
  }

  try {
    const res = await request.get<DefaultResponse<any>>(API_CUSTOMER_DISCOUNT, {
      params: queryParams,
    })

    const responseData = res.data.data

    return {
      items: responseData.data || [],
      totalItems: responseData.totalElements || 0,
      totalPages: responseData.totalPages || 0,
      currentPage: responseData.currentPage || 1,
    }
  }
  catch (error) {
    console.error('Error fetching discounts:', error)
    return {
      items: [],
      totalItems: 0,
      totalPages: 0,
      currentPage: 1,
    }
  }
}

export interface ProductResponse {
  id: string
  productCode: string
  productName: string
  productBrand: string
  quantity: number
}

export interface ProductDetailResponse {
  id: string
  productCode: string
  image: string
  productDetailCode: string
  productName: string
  colorName?: string
  ramName?: string
  hardDriveName?: string
  materialName?: string
  gpuName?: string
  cpuName?: string
  price: number
  description?: string
}

export async function getAllProducts(params: PaginationParams) {
  const page = (params.page && params.page > 0) ? params.page - 1 : 0
  const size = params.size || 10

  const queryParams: any = {
    page,
    size,
  }

  if (params.q?.trim()) {
    queryParams.q = params.q.trim()
  }

  try {
    const res = await request.get<DefaultResponse<any>>(
      `${API_CUSTOMER_DISCOUNTS}/detail`,
      { params: queryParams },
    )

    const responseData = res.data.data
    return {
      items: responseData?.content || responseData?.data || [],
      totalItems: responseData?.totalElements || responseData?.total || 0,
      totalPages: responseData?.totalPages || 0,
      currentPage: (responseData?.number ?? page) + 1,
    }
  }
  catch (error) {
    console.error('Error fetching products:', error)
    return {
      items: [],
      totalItems: 0,
      totalPages: 0,
      currentPage: 1,
    }
  }
}

export async function getProductDetailsByProductId(productId: string) {
  try {
    const res = await request.get<DefaultResponse<ProductDetailResponse[]>>(
      `${API_CUSTOMER_DISCOUNTS}/detail/${productId}`,
    )
    return res.data
  }
  catch (error) {
    console.error(`❌ Error fetching product details for productId=${productId}:`, error)
    throw error
  }
}

export async function createDiscount(data: CreateDiscountRequest) {
  const res = await request.post(`${API_CUSTOMER_DISCOUNT}/addDiscount`, data)
  return res.data
}

export async function updateDiscount(id: string, data: UpdateDiscountRequest) {
  const res = await request.put(`${API_CUSTOMER_DISCOUNT}/updateDiscount/${id}`, data)
  return res.data
}

export async function deactivateDiscount(id: string) {
  const res = await request.put(`${API_CUSTOMER_DISCOUNT}/end/${id}`)
  return res.data
}

export async function startDiscount(id: string) {
  const res = await request.put(`${API_CUSTOMER_DISCOUNT}/start/${id}`)
  return res.data
}

export async function deleteDiscount(id: string) {
  const res = await request.delete(`${API_CUSTOMER_DISCOUNT}/delete/${id}`)
  return res.data
}

export async function sendEmail(id: string) {
  const res = await request.post(`${API_CUSTOMER_DISCOUNT}/sendEmail/${id}`)
  return res.data
}
