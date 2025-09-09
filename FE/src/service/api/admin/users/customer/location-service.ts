import axios from 'axios'

interface Location {
  code: number
  codename: string
  name_with_type: string
  name: string
  parent_code?: number
}

const provinceByCodename = new Map<string, string>()
const provinceByCode = new Map<number, string>()
const districtByCodename = new Map<string, string>()
const districtByCode = new Map<number, string>()
const wardByCodename = new Map<string, string>()
const wardByCode = new Map<number, string>()

export async function initLocationData() {
  // fetch all lists (you already do this elsewhere)
  const [provincesRes, districtsRes, wardsRes] = await Promise.all([
    axios.get<any[]>('https://provinces.open-api.vn/api/p/'),
    axios.get<any[]>('https://provinces.open-api.vn/api/d/'),
    axios.get<any[]>('https://provinces.open-api.vn/api/w/'),
  ])

  provincesRes.data.forEach(p => {
    const label = p.name_with_type || p.name || ''
    if (p.codename) provinceByCodename.set(p.codename, label)
    provinceByCode.set(Number(p.code), label)
  })

  districtsRes.data.forEach(d => {
    const label = d.name_with_type || d.name || ''
    if (d.codename) districtByCodename.set(d.codename, label)
    districtByCode.set(Number(d.code), label)
  })

  wardsRes.data.forEach(w => {
    const label = w.name_with_type || w.name || ''
    if (w.codename) wardByCodename.set(w.codename, label)
    wardByCode.set(Number(w.code), label)
  })
}

// unified getters accept string (codename or already name) or number (code)
function tidyLabelFromString(s?: string | null) {
  if (!s) return ''
  // if it already looks human (contains space or unicode letters) return as-is
  if (/\p{L}/u.test(s) && /\s/.test(s)) return s
  // fallback: replace underscores/hyphens with spaces and titlecase
  return s.replace(/[_-]+/g, ' ').split(' ')
    .map(p => p ? (p[0].toUpperCase() + p.slice(1)) : '')
    .join(' ')
}

export function getProvinceName(key?: string | number | null) : string {
  if (!key && key !== 0) return ''
  if (typeof key === 'number') {
    return provinceByCode.get(key) || ''
  }
  // key is string: try codename map first, then return it tidied (maybe it's already name)
  return provinceByCodename.get(key) || tidyLabelFromString(key)
}

export function getDistrictName(key?: string | number | null) : string {
  if (!key && key !== 0) return ''
  if (typeof key === 'number') {
    return districtByCode.get(key) || ''
  }
  return districtByCodename.get(key) || tidyLabelFromString(key)
}

export function getWardName(key?: string | number | null) : string {
  if (!key && key !== 0) return ''
  if (typeof key === 'number') {
    return wardByCode.get(key) || ''
  }
  return wardByCodename.get(key) || tidyLabelFromString(key)
}
