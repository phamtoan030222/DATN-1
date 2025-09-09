
import { API_ADMIN_DISCOUNT, API_ADMIN_DISCOUNTS } from '@/constants/url'
import { DefaultResponse, PaginationParams } from '@/typings/api/api.common'
import request from '@/service/request'

export interface ParamsGetDiscount extends PaginationParams {
  q?: string | ''
  discountName?: string
  discountCode?:string
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
  productName: string
  discountName: string
  discountCode: string
  percentageDiscount: number
  startTime: number
  endTime: number
  description?: string
  price: number 
}


export interface ApplyDiscountRequest {
  productDetailIds: string[]
  discountId: string
  originalPrice: number
  salePrice: number
  description?: string
}

export const applySingleProductToDiscount = async (data: ApplyDiscountRequest) => {
  try {
    const res = await request.post(`${API_ADMIN_DISCOUNTS}/detail/applyProducts`, data)
    return res.data
  } catch (error) {
    console.error("Error applying product to discount:", error)
    throw error
  }
}

export const applyMultipleProducts = async (
  discountId: string,
  products: { productDetailId: string; originalPrice: number; salePrice: number; description?: string }[]
) => {
  try {
    const results = []
    
    for (const product of products) {
      const requestData: ApplyDiscountRequest = {
        productDetailIds: [product.productDetailId],
        discountId: discountId,
        originalPrice: product.originalPrice,
        salePrice: product.salePrice,
        description: product.description || "Áp dụng sản phẩm"
      }
      
      console.log('Applying product:', requestData)
      const result = await applySingleProductToDiscount(requestData)
      results.push(result)
    }
    
    return results
  } catch (error) {
    console.error("Error applying multiple products:", error)
    throw error
  }
}


export const getAppliedProducts = async (
  discountId: string,
  params: PaginationParams
) => {
  console.log('🚀 Fetching applied products for discount:', discountId);
  console.log('📤 API params:', params);
  
  const page = (params.page && params.page > 0) ? params.page - 1 : 0; 
  const size = params.size || 10;

  const queryParams: any = {
    discountId,
    page,
    size,
  };

  if (params.q?.trim()) {
    queryParams.q = params.q.trim();
  }

  try {
    const res = await request.get<DefaultResponse<any>>(
      `${API_ADMIN_DISCOUNTS}/detail/applied-products`,
      { params: queryParams }
    );
    console.log('📥 Applied products API response:', res.data);
    const responseData = res.data.data;
    if (!responseData) {
      console.warn('⚠️ No responseData found');
      return {
        items: [],
        totalItems: 0,
        totalPages: 0,
        currentPage: 1,
      };
    }

    const items = Array.isArray(responseData.data) ? responseData.data : 
                 Array.isArray(responseData.content) ? responseData.content :
                 Array.isArray(responseData) ? responseData : [];

    console.log('✅ Applied products items:', items);

    return {
      items: items,
      totalItems: responseData.totalElements || responseData.total || items.length || 0,
      totalPages: responseData.totalPages || Math.ceil((responseData.totalElements || items.length) / size) || 0,
      currentPage: (responseData.currentPage ?? responseData.number ?? 0) + 1,
    };
  } catch (error) {
    console.error('❌ Error fetching applied products:', error);
    throw error;
  }
};

export const getUnappliedProducts = async (
  discountId: string,
  params: PaginationParams
) => {
  console.log('🚀 Fetching unapplied products for discount:', discountId);
  console.log('📤 API params:', params);
  
  const page = (params.page && params.page > 0) ? params.page - 1 : 0;
  const size = params.size || 10;

  const queryParams: any = {
    discountId,
    page,
    size,
  };

  if (params.q?.trim()) {
    queryParams.q = params.q.trim();
  }

  try {
    const res = await request.get<DefaultResponse<any>>(
      `${API_ADMIN_DISCOUNTS}/detail/not-applied-products`,
      { params: queryParams }
    );

    console.log('📥 Unapplied products API response:', res.data);

    const responseData = res.data.data;
    if (!responseData) {
      console.warn('⚠️ No responseData found');
      return {
        items: [],
        totalItems: 0,
        totalPages: 0,
        currentPage: 1,
      };
    }

    const items = Array.isArray(responseData.data) ? responseData.data : 
                 Array.isArray(responseData.content) ? responseData.content :
                 Array.isArray(responseData) ? responseData : [];

    console.log('✅ Unapplied products items:', items);

    return {
      items: items,
      totalItems: responseData.totalElements || responseData.total || items.length || 0,
      totalPages: responseData.totalPages || Math.ceil((responseData.totalElements || items.length) / size) || 0,
      currentPage: (responseData.currentPage ?? responseData.number ?? 0) + 1,
    };
  } catch (error) {
    console.error('❌ Error fetching unapplied products:', error);
    throw error;
  }
};



// Áp dụng nhiều sản phẩm vào discount
export const applyProducts = async (
  discountId: string,
  products: { productDetailId: string; originalPrice: number; salePrice: number; description?: string }[]
) => {
  try {
    const res = await request.post(`${API_ADMIN_DISCOUNTS}/detail/applyProducts`, {
      discountId,
      products,
    })
    return res.data
  } catch (error) {
    console.error("Error applying products to discount:", error)
    throw error
  }
}


export const removeProductsFromDiscount = async(id:String) =>{
  const res = await request.put(`${API_ADMIN_DISCOUNTS}/detail/updateStatus/${id}`)
  return res.data
}

export const getAllDiscounts = async (params: ParamsGetDiscount) => {
 const queryParams: any = {
  page: params.page || 1, 
  size: params.size || 10,
}

 if (params.q && params.q.trim()) {
   queryParams.discountName = params.q.trim()
 } else if (params.discountName && params.discountName.trim()) {
   queryParams.discountName = params.discountName.trim()
 }

 if (params.discountStatus !== undefined && params.discountStatus !== null) {
   queryParams.discountStatus = params.discountStatus
 }

 try {
   const res = await request.get<DefaultResponse<any>>(API_ADMIN_DISCOUNT, {
     params: queryParams,
   })


   const responseData = res.data.data

   return {
     items: responseData.data || [],
     totalItems: responseData.totalElements || 0,
     totalPages: responseData.totalPages || 0,
     currentPage: responseData.currentPage || 1,
   }
 } catch (error) {
   console.error("Error fetching discounts:", error);
   return {
     items: [],
     totalItems: 0,
     totalPages: 0,
     currentPage: 1,
   }
 }
}


export const createDiscount = async (data: CreateDiscountRequest) => {
  const res = await request.post(`${API_ADMIN_DISCOUNT}/addDiscount`, data)
  return res.data
}


export const updateDiscount = async (id: string, data: UpdateDiscountRequest) => {
  const res = await request.put(`${API_ADMIN_DISCOUNT}/updateDiscount/${id}`, data)
  return res.data
}


export const deactivateDiscount = async (id: string) => {
  const res = await request.put(`${API_ADMIN_DISCOUNT}/end/${id}`)
  return res.data
}

export const startDiscount = async (id: string) => {
  const res = await request.put(`${API_ADMIN_DISCOUNT}/start/${id}`)
  return res.data
}


export const deleteDiscount = async (id: string) => {
  const res = await request.delete(`${API_ADMIN_DISCOUNT}/delete/${id}`)
  return res.data
}

export const sendEmail = async (id: string) => {
  const res = await request.post(`${API_ADMIN_DISCOUNT}/sendEmail/${id}`)
  return res.data
}