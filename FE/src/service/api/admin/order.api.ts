import request from '@/service/request';

/** API Bán hàng tại quầy */
export class OrderApi {
  // Tạo đơn hàng tại quầy
  static createOrderAtCounter(data: any) {
    return request.post<any>('/admin/orders/counter', data);
  }

  // Tìm kiếm sản phẩm để bán (Sử dụng API list sản phẩm có sẵn)
  static searchProducts(data: any) {
    return request.post<any>('/admin/product/list', data);
  }

  // Tìm kiếm khách hàng (Sử dụng API khách hàng có sẵn)
  static searchCustomers(data: any) {
    return request.post<any>('/admin/customer/list', data);
  }
}
