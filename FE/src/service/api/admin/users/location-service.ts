import axios from 'axios'

const provinceByCodename = new Map<string, string>()
const provinceByCode = new Map<number, string>()
const districtByCodename = new Map<string, string>()
const districtByCode = new Map<number, string>()
const wardByCodename = new Map<string, string>()
const wardByCode = new Map<number, string>()

export async function initLocationData() {
  try {
    const [provincesRes, districtsRes, wardsRes] = await Promise.all([
      axios.get<any[]>('https://provinces.open-api.vn/api/p/'),
      axios.get<any[]>('https://provinces.open-api.vn/api/d/'),
      axios.get<any[]>('https://provinces.open-api.vn/api/w/'),
    ])

    provincesRes.data.forEach((p) => {
      const label = p.name_with_type || p.name || ''
      if (p.codename)
        provinceByCodename.set(p.codename, label)
      provinceByCode.set(Number(p.code), label)
    })

    districtsRes.data.forEach((d) => {
      const label = d.name_with_type || d.name || ''
      if (d.codename)
        districtByCodename.set(d.codename, label)
      districtByCode.set(Number(d.code), label)
    })

    wardsRes.data.forEach((w) => {
      const label = w.name_with_type || w.name || ''
      if (w.codename)
        wardByCodename.set(w.codename, label)
      wardByCode.set(Number(w.code), label)
    })
  }
  catch (error) {
    console.error('Lỗi tải dữ liệu địa chính:', error)
  }
}

function tidyLabelFromString(s?: string | null) {
  if (!s)
    return ''
  if (/\p{L}/u.test(s) && /\s/.test(s))
    return s
  return s.replace(/[_-]+/g, ' ').split(' ').map(p => p ? (p[0].toUpperCase() + p.slice(1)) : '').join(' ')
}

// --- HÀM HELPER ĐỂ TÌM TÊN THÔNG MINH HƠN ---
function getNameFromMap(
  key: string | number | null | undefined,
  mapByCode: Map<number, string>,
  mapByCodename: Map<string, string>,
): string {
  if (!key)
    return ''

  // 1. Nếu key là số, tìm trực tiếp trong Map Code
  if (typeof key === 'number') {
    return mapByCode.get(key) || ''
  }

  // 2. Nếu key là chuỗi
  if (typeof key === 'string') {
    // 2a. Thử ép kiểu sang số để tìm trong Map Code (Fix lỗi của bạn ở đây)
    // Ví dụ: key="01" -> num=1 -> tìm thấy Hà Nội
    const num = Number(key)
    if (!isNaN(num) && mapByCode.has(num)) {
      return mapByCode.get(num) || ''
    }

    // 2b. Nếu không phải số, tìm trong Map Codename (ví dụ: "ha_noi")
    if (mapByCodename.has(key)) {
      return mapByCodename.get(key) || ''
    }

    // 2c. Nếu vẫn không thấy, trả về chuỗi gốc đã làm đẹp (fallback)
    return tidyLabelFromString(key)
  }

  return ''
}

// --- CÁC HÀM EXPORT SỬ DỤNG LOGIC MỚI ---

export function getProvinceName(key?: string | number | null): string {
  return getNameFromMap(key, provinceByCode, provinceByCodename)
}

export function getDistrictName(key?: string | number | null): string {
  return getNameFromMap(key, districtByCode, districtByCodename)
}

export function getWardName(key?: string | number | null): string {
  return getNameFromMap(key, wardByCode, wardByCodename)
}
