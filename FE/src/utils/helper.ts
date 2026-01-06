import dayjs from 'dayjs'

export function toBlob(data: any, options: BlobPropertyBag = { type: 'application/json' }): Blob {
  return new Blob([JSON.stringify(data)], options)
}

// Chuyển Mili giây -> Năm (VD: 1672531200000 -> 2023)
export function convertMsToYear(ms: number | null | undefined): number {
  if (!ms)
    return new Date().getFullYear() // Hoặc trả về 0 tùy logic
  return new Date(ms).getFullYear()
}

// Chuyển Năm -> Mili giây (VD: 2023 -> 1672531200000) để hiển thị lên DatePicker
export function convertYearToMs(year: number | null | undefined): number {
  if (!year)
    return Date.now()
  // Tạo ngày 1/1 của năm đó
  return new Date(year, 0, 1).getTime()
}
