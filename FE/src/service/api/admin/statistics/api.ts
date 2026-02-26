import request from '@/service/request'
import type { DefaultResponse } from '@/typings/api/api.common'
import { API_ADMIN_STATISTICS } from '@/constants/url'

// --- 1. INTERFACES (Định nghĩa kiểu dữ liệu) ---

export interface StatisticCard {
  revenue: number
  soldProducts: number
  totalOrders: number
  completed: number
  cancelled: number
  processing: number
  growthRate: string
}

export interface TopProductOverview {
  name: string
  count: number
  price: number
  image: string
}

export interface DashboardOverviewResponse {
  today: StatisticCard
  week: StatisticCard
  month: StatisticCard
  year: StatisticCard
  topSellingProducts: TopProductOverview[]
}

// Interface cho Biểu đồ Doanh thu (QUAN TRỌNG: Phải khớp với dữ liệu trả về)
export interface RevenueChartResponse {
  timeLabels: string[] // Nhãn trục hoành (Ngày/Tháng)
  currentData: number[] // Dữ liệu doanh thu
}

// Interface cho Biểu đồ Tròn
export interface ChartItemResponse {
  name: string
  value: number
}

// --- 2. API FUNCTIONS ---

export async function getOverview() {
  try {
    const res = await request.get<DefaultResponse<DashboardOverviewResponse>>(
      `${API_ADMIN_STATISTICS}/overview`,
    )
    return res.data.data
  }
  catch (error) {
    console.error('Lỗi API tổng quan:', error)
    return null
  }
}

// Chart Doanh thu (SỬA LẠI KIỂU TRẢ VỀ LÀ RevenueChartResponse)
export async function getRevenueChart(type: string = 'week', rangeDate?: number[] | null) {
  try {
    const params: any = { type }
    if (rangeDate && rangeDate.length === 2) {
      params.start = rangeDate[0]
      params.end = rangeDate[1]
    }

    // Sửa <any[]> thành <RevenueChartResponse>
    const res = await request.get<DefaultResponse<RevenueChartResponse>>(
      `${API_ADMIN_STATISTICS}/chart/revenue`,
      { params },
    )
    // Trả về object rỗng nếu null để tránh lỗi crash
    return res.data.data || { timeLabels: [], currentData: [] }
  }
  catch (error) {
    console.error('Lỗi API biểu đồ doanh thu:', error)
    return { timeLabels: [], currentData: [] }
  }
}

// Chart Trạng thái đơn hàng
export async function getOrderStatusChart(type: string = 'week', rangeDate?: number[] | null) {
  try {
    const params: any = { type }
    if (rangeDate && rangeDate.length === 2) {
      params.start = rangeDate[0]
      params.end = rangeDate[1]
    }

    const res = await request.get<DefaultResponse<ChartItemResponse[]>>(
      `${API_ADMIN_STATISTICS}/chart/order-status`,
      { params },
    )
    return res.data.data || []
  }
  catch (error) {
    console.error('Lỗi API biểu đồ trạng thái:', error)
    return []
  }
}

export async function exportRevenueExcel(type: string, rangeDate?: number[] | null) {
  try {
    const params: any = { type }
    if (rangeDate && rangeDate.length === 2) {
      params.start = rangeDate[0]
      params.end = rangeDate[1]
    }
    const res = await request.get<Blob>(`${API_ADMIN_STATISTICS}/export/revenue`, { params, responseType: 'blob' })
    return res.data
  }
  catch (error) {
    console.error('Lỗi xuất Excel:', error)
    return null
  }
}

export async function getTopProductsChart(type: string = 'week', rangeDate?: number[] | null) {
  try {
    const params: any = { type }
    if (rangeDate && rangeDate.length === 2) {
      params.start = rangeDate[0]
      params.end = rangeDate[1]
    }
    const res = await request.get<DefaultResponse<ChartItemResponse[]>>(
      `${API_ADMIN_STATISTICS}/chart/top-products`,
      { params },
    )
    return res.data.data || []
  }
  catch (error) {
    return []
  }
}
export async function getGrowthStats() {
  try {
    const res = await request.get<DefaultResponse<any[]>>( // Thay any bằng interface GrowthItem nếu muốn chặt chẽ
      `${API_ADMIN_STATISTICS}/growth`,
    )
    return res.data.data || []
  }
  catch (error) {
    console.error('Lỗi lấy dữ liệu tăng trưởng:', error)
    return []
  }
}
export async function getTopProductsFilter(type: string = 'month', rangeDate?: number[] | null) {
  try {
    const params: any = { type }
    if (rangeDate && rangeDate.length === 2) {
      params.start = rangeDate[0]
      params.end = rangeDate[1]
    }
    const res = await request.get<DefaultResponse<TopProductOverview[]>>(
      `${API_ADMIN_STATISTICS}/top-products-filter`,
      { params },
    )
    return res.data.data || []
  }
  catch (error) {
    console.error('Lỗi API lấy top sản phẩm:', error)
    return []
  }
}
// lấy danh sách sặp hết hàng
export async function getLowStockProducts(limit: number = 10) {
  try {
    const res = await request.get<DefaultResponse<any>>(
      `${API_ADMIN_STATISTICS}/low-stock-product`,
      { params: { quantity: limit, page: 0, size: limit } },
    )
    return res.data?.data?.content || []
  }
  catch (error) {
    console.error('Lỗi API lấy sản phẩm sắp hết:', error)
    return []
  }
}

export async function sendReportEmailManual(type: string) {
  try {
    const res = await request.get(`${API_ADMIN_STATISTICS}/send-email-report`, {
      params: { type },
    })
    return res.data
  }
  catch (error) {
    console.error('Lỗi gửi email:', error)
    return null
  }
}

// --- 3. EXPORT ---
export const statisticsApi = {
  getOverview,
  getRevenueChart,
  getRevenueChartData: getRevenueChart,
  getOrderStatusChart,
  getOrderStatusChartData: getOrderStatusChart,
  getTopProductsChart,
  exportRevenueExcel,
  getGrowthStats,
  getTopProductsFilter,
  getLowStockProducts,
  sendReportEmailManual,
}
