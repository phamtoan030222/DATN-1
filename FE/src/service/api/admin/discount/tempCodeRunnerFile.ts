xport interface ADCustomerResponseWrapper {
  content: ADCustomerResponse[]
  totalElements: number
}

export async function getCustomers(query: ADCustomerQuery = {}): Promise<ADCustomerResponseWrapper> {
  try {
    const url = `${API_ADMIN_CUSTOMERS}?${new URLSearchParams(query as any).toString()}`
    const res = await request(url, {
      method: 'GET',
    })
    return res.data // Adjust based on actual response structure (e.g., res.data.content, res.data.totalElements)
  }
  catch (err) {
    throw new Error(`Lỗi tải danh sách khách hàng: ${(err as Error).message || 'Unknown error'}`)
  }
}