import request from '@/service/request'
import { DefaultResponse } from '@/typings/api/api.common'
import { API_ADMIN_STATISTICS } from '@/constants/url' 

// --- 1. INTERFACES (Định nghĩa kiểu dữ liệu) ---

export interface StatisticCard {
  revenue: number;        
  soldProducts: number;   
  totalOrders: number;    
  completed: number;      
  cancelled: number;       
  processing: number;      
  growthRate: string;      
}

export interface TopProductOverview {
  name: string;
  count: number;
  price: number;
  image: string;
}

export interface DashboardOverviewResponse {
  today: StatisticCard;
  week: StatisticCard;
  month: StatisticCard;
  year: StatisticCard;
  topSellingProducts: TopProductOverview[];
}

// Interface cho Biểu đồ Doanh thu (QUAN TRỌNG: Phải khớp với dữ liệu trả về)
export interface RevenueChartResponse {
  timeLabels: string[];   // Nhãn trục hoành (Ngày/Tháng)
  currentData: number[];  // Dữ liệu doanh thu
}

// Interface cho Biểu đồ Tròn
export interface ChartItemResponse {
  name: string;
  value: number;
}

// --- 2. API FUNCTIONS ---

export const getOverview = async () => {
  try {
    const res = await request.get<DefaultResponse<DashboardOverviewResponse>>(
      `${API_ADMIN_STATISTICS}/overview`
    )
    return res.data.data
  } catch (error) {
    console.error("Lỗi API tổng quan:", error)
    return null
  }
}

// Chart Doanh thu (SỬA LẠI KIỂU TRẢ VỀ LÀ RevenueChartResponse)
export const getRevenueChart = async (type: string = 'week', rangeDate?: number[] | null) => {
  try {
    const params: any = { type };
    if (rangeDate && rangeDate.length === 2) {
      params.start = rangeDate[0];
      params.end = rangeDate[1];
    }

    // Sửa <any[]> thành <RevenueChartResponse>
    const res = await request.get<DefaultResponse<RevenueChartResponse>>(
      `${API_ADMIN_STATISTICS}/chart/revenue`,
      { params }
    )
    // Trả về object rỗng nếu null để tránh lỗi crash
    return res.data.data || { timeLabels: [], currentData: [] }
  } catch (error) {
    console.error("Lỗi API biểu đồ doanh thu:", error)
    return { timeLabels: [], currentData: [] }
  }
}

// Chart Trạng thái đơn hàng
export const getOrderStatusChart = async (type: string = 'week', rangeDate?: number[] | null) => {
  try {
    const params: any = { type };
    if (rangeDate && rangeDate.length === 2) {
      params.start = rangeDate[0];
      params.end = rangeDate[1];
    }

    const res = await request.get<DefaultResponse<ChartItemResponse[]>>(
      `${API_ADMIN_STATISTICS}/chart/order-status`,
      { params }
    )
    return res.data.data || []
  } catch (error) {
    console.error("Lỗi API biểu đồ trạng thái:", error)
    return []
  }
}

export const exportRevenueExcel = async () => {
  try {
    const res = await request.get(`${API_ADMIN_STATISTICS}/export/revenue`, {
      responseType: 'blob', 
    });
    return res.data;
  } catch (error) {
    console.error("Lỗi xuất Excel:", error);
    throw error;
  }
};

export const getTopProductsChart = async (type: string = 'week', rangeDate?: number[] | null) => {
  try {
    const params: any = { type };
    if (rangeDate && rangeDate.length === 2) {
      params.start = rangeDate[0];
      params.end = rangeDate[1];
    }
    const res = await request.get<DefaultResponse<ChartItemResponse[]>>(
      `${API_ADMIN_STATISTICS}/chart/top-products`,
      { params }
    )
    return res.data.data || []
  } catch (error) {
    return []
  }
}

// --- 3. EXPORT ---
export const statisticsApi = {
  getOverview,
  getRevenueChart,
  // Alias giúp code cũ không bị lỗi
  getRevenueChartData: getRevenueChart, 
  getOrderStatusChart,
  getOrderStatusChartData: getOrderStatusChart, 
  getTopProductsChart,
  exportRevenueExcel
}