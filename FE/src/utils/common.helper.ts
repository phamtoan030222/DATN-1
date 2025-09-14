// Trong file @/utils/common.helper.ts
export default function formatDate(timestamp: number | string): string {
  if (!timestamp)
    return 'N/A' // Fallback rõ ràng hơn

  // Ép sang number, xử lý string input
  let timeNum = typeof timestamp === 'string' ? Number(timestamp) : timestamp

  // Nếu nhỏ hơn 1e12 thì là giây, cần nhân 1000 để ra milliseconds
  if (timeNum < 1e12) {
    timeNum *= 1000
  }

  const date = new Date(timeNum)
  if (Number.isNaN(date.getTime()))
    return 'N/A'

  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()

  return `${day}/${month}/${year}
            ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

