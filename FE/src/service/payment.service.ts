import axios from 'axios'

const API_URL = 'http://localhost:2345/api/payment'

export interface VNPayPaymentRequest {
  orderId: string
  amount: number
  orderInfo?: string
  customerName?: string
  customerPhone?: string
  customerEmail?: string
  customerAddress?: string
  orderType?: string
  language?: string
}

export interface VNPayPaymentResponse {
  code: string
  message: string
  paymentUrl: string
  transactionId: string
  amount: number
  orderId: string
}

class PaymentService {
  async createVNPayPayment(data: VNPayPaymentRequest): Promise<VNPayPaymentResponse> {
    try {
      const response = await axios.post(`${API_URL}/create-vnpay`, data)
      return response.data
    }
    catch (error) {
      console.error('Lỗi tạo thanh toán VNPAY:', error)
      throw error
    }
  }

  async checkPaymentResult(params: any): Promise<VNPayPaymentResponse> {
    try {
      const response = await axios.get(`${API_URL}/vnpay-return`, { params })
      return response.data
    }
    catch (error) {
      console.error('Lỗi kiểm tra kết quả:', error)
      throw error
    }
  }
}

export default new PaymentService()
