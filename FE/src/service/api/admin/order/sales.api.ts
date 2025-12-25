import axios from "axios";

import { API_ADMIN_CUSTOMERS, API_ADMIN_SALES } from "@/constants/url";


// Interface trả về từ Backend (Entity)
export interface Customer {
  id: string;
  code: string;
  name: string; // Thêm name vì thường hiển thị tên
  phone: string; // Phone nên để string để giữ số 0 ở đầu
  email?: string;
  // ... các trường khác
}

// Interface bộ lọc gửi lên (Request)
export interface CustomerFilterParams {
  page?: number;
  size?: number;
  customerName?: string;
  customerPhone?: string | number;
  keyword?: string; // Thường dùng 1 ô input để tìm chung cho cả tên và sđt
}

export function getCustomers(params?: CustomerFilterParams) {
  return axios.get(API_ADMIN_CUSTOMERS, {
    params
    // axios sẽ tự chuyển object này thành query string:
    // ?page=0&size=10&customerName=...
  });
}
