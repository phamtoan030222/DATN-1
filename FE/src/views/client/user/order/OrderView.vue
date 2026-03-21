<template>
  <div class="flex flex-col gap-16px">
    <!-- Filter Card -->
    <NCard class="shadow-sm">
      <NForm inline label-placement="top">
        <n-grid :span="24" :x-gap="24">
          <NFormItemGi :span="8" label="Tìm kiếm" class="lg:col-span-2">
            <NInput v-model:value="stateSearch.searchQuery" placeholder="Nhập mã hóa đơn..." clearable
              @keyup.enter="fetchInvoices">
              <template #prefix>
                <search-outline />
              </template>
            </NInput>
          </NFormItemGi>

          <NFormItemGi :span="7" label="Thời gian">
            <NDatePicker v-model:value="dateRange" end-placeholder="Chọn ngày" start-placeholder="Chọn ngày" type="daterange" clearable :is-date-disabled="disableFutureDate"
              placeholder="Chọn khoảng" @update:value="handleDateRangeChange"/>
          </NFormItemGi>

          <NFormItemGi :span="4" label="Loại HĐ">
            <NSelect v-model:value="stateSearch.loaiHoaDon" :options="LOAI_HOA_DON_OPTIONS" placeholder="Tất cả" clearable/>
          </NFormItemGi>

          <NFormItemGi :span="4" label="Trạng thái">
            <NSelect v-model:value="stateSearch.searchStatus" :options="STATUS_OPTIONS" placeholder="Tất cả" clearable/>
          </NFormItemGi>
          <NFormItemGi :span="1">
            <NTooltip trigger="hover">
              <template #trigger>
                <NButton size="large" circle secondary type="primary" class="transition-all hover:scale-110"
                  @click="resetFilters">
                  <NIcon size="20">
                    <refresh-outline />
                  </NIcon>
                </NButton>
              </template>
              Làm mới bộ lọc
            </NTooltip>
          </NFormItemGi>
        </n-grid>
      </NForm>
    </NCard>

    <div v-for="invoice in invoices" :key="invoice.id" class="bg-white rounded-10px border border-gray-200">

      <!-- Header -->
      <div class="flex justify-between items-center px-20px py-14px border-b border-gray-200">

        <div class="flex items-center gap-10px text-14px">
          <span class="font-semibold text-gray-700">
            Mã đơn: {{ invoice.code }}
          </span>
        </div>

        <div class="flex items-center gap-12px text-14px">
          <span class=" flex items-center gap-4px">
            <n-button type="success" @click="handleClickTracking(invoice)">
              {{ invoice.invoiceStatus !== 5 ? 'Theo dõi đơn hàng' : 'Xem chi tiết đơn hàng' }}
            </n-button>
          </span>
          <span class="text-green-600 flex items-center gap-4px">
            <n-tag :type="getTypeInvoiceStatus(invoice.invoiceStatus)">
              {{ getInvoiceStatusText(invoice.invoiceStatus) }}
            </n-tag>
          </span>

          <span class=" font-semibold">
            <span>Ngày đặt: </span>
            {{ convertTimeMillis(invoice.createDate) }}
          </span>
        </div>

      </div>

      <!-- Product list -->
      <div v-for="detail in invoicesDetails?.[invoice.id]" :key="detail.id"
        class="flex gap-16px px-20px py-16px border-b border-gray-100">

        <!-- image -->
        <img :src="detail.urlImage" class="w-100px object-cover rounded" />

        <!-- info -->
        <div class="flex-1 flex flex-col gap-4px">

          <div class="text-15px font-semibold line-clamp-2">
            {{ detail.nameProductDetail }}
          </div>

          <div class="text-13px text-gray-500">
            <div class="flex flex-wrap text-xs text-gray-500 mt-1 gap-2">
              <span v-if="detail.color" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                detail.color }}</span>
              <span v-if="detail.ram" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                detail.ram }}</span>
              <span v-if="detail.hardDrive" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                detail.hardDrive }}</span>
              <span v-if="detail.cpu" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                detail.cpu }}</span>
              <span v-if="detail.gpu" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                detail.gpu }}</span>
              <span v-if="detail.material" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                detail.material }}</span>
            </div>
          </div>

          <div class="text-13px text-gray-500">
            x{{ detail.quantity }} số lượng
          </div>

        </div>

        <!-- price -->
        <div class="flex flex-col items-end justify-center text-14px">

          <!-- <div class="text-gray-400 line-through">
            {{ formatMoney(detail.price) }}đ
          </div> -->

          <div class=" font-semibold">
            {{ formatMoney(detail.totalAmount) }}đ
          </div>

        </div>

      </div>

      <!-- Footer -->
      <div class="p-2 flex justify-end items-center text-14px">
        <div class="max-w-xs ml-auto text-sm space-y-3">
          <!-- <div class="flex justify-between">
            <span>Tạm tính:</span>
            <span>{{ formatMoney(invoice.totalAmount) }}</span>
          </div>
          <div class="flex justify-between text-red-600">
            <span>Giảm giá:</span>
            <span>-{{ formatMoney(getDiscount(invoice.id)) }}</span>
          </div>
          <div class="flex justify-between">
            <span>Phí vận chuyển:</span>
            <span>{{ formatMoney(invoice.shippingFee) }}</span>
          </div> -->
          <div class="flex justify-between font-bold text-lg p-2">
            <span>Tổng cộng:</span>
            <span class="text-red-600">{{ formatMoney(invoice.totalAmountAfterDecrease) }}</span>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import {
  ClientInvoiceDetailResponse,
  ClientInvoiceDetailsResponse,
  getInvoiceDetails,
  getInvoicesByUser
} from '@/service/api/invoice.api'
import { onMounted, ref } from 'vue'

