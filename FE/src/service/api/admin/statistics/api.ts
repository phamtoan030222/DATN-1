import request from '@/service/request'
import { DefaultResponse, PaginationParams } from '@/typings/api/api.common'
import { API_ADMIN_STATISTICS } from '@/constants/url' 



export interface TopProductOverview {
  name: string;
  salesCount: number;
}

export interface TopItemDTO {
  name: string;
  count: number;
}


export interface TopCategoryOverview {
  name: string;
  productCount: number;
}

export interface DashboardOverviewResponse {
  // Doanh thu
  revenueToday: number
  revenueWeek: number
  revenueMonth: number
  revenueYear?: number
  totalRevenue: number
  
  // Đơn hàng
  totalOrders: number
  successfulOrders: number
  cancelledOrders: number
  pendingOrders: number
  processingOrders: number
  
  // Sản phẩm
  totalProducts: number
  lowStockProducts: number

  // KHÁCH HÀNG (Mới thêm)
  totalCustomers: number
  newCustomersMonth: number

  topSellingProducts: TopProductOverview[];
  topBrands: TopItemDTO[];

}

export interface GrowthStatResponse {
  label: string
  value: string
  percent: string
  trend: 'up' | 'down'
}

export interface LowStockProductResponse {
  id: string
  name: string
  brandName: string
  quantity: number
  createdDate: number
}

export interface RevenueChartResponse {
  timeLabels: string[]
  currentData: number[]
  previousData: number[]
}

export interface ChartItemResponse {
  name: string
  value: number
}

// --- 2. API FUNCTIONS ---

// 1. Lấy dữ liệu Tổng quan (3 Card đầu + Card Khách hàng)
export const getOverview = async () => {
  try {
    const res = await request.get<DefaultResponse<DashboardOverviewResponse>>(
      `${API_ADMIN_STATISTICS}/overview`
    )
    return res.data.data
  } catch (error) {
    console.error("❌ Error fetching overview:", error)
    return null
  }
}

// 2. Lấy dữ liệu bảng Tăng trưởng
export const getGrowthStats = async () => {
  try {
    const res = await request.get<DefaultResponse<GrowthStatResponse[]>>(
      `${API_ADMIN_STATISTICS}/growth`
    )
    return res.data.data || []
  } catch (error) {
    console.error("❌ Error fetching growth stats:", error)
    return []
  }
}

// 3. Lấy danh sách sản phẩm sắp hết hàng (Phân trang)
export const getLowStockProducts = async (
  limitQuantity: number, 
  params: PaginationParams
) => {
  const page = (params.page && params.page > 0) ? params.page - 1 : 0
  const size = params.size || 5 

  const queryParams: any = {
    quantity: limitQuantity, 
    page,
    size,
  }

  try {
    const res = await request.get<DefaultResponse<any>>(
      `${API_ADMIN_STATISTICS}/low-stock-product`,
      { params: queryParams }
    )

    const responseData = res.data.data

    return {
      items: responseData?.content || [],
      totalItems: responseData?.totalElements || 0,
      totalPages: responseData?.totalPages || 0,
      currentPage: (responseData?.number ?? page) + 1,
    }
  } catch (error) {
    console.error("❌ Error fetching low stock products:", error)
    return { items: [], totalItems: 0, totalPages: 0, currentPage: 1 }
  }
}

// 4. Chart Doanh thu
export const getRevenueChart = async (type: string = 'week') => {
  try {
    const res = await request.get<DefaultResponse<RevenueChartResponse>>(
      `${API_ADMIN_STATISTICS}/chart/revenue`,
      { params: { type } }
    )
    return res.data.data
  } catch (error) {
    console.error("❌ Error fetching revenue chart:", error)
    return { timeLabels: [], currentData: [], previousData: [] }
  }
}

// 5. Chart Trạng thái đơn hàng
export const getOrderStatusChart = async (type: string = 'week') => {
  try {
    const res = await request.get<DefaultResponse<ChartItemResponse[]>>(
      `${API_ADMIN_STATISTICS}/chart/order-status`,
      { params: { type } }
    )
    return res.data.data || []
  } catch (error) {
    console.error("❌ Error fetching order status chart:", error)
    return []
  }
}

// 6. Chart Top sản phẩm
export const getTopProductsChart = async (type: string = 'week') => {
  try {
    const res = await request.get<DefaultResponse<ChartItemResponse[]>>(
      `${API_ADMIN_STATISTICS}/chart/top-products`,
      { params: { type } }
    )
    return res.data.data || []
  } catch (error) {
    console.error("❌ Error fetching top products chart:", error)
    return []
  }
}

export const statisticsApi = {
  getOverview,
  getGrowthStats,
  getLowStockProducts,
  getRevenueChart,
  getOrderStatusChart,
  getTopProductsChart
}