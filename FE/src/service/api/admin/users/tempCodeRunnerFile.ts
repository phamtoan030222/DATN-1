export async function updateStaff(id: string, data: CreateStaffRequest) {
  const res = await request({
    url: `${API_ADMIN_STAFF}/${id}`,
    method: 'PUT',
    data,
  })
  return res.data.data
}