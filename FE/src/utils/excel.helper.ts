import * as XLSX from 'xlsx'

const notification = useNotification()

export function exportToExcel(excelData: any[], fileName: string) {
  const ws = XLSX.utils.json_to_sheet(excelData)
  ws['!cols'] = [{ wch: 5 }, { wch: 15 }, { wch: 25 }, { wch: 10 }, { wch: 15 }, { wch: 25 }, { wch: 40 }, { wch: 15 }]

  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Data')
  XLSX.writeFile(wb, `${fileName}_${new Date().getTime()}.xlsx`)
  notification.success({content: 'Xuất file Excel thành công', duration: 3000})
}