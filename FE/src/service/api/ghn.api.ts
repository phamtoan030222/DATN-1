// =====================================================
// THAY 2 DÒNG NÀY bằng thông tin thật từ GHN dashboard
// =====================================================
const GHN_TOKEN = 'c0c5f890-1d47-11f1-b27c-4e3d3481b5ab'
const GHN_SHOP_ID = 123456
// =====================================================

const BASE = 'https://online-gateway.ghn.vn/shiip/public-api'

// =====================================================
// CACHE — tránh gọi API lặp đi lặp lại
// =====================================================
let _provinceCache: any[] = []
const _districtCache: Record<number, any[]> = {}
const _wardCache: Record<number, any[]> = {}
const _wardMapCache: Record<string, { wardCode: string, districtId: number } | null> = {}

// =====================================================
// CHUẨN HÓA TÊN
// Mục đích: so sánh "Xã Quảng Phú" === "Quảng Phú" === "quang phu"
// =====================================================
function normalizeName(name: string): string {
  if (!name)
    return ''
  return name
    .toLowerCase()
    .replace(/^(xã\s+|phường\s+|thị\s+trấn\s+)/i, '')
    .normalize('NFD')
    .replace(/[\u0300-\u036F]/g, '')
    .replace(/đ/g, 'd')
    .replace(/[^a-z0-9\s]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

// =====================================================
// LẤY DANH SÁCH TỈNH GHN (có cache)
// =====================================================
async function fetchGHNProvinces(): Promise<any[]> {
  if (_provinceCache.length > 0)
    return _provinceCache

  const res = await fetch(`${BASE}/master-data/province`, {
    headers: { Token: GHN_TOKEN },
  })
  const data = await res.json()
  _provinceCache = data?.data || []
  return _provinceCache
}

// =====================================================
// LẤY DANH SÁCH QUẬN/HUYỆN GHN THEO TỈNH (có cache)
// =====================================================
async function fetchGHNDistricts(provinceId: number): Promise<any[]> {
  if (_districtCache[provinceId])
    return _districtCache[provinceId]

  const res = await fetch(`${BASE}/master-data/district?province_id=${provinceId}`, {
    headers: { Token: GHN_TOKEN },
  })
  const data = await res.json()
  _districtCache[provinceId] = data?.data || []
  return _districtCache[provinceId]
}

// =====================================================
// LẤY DANH SÁCH XÃ/PHƯỜNG GHN THEO QUẬN (có cache)
// =====================================================
async function fetchGHNWards(districtId: number): Promise<any[]> {
  if (_wardCache[districtId])
    return _wardCache[districtId]

  const res = await fetch(`${BASE}/master-data/ward?district_id=${districtId}`, {
    headers: { Token: GHN_TOKEN },
  })
  const data = await res.json()
  _wardCache[districtId] = data?.data || []
  return _wardCache[districtId]
}

// =====================================================
// HÀM CHÍNH: Map tên tỉnh + tên xã → GHN districtId + wardCode
//
// Lý do cần hàm này:
//   - DB của bạn lưu: tỉnh="thanh_hoa", xã="xa_quang_phu"
//   - GHN cần:        districtId=3196, wardCode="90113"
//   - Hàm này làm cầu nối giữa 2 hệ thống
// =====================================================
export async function findGHNLocation(
  provinceName: string,
  wardName: string,
): Promise<{ wardCode: string, districtId: number } | null> {
  const cacheKey = `${normalizeName(provinceName)}__${normalizeName(wardName)}`

  // Trả về cache nếu đã tìm trước đó
  if (cacheKey in _wardMapCache)
    return _wardMapCache[cacheKey]

  try {
    // Bước 1: Tìm tỉnh GHN khớp tên
    const provinces = await fetchGHNProvinces()
    const province = provinces.find(
      p => normalizeName(p.ProvinceName) === normalizeName(provinceName),
    )

    if (!province) {
      console.warn('[GHN] Không tìm thấy tỉnh:', provinceName)
      _wardMapCache[cacheKey] = null
      return null
    }

    // Bước 2: Lấy tất cả quận của tỉnh
    const districts = await fetchGHNDistricts(province.ProvinceID)

    // Bước 3: Duyệt từng quận, tìm xã khớp tên
    for (const district of districts) {
      const wards = await fetchGHNWards(district.DistrictID)
      const ward = wards.find(
        w => normalizeName(w.WardName) === normalizeName(wardName),
      )
      if (ward) {
        const result = {
          wardCode: ward.WardCode,
          districtId: district.DistrictID,
        }
        _wardMapCache[cacheKey] = result
        return result
      }
    }

    console.warn('[GHN] Không tìm thấy xã:', wardName, 'trong tỉnh:', provinceName)
    _wardMapCache[cacheKey] = null
    return null
  }
  catch (e) {
    console.error('[GHN] Lỗi tìm địa chỉ:', e)
    return null
  }
}

// =====================================================
// HÀM TÍNH PHÍ SHIP
// Trả về số tiền phí (VND) hoặc null nếu lỗi
// =====================================================
export async function calculateGHNShippingFee(params: {
  districtId: number
  wardCode: string
  insuranceValue: number
  weight?: number // gram, mặc định 1500g (1.5kg)
}): Promise<number | null> {
  try {
    const res = await fetch(`${BASE}/v2/shipping-order/fee`, {
      method: 'POST',
      headers: {
        'Token': GHN_TOKEN,
        'ShopId': String(GHN_SHOP_ID),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        service_type_id: 2, // Giao hàng thường
        to_district_id: params.districtId,
        to_ward_code: params.wardCode,
        weight: params.weight ?? 1500,
        insurance_value: params.insuranceValue,
      }),
    })

    const data = await res.json()

    if (data?.code === 200)
      return data.data.total

    console.warn('[GHN] Lỗi tính phí ship:', data?.message)
    return null
  }
  catch (e) {
    console.error('[GHN] Lỗi kết nối:', e)
    return null
  }
}