import _ from 'lodash'
import dayjs from 'dayjs'
import { SelectMixedOption } from 'naive-ui/es/select/src/interface'
import { RefreshOutline, SearchOutline } from '@vicons/ionicons5'

type ClientInvoiceDetailsResponseWithoutIdInvoice =
  Omit<ClientInvoiceDetailsResponse, 'idInvoice'>

const invoices = ref<Array<ClientInvoiceDetailResponse>>([])
const router = useRouter()
const getDiscount = (id: string) => {
  const invoice = invoices.value.find(inv => inv.id === id)
  if (!invoice) return 0
  return invoice.totalAmount - (invoice.totalAmountAfterDecrease ?? invoice.totalAmount)
}

const stateSearch = reactive({
  searchQuery: '',
  searchStatus: null as string | null,
  loaiHoaDon: null as string | null,
  startDate: null as number | null,
  endDate: null as number | null,
})

const dateRange = ref<[number, number] | null>(null)

const invoicesDetails = ref<{
  [key: string]: ClientInvoiceDetailsResponseWithoutIdInvoice[]
} | null>(null)

const fetchInvoices = async () => {
  try {
    const res = await getInvoicesByUser(stateSearch)
    invoices.value = res.data || []

    if (invoices.value.length > 0) {

      const detailsRes = await getInvoiceDetails(
        invoices.value.map(it => it.id)
      )

      const detailsData = detailsRes.data || []

      invoicesDetails.value = detailsData.reduce((acc, detail) => {

        if (!acc[detail.idInvoice]) {
          acc[detail.idInvoice] = []
        }

        acc[detail.idInvoice].push(_.omit(detail, 'idInvoice'))

        return acc

      }, {} as { [key: string]: ClientInvoiceDetailsResponseWithoutIdInvoice[] })

    }

  } catch (e) {

    console.error('Error fetching invoices:', e)
    invoices.value = []

  }
}

function convertTimeMillis(ms: number | undefined | null): string {
  if (ms == null) return ''
  const d = new Date(ms)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${pad(d.getDate())}/${pad(
    d.getMonth() + 1
  )}/${d.getFullYear()}`
}

const formatMoney = (value: number) => {
  return new Intl.NumberFormat('vi-VN').format(value)
}

// function convertTextFromTrangThaiThanhToan(status: string): string {
//   switch (status) {
//     case 'CHUA_THANH_TOAN':
//       return 'Chưa thanh toán'
//     case 'CHO_THANH_TOAN_VNPAY':
//       return 'Chờ thanh toán VNPay'
//     case 'DA_THANH_TOAN':
//       return 'Đã thanh toán'
//     case 'THANH_TOAN_MOT_PHAN':
//       return 'Thanh toán một phần'
//     case 'THANH_TOAN_THAT_BAI':
//       return 'Thanh toán thất bại'
//     default:
//       return ''
//   }
// }

function getInvoiceStatusText(status: number): string {
  switch (status) {
    case 0:
      return 'Chờ xác nhận'
    case 1:
      return 'Đã xác nhận'
    case 2:
      return 'Chờ giao'
    case 3:
      return 'Đang giao'
    case 4:
      return 'Hoàn thành'
    case 5:
      return 'Đã hủy'
    case 6:
      return 'Lưu tạm'
    default:
      return 'Trạng thái không xác định'
  }
}

function getTypeInvoiceStatus(status: number): any {
  switch (status) {
    case 0:
      return 'success'
    case 1:
      return 'success'
    case 2:
      return 'success'
    case 3:
      return 'success'
    case 4:
      return 'success'
    case 5:
      return 'error'
    case 6:
      return 'success'
    default:
      return 'success'
  }
}

const handleClickTracking = (invoice: ClientInvoiceDetailResponse) => {
  router.push({
    name: 'OrderTracking',
    query: {
      q: invoice.id
    }
  })
}

function resetFilters() {
  stateSearch.searchQuery = ''
  stateSearch.searchStatus = null
  stateSearch.loaiHoaDon = null
  dateRange.value = null
  stateSearch.startDate = null
  stateSearch.endDate = null
  fetchInvoices()
}

function disableFutureDate(timestamp: number) {
  return dayjs(timestamp).isAfter(dayjs().endOf('day'))
}

function handleDateRangeChange(value: [number, number] | null) {
  if (value?.length === 2) {
    stateSearch.startDate = dayjs(value[0]).startOf('day').valueOf()
    stateSearch.endDate = dayjs(value[1]).endOf('day').valueOf()
  }
  else {
    stateSearch.startDate = null
    stateSearch.endDate = null
    dateRange.value = null
  }
  fetchInvoices()
}

const LOAI_HOA_DON_OPTIONS = [
  { label: 'Tất cả', value: null },
  { label: 'Online', value: 'ONLINE' },
  { label: 'Online tại quầy', value: 'ONLINE_TAI_QUAY' },
] as SelectMixedOption[]

const STATUS_OPTIONS = [
  { label: 'Tất cả', value: null },
  { label: 'Chờ xác nhận', value: 'CHO_XAC_NHAN' },
  { label: 'Đã xác nhận', value: 'DA_XAC_NHAN' },
  { label: 'Chờ giao', value: 'CHO_GIAO' },
  { label: 'Đang giao', value: 'DANG_GIAO' },
  { label: 'Hoàn thành', value: 'HOAN_THANH' },
  { label: 'Đã hủy', value: 'DA_HUY' },
] as SelectMixedOption[]

watch(
  () => [
    stateSearch.searchQuery,
    stateSearch.searchStatus,
    stateSearch.loaiHoaDon,
  ],
  () => {
    fetchInvoices()
  }
)

onMounted(() => {
  fetchInvoices()
})
</script>

<style scoped></style>