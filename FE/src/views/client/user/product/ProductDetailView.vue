<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NDescriptions,
  NDescriptionsItem,
  NEmpty,
  NGrid,
  NGridItem,
  NIcon,
  NModal,
  NRate,
  NSpin,
  NTag,
  useMessage,
  useNotification,
} from 'naive-ui'
import {
  AlertCircleOutline,
  ArrowBack,
  CartOutline,
  CheckmarkCircle,
  Flash,
  RefreshOutline,
  TicketOutline,
} from '@vicons/ionicons5'

// [QUAN TRỌNG] Bỏ các API cũ liên quan đến hóa đơn (themSanPham, getCreateHoaDon...)
import {
  getColors,
  getCPUs,
  getGPUs,
  getHardDrives,
  getImeiProductDetail,
  getProductDetailById,
  getProductDetails,
  getRAMs,
} from '@/service/api/client/product/productDetail.api'

import { getVouchers } from '@/service/api/client/discount/api.voucher'
import type { ADVoucherResponse } from '@/service/api/client/discount/api.voucher'
import { themSanPham } from '@/service/api/client/banhang.api'
import { localStorageAction } from '@/utils'
import { CUSTOMER_CART_ID } from '@/constants/storageKey'

// [QUAN TRỌNG] Import Store mới và Type
// import { CartStore } from '@/utils/cartStore'
// import type { CartItem } from '@/utils/cartStore'

// --- CONFIG ---
const route = useRoute()
const router = useRouter()
const message = useMessage()
const notification = useNotification()

// --- STATE ---
const loading = ref(false)
const loadingCart = ref(false) // Loading giả lập khi thêm vào local

// Product Data
const product = ref<any>(null)
const allVariants = ref<any[]>([])
const selectedImage = ref('')
const quantity = ref(1)

// Options Data
const gpuOptions = ref<any[]>([])
const cpuOptions = ref<any[]>([])
const ramOptions = ref<any[]>([])
const hardDriveOptions = ref<any[]>([])
const colorOptions = ref<any[]>([])

// Selected Options
const selectedGpu = ref<string | null>(null)
const selectedCpu = ref<string | null>(null)
const selectedRam = ref<string | null>(null)
const selectedHardDrive = ref<string | null>(null)
const selectedColor = ref<string | null>(null)

// Stock Check (Vẫn giữ để hiển thị Hết hàng nếu kho = 0)
const isOutOfStock = ref(false)

// Voucher & Timer
const showVoucherModal = ref(false)
const rawVoucherList = ref<ADVoucherResponse[]>([])
const selectedVoucher = ref<ADVoucherResponse | null>(null)
const timeLeft = ref('')
let timerInterval: any = null
const cartId = ref<string | null>(null)

// ==========================================
// 1. LOGIC GIỎ HÀNG MỚI (CLIENT-SIDE)
// ==========================================

async function addToCartAction(isBuyNow: boolean) {
  const res = await themSanPham({
    cartId: cartId.value as string,
    productDetailId: product.value.id,
    quantity: quantity.value,
  })

  if (isBuyNow) {
    message.success('Đang chuyển đến thanh toán...')
    router.push('/checkout')
  }
  else {
    notification.success({
      title: 'Đã thêm vào giỏ hàng!',
      content: `Bạn vừa thêm "${product.value.name}" thành công.`,
      duration: 3000,
      keepAliveOnHover: true,
      action: () =>
        h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => router.push('/cart'),
          },
          { default: () => 'Xem giỏ hàng ngay' },
        ),
    })
  }
}

onMounted(() => {
  cartId.value = localStorageAction.get(CUSTOMER_CART_ID)
})

const handleAddToCart = () => addToCartAction(false)
const handleBuyNow = () => addToCartAction(true)

// ==========================================
// 2. DATA FETCHING (Giữ nguyên logic hiển thị)
// ==========================================

async function fetchData(id: string) {
  loading.value = true
  try {
    const res = await getProductDetailById(id)
    if (res.data) {
      product.value = res.data
      selectedImage.value = product.value.urlImage || 'https://via.placeholder.com/500'

      // Set options
      selectedGpu.value = product.value.idGPU || null
      selectedCpu.value = product.value.idCPU || null
      selectedRam.value = product.value.idRAM || null
      selectedHardDrive.value = product.value.idHardDrive || null
      selectedColor.value = product.value.idColor || null

      await Promise.all([
        loadGlobalOptions(),
        loadAllVariants(product.value.productName || product.value.name),
      ])

      // Check endDate giảm giá
      if (!product.value.endDate && allVariants.value.length > 0) {
        const currentVariant = allVariants.value.find((v: any) => v.id === product.value.id)
        if (currentVariant && currentVariant.endDate) {
          product.value.endDate = currentVariant.endDate
        }
      }
      startCountdown()

      // Check tồn kho
      await checkStock(product.value.id)

      fetchAvailableVouchers()
    }
  }
  catch (error) {
    message.error('Không thể tải thông tin sản phẩm')
  }
  finally {
    loading.value = false
  }
}

// Logic check kho đơn giản hóa (Chỉ cần biết có IMEI nào ACTIVE không)
async function checkStock(productDetailId: string) {
  isOutOfStock.value = false
  try {
    const res = await getImeiProductDetail(productDetailId)
    const arr = Array.isArray(res?.data) ? res.data : (res?.data as any)?.data || []

    // Đếm số lượng IMEI khả dụng (Status = 0 hoặc ACTIVE)
    const count = (arr || []).filter((i: any) => {
      const s = i.status ?? i.imeiStatus
      return s === undefined || s === null || String(s) === '0' || String(s).toUpperCase() === 'ACTIVE'
    }).length

    if (count === 0) {
      isOutOfStock.value = true
    }
  }
  catch (e) {
    console.error('Lỗi check kho', e)
    isOutOfStock.value = true
  }
}

// ... (Giữ nguyên các hàm loadGlobalOptions, loadAllVariants, handleOptionClick...)

async function loadGlobalOptions() {
  const mapOpt = (arr: any) =>
    (arr?.data ?? arr ?? []).map((x: any) => ({
      label: x.label || x.name || x.code,
      value: x.value || x.id,
    }))

  try {
    const [gpus, cpus, rams, hds, colors] = await Promise.all([
      getGPUs(),
      getCPUs(),
      getRAMs(),
      getHardDrives(),
      getColors(),
    ])
    gpuOptions.value = mapOpt(gpus)
    cpuOptions.value = mapOpt(cpus)
    ramOptions.value = mapOpt(rams)
    hardDriveOptions.value = mapOpt(hds)
    colorOptions.value = mapOpt(colors)
  }
  catch (e) {
    console.warn('Lỗi tải thuộc tính chung', e)
  }
}

async function loadAllVariants(productName: string) {
  try {
    const res = await getProductDetails({ page: 1, size: 100, keyword: productName })
    const list = res?.data?.content || res?.data?.data || []
    allVariants.value = list.filter(
      (x: any) => (x.productName || x.product || x.name) === productName,
    )
  }
  catch (e) {
    console.error(e)
  }
}

function isOptionDisabled(type: string, value: string) {
  if (allVariants.value.length === 0)
    return false
  const criteria: any = {
    idGPU: selectedGpu.value,
    idCPU: selectedCpu.value,
    idRAM: selectedRam.value,
    idHardDrive: selectedHardDrive.value,
    idColor: selectedColor.value,
  }
  if (type === 'GPU')
    delete criteria.idGPU
  if (type === 'CPU')
    delete criteria.idCPU
  if (type === 'RAM')
    delete criteria.idRAM
  if (type === 'HDD')
    delete criteria.idHardDrive
  if (type === 'COLOR')
    delete criteria.idColor

  if (type === 'GPU')
    criteria.idGPU = value
  if (type === 'CPU')
    criteria.idCPU = value
  if (type === 'RAM')
    criteria.idRAM = value
  if (type === 'HDD')
    criteria.idHardDrive = value
  if (type === 'COLOR')
    criteria.idColor = value

  const match = allVariants.value.find((v) => {
    return (
      (!criteria.idGPU || v.idGPU === criteria.idGPU)
      && (!criteria.idCPU || v.idCPU === criteria.idCPU)
      && (!criteria.idRAM || v.idRAM === criteria.idRAM)
      && (!criteria.idHardDrive || v.idHardDrive === criteria.idHardDrive)
      && (!criteria.idColor || v.idColor === criteria.idColor)
    )
  })
  return !match
}

async function handleOptionClick() {
  const exactMatch = allVariants.value.find(
    v =>
      (selectedGpu.value ? v.idGPU === selectedGpu.value : true)
      && (selectedCpu.value ? v.idCPU === selectedCpu.value : true)
      && (selectedRam.value ? v.idRAM === selectedRam.value : true)
      && (selectedHardDrive.value ? v.idHardDrive === selectedHardDrive.value : true)
      && (selectedColor.value ? v.idColor === selectedColor.value : true),
  )

  if (exactMatch && exactMatch.id !== product.value.id) {
    await router.replace({ params: { id: exactMatch.id } })
    await fetchData(exactMatch.id)
  }
}

function handleResetFilter() {
  selectedGpu.value = null
  selectedCpu.value = null
  selectedRam.value = null
  selectedHardDrive.value = null
  selectedColor.value = null
  message.info('Đã làm mới bộ lọc')
}

// ==========================================
// 4. PRICE & VOUCHER & TIMER (Giữ nguyên logic tính giá)
// ==========================================

async function fetchAvailableVouchers() {
  try {
    const res = await getVouchers({ page: 1, size: 50, status: 'ACTIVE' })
    const now = new Date().getTime()
    rawVoucherList.value = res.content.filter((v: any) => {
      const isTimeValid = (!v.startDate || v.startDate <= now) && (!v.endDate || v.endDate >= now)
      const hasQuantity = v.remainingQuantity === null || v.remainingQuantity > 0
      return isTimeValid && hasQuantity
    })
    if (validVouchers.value.length > 0) {
      selectedVoucher.value = validVouchers.value[0]
    }
  }
  catch (e) { console.error(e) }
}

const currentPercent = computed(() => Number(product.value?.percentage) || 0)
const listPrice = computed(() => Number(product.value?.price) || 0)
const sellingPrice = computed(() => {
  const percent = currentPercent.value
  return percent <= 0 ? listPrice.value : (listPrice.value * (100 - percent)) / 100
})
const currentOrderTotal = computed(() => sellingPrice.value * quantity.value)

const validVouchers = computed(() => {
  const total = currentOrderTotal.value
  return rawVoucherList.value
    .filter(v => !v.conditions || total >= v.conditions)
    .sort((a, b) => calcDiscount(b, total) - calcDiscount(a, total))
})

function calcDiscount(v: ADVoucherResponse, total: number) {
  if (v.typeVoucher === 'FIXED_AMOUNT')
    return v.discountValue || 0
  let disc = (total * (v.discountValue || 0)) / 100
  if (v.maxValue && disc > v.maxValue)
    disc = v.maxValue
  return disc
}

const voucherDiscountAmount = computed(() => {
  if (!selectedVoucher.value)
    return 0
  const total = currentOrderTotal.value
  if (selectedVoucher.value.conditions && total < selectedVoucher.value.conditions)
    return 0
  return calcDiscount(selectedVoucher.value, total)
})

const finalTotalPrice = computed(() => {
  const total = currentOrderTotal.value - voucherDiscountAmount.value
  return total > 0 ? total : 0
})

function startCountdown() {
  const rawEndDate = product.value?.endDate
  if (!rawEndDate) {
    timeLeft.value = ''
    if (timerInterval)
      clearInterval(timerInterval)
    return
  }
  const endTimestamp = new Date(rawEndDate).getTime()
  if (timerInterval)
    clearInterval(timerInterval)

  const updateTimer = () => {
    const now = new Date().getTime()
    const distance = endTimestamp - now
    if (distance < 0) {
      timeLeft.value = 'ĐÃ KẾT THÚC'
      clearInterval(timerInterval)
      return
    }
    const d = Math.floor(distance / (1000 * 60 * 60 * 24))
    const h = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const m = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60))
    const s = Math.floor((distance % (1000 * 60)) / 1000)
    const f = (n: number) => (n < 10 ? `0${n}` : n)
    timeLeft.value = d > 0 ? `${d} ngày ${f(h)}:${f(m)}:${f(s)}` : `${f(h)}:${f(m)}:${f(s)}`
  }
  updateTimer()
  timerInterval = setInterval(updateTimer, 1000)
}

function formatCurrency(val: number | undefined) {
  return val === undefined ? '0₫' : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

function handleSelectVoucher(v: ADVoucherResponse) {
  if (selectedVoucher.value?.id === v.id) {
    selectedVoucher.value = null
  }
  else {
    selectedVoucher.value = v
    showVoucherModal.value = false
    message.success('Đã áp dụng voucher')
  }
}

watch(() => route.params.id, (newId) => {
  if (newId)
    fetchData(newId as string)
})

onMounted(() => { fetchData(route.params.id as string) })
onUnmounted(() => {
  if (timerInterval)
    clearInterval(timerInterval)
})
</script>

<template>
  <div class="detail-container">
    <div v-if="loading" class="flex justify-center py-20">
      <NSpin size="large" />
    </div>

    <div v-else-if="product" class="main-content">
      <div class="mb-4">
        <NButton text @click="router.back()">
          <template #icon>
            <NIcon>
              <ArrowBack />
            </NIcon>
          </template>
          Quay lại
        </NButton>
      </div>

      <NGrid x-gap="24" cols="1 m:2" responsive="screen">
        <NGridItem>
          <div class="gallery-box relative mb-8">
            <div class="main-image-wrapper border border-gray-100 rounded-lg overflow-hidden bg-white">
              <img :src="selectedImage" class="w-full h-full object-contain p-4"
                @error="selectedImage = 'https://via.placeholder.com/500'">
              <div v-if="currentPercent > 0"
                class="absolute top-4 left-4 bg-red-600 text-white font-bold px-3 py-1 rounded shadow-md z-10 animate-pulse">
                -{{ currentPercent }}%
              </div>
            </div>
          </div>

          <div class="config-table-section">
            <h3 class="font-bold mb-4 border-l-4 border-green-600 pl-3 text-lg text-gray-800">
              Thông số cấu hình chi tiết
            </h3>
            <NDescriptions bordered label-placement="left" size="small" :column="1"
              label-style="width: 140px; font-weight: 600; background-color: #f0fdf4; color: #166534;">
              <NDescriptionsItem label="CPU">
                {{ product.cpuName || product.cpu || product.idCPU || 'Đang cập nhật' }}
              </NDescriptionsItem>

              <NDescriptionsItem label="GPU">
                {{ product.gpuName || product.gpu || product.idGPU || 'Đang cập nhật' }}
              </NDescriptionsItem>

              <NDescriptionsItem label="RAM">
                {{ product.ramName || product.ram || product.idRAM || 'Đang cập nhật' }}
              </NDescriptionsItem>
              <NDescriptionsItem label="Ổ cứng">
                {{
                  product.hardDriveName
                  || product.hardDrive
                  || product.idHardDrive
                  || 'Đang cập nhật'
                }}
              </NDescriptionsItem>
              <NDescriptionsItem label="Màn hình">
                {{ product.screenName || product.screen || product.idScreen || 'Đang cập nhật' }}
              </NDescriptionsItem>
              <NDescriptionsItem label="Màu sắc">
                {{ product.colorName || product.color || product.idColor || 'Đang cập nhật' }}
              </NDescriptionsItem>
              <NDescriptionsItem label="Pin">
                {{ product.batteryName || product.battery || product.idBattery || 'Đang cập nhật' }}
              </NDescriptionsItem>
              <NDescriptionsItem label="Chất liệu">
                {{ product.materialName || product.material || product.idMaterial || 'Đang cập nhật' }}
              </NDescriptionsItem>
              <NDescriptionsItem label="Hệ điều hành">
                {{ product.operatingSystem || product.operatingSystemName || product.idOperatingSystem || 'Đang cập nhật' }}
              </NDescriptionsItem>
            </NDescriptions>
          </div>
        </NGridItem>

        <NGridItem>
          <div class="info-box">
            <h1 class="text-2xl font-bold text-gray-800 mb-2 leading-tight">
              {{ product.productName || product.name }} {{ product.cpuName }} {{ product.ramName }} {{
                product.hardDriveName
              }}
            </h1>
            <div class="flex items-center gap-2 mb-4 text-sm text-gray-500">
              <NRate readonly :default-value="5" size="small" />
              <span>(Mã: {{ product.code }})</span>
            </div>

            <div v-if="currentPercent > 0"
              class="flash-sale-bar bg-gradient-to-r from-red-600 to-orange-500 text-white p-3 rounded-t-lg flex justify-between items-center shadow-md select-none">
              <div class="flex items-center gap-2">
                <NIcon size="24" class="animate-pulse">
                  <Flash />
                </NIcon>
                <span class="font-bold text-lg uppercase tracking-wide">FLASH SALE</span>
              </div>
              <div class="flex items-center gap-2 bg-black/20 px-3 py-1 rounded backdrop-blur-sm">
                <span class="text-xs opacity-90">Kết thúc trong:</span>
                <span class="font-mono font-bold text-lg text-yellow-300">{{ timeLeft }}</span>
              </div>
            </div>

            <div class="price-section bg-red-50/50 p-5 border border-red-100 mb-6 transition-all" :class="{
              'rounded-b-lg border-t-0': currentPercent > 0,
              'rounded-lg': currentPercent <= 0,
            }">
              <div v-if="currentPercent > 0" class="flex items-center gap-2 mb-1">
                <span class="text-gray-400 text-sm">Giá niêm yết:</span>
                <span class="text-gray-400 text-lg line-through font-medium">{{
                  formatCurrency(listPrice)
                  }}</span>
                <span class="text-red-600 text-xs font-bold bg-red-100 px-2 py-0.5 rounded-full">-{{ currentPercent
                  }}%</span>
              </div>
              <div class="flex items-baseline gap-2">
                <span class="text-4xl font-bold text-red-600 tracking-tight">{{
                  formatCurrency(sellingPrice)
                  }}</span>
                <span v-if="currentPercent <= 0" class="text-xs text-gray-500">(Giá đã bao gồm VAT)</span>
              </div>
            </div>

            <div class="config-section mb-6 border border-gray-200 bg-white p-4 rounded-lg shadow-sm relative">
              <div class="flex justify-between items-center mb-3">
                <div class="font-bold text-gray-700">
                  Tuỳ chọn phiên bản
                </div>
                <NButton size="tiny" tertiary round @click="handleResetFilter">
                  <template #icon>
                    <NIcon>
                      <RefreshOutline />
                    </NIcon>
                  </template>
                  Làm mới
                </NButton>
              </div>

              <div class="grid gap-4">
                <div v-if="gpuOptions.some((o) => !isOptionDisabled('GPU', o.value))">
                  <div class="text-xs font-semibold text-gray-500 mb-1 uppercase">
                    GPU
                  </div>
                  <div class="flex flex-wrap gap-2">
                    <button v-for="opt in gpuOptions" :key="opt.value" class="chip" :class="{
                      active: selectedGpu === opt.value,
                      disabled: isOptionDisabled('GPU', opt.value),
                    }" @click="
                        !isOptionDisabled('GPU', opt.value)
                        && ((selectedGpu = opt.value), handleOptionClick())
                        ">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
                <div v-if="cpuOptions.some((o) => !isOptionDisabled('CPU', o.value))">
                  <div class="text-xs font-semibold text-gray-500 mb-1 uppercase">
                    CPU
                  </div>
                  <div class="flex flex-wrap gap-2">
                    <button v-for="opt in cpuOptions" :key="opt.value" class="chip" :class="{
                      active: selectedCpu === opt.value,
                      disabled: isOptionDisabled('CPU', opt.value),
                    }" @click="
                        !isOptionDisabled('CPU', opt.value)
                        && ((selectedCpu = opt.value), handleOptionClick())
                        ">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
                <div v-if="ramOptions.some((o) => !isOptionDisabled('RAM', o.value))">
                  <div class="text-xs font-semibold text-gray-500 mb-1 uppercase">
                    RAM
                  </div>
                  <div class="flex flex-wrap gap-2">
                    <button v-for="opt in ramOptions" :key="opt.value" class="chip" :class="{
                      active: selectedRam === opt.value,
                      disabled: isOptionDisabled('RAM', opt.value),
                    }" @click="
                        !isOptionDisabled('RAM', opt.value)
                        && ((selectedRam = opt.value), handleOptionClick())
                        ">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
                <div v-if="hardDriveOptions.some((o) => !isOptionDisabled('HDD', o.value))">
                  <div class="text-xs font-semibold text-gray-500 mb-1 uppercase">
                    Ổ cứng
                  </div>
                  <div class="flex flex-wrap gap-2">
                    <button v-for="opt in hardDriveOptions" :key="opt.value" class="chip" :class="{
                      active: selectedHardDrive === opt.value,
                      disabled: isOptionDisabled('HDD', opt.value),
                    }" @click="
                        !isOptionDisabled('HDD', opt.value)
                        && ((selectedHardDrive = opt.value), handleOptionClick())
                        ">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
                <div v-if="colorOptions.some((o) => !isOptionDisabled('COLOR', o.value))">
                  <div class="text-xs font-semibold text-gray-500 mb-1 uppercase">
                    Màu sắc
                  </div>
                  <div class="flex flex-wrap gap-2">
                    <button v-for="opt in colorOptions" :key="opt.value" class="chip" :class="{
                      active: selectedColor === opt.value,
                      disabled: isOptionDisabled('COLOR', opt.value),
                    }" @click="
                        !isOptionDisabled('COLOR', opt.value)
                        && ((selectedColor = opt.value), handleOptionClick())
                        ">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div
              class="voucher-section mb-6 border border-dashed border-red-300 bg-red-50 p-4 rounded-lg hover:bg-red-100/50 transition-colors">
              <div class="flex justify-between items-center">
                <div class="flex items-center gap-2 text-red-700 font-bold">
                  <NIcon size="20">
                    <TicketOutline />
                  </NIcon>
                  Mã ưu đãi
                </div>
                <NButton size="small" type="error" ghost @click="showVoucherModal = true">
                  {{ selectedVoucher ? 'Đổi mã khác' : 'Chọn mã giảm giá' }}
                </NButton>
              </div>
              <div v-if="selectedVoucher"
                class="mt-3 bg-white border border-red-200 p-3 rounded flex justify-between items-center shadow-sm">
                <div>
                  <div class="font-bold text-red-600">
                    {{ selectedVoucher.code }}
                  </div>
                  <div class="text-sm text-gray-600">
                    Đã giảm:
                    <span class="font-bold text-red-600">-{{ formatCurrency(voucherDiscountAmount) }}</span>
                  </div>
                </div>
                <NIcon color="#d03050" size="24">
                  <CheckmarkCircle />
                </NIcon>
              </div>
            </div>

            <div class="actions border-t pt-6">
              <div class="flex items-center justify-between mb-6">
                <div class="flex items-center gap-4">
                  <span class="font-medium text-gray-700" />
                </div>
                <div class="text-right">
                  <div class="text-lg text-black-900 mb-1">
                    Tổng thanh toán
                  </div>
                  <div class="text-2xl font-bold text-red-600">
                    {{ formatCurrency(finalTotalPrice) }}
                  </div>
                </div>
              </div>

              <div class="flex gap-4 h-12">
                <NButton v-if="!isOutOfStock" type="primary"
                  class="flex-1 h-full text-lg font-bold shadow-lg shadow-green-200 hover:-translate-y-0.5 transition-transform"
                  color="#059669" :loading="loadingCart" @click="handleBuyNow">
                  MUA NGAY
                </NButton>
                <NButton v-else disabled
                  class="flex-1 h-full text-lg font-bold bg-gray-300 text-gray-500 cursor-not-allowed">
                  <template #icon>
                    <NIcon>
                      <AlertCircleOutline />
                    </NIcon>
                  </template>
                  HẾT HÀNG
                </NButton>
                <NButton strong secondary type="info"
                  class="flex-1 h-full text-lg font-bold hover:-translate-y-0.5 transition-transform"
                  :disabled="isOutOfStock" :loading="loadingCart" @click="handleAddToCart">
                  <template #icon>
                    <NIcon>
                      <CartOutline />
                    </NIcon>
                  </template>
                  THÊM GIỎ
                </NButton>
              </div>
              <div v-if="isOutOfStock" class="text-red-500 text-sm mt-2 text-center italic">
                Sản phẩm hiện tại chưa có sẵn trong kho.
              </div>
            </div>
          </div>
        </NGridItem>
      </NGrid>
    </div>

    <NModal v-model:show="showVoucherModal" preset="card" title="Mã Giảm Giá Khả Dụng" style="width: 500px">
      <div v-if="validVouchers.length === 0" class="text-center py-8">
        <NEmpty description="Tiếc quá! Chưa có mã giảm giá nào phù hợp" />
      </div>
      <div v-else class="space-y-3 p-1 max-h-[400px] overflow-y-auto">
        <div v-for="(v, index) in validVouchers" :key="v.id"
          class="border rounded-lg p-3 cursor-pointer transition-all hover:shadow-md relative bg-white group" :class="{
            'border-red-500 bg-red-50 ring-1 ring-red-200': selectedVoucher?.id === v.id,
            'border-gray-200': selectedVoucher?.id !== v.id,
          }" @click="handleSelectVoucher(v)">
          <div v-if="index === 0"
            class="absolute -top-2 -right-2 bg-yellow-500 text-white text-[10px] px-2 py-0.5 rounded-full shadow-sm z-10 font-bold">
            TỐT NHẤT
          </div>
          <div class="flex justify-between items-start">
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <span class="font-bold text-lg text-red-600 group-hover:text-red-700">{{
                  v.code
                  }}</span>
                <NTag size="tiny" type="error" bordered>
                  {{ v.typeVoucher === 'PERCENTAGE' ? 'Giảm %' : 'Giảm tiền' }}
                </NTag>
              </div>
              <div class="text-sm font-medium text-gray-700 mt-1">
                {{ v.name }}
              </div>
              <div class="mt-2 text-xs text-gray-500 bg-gray-50 p-2 rounded inline-block">
                <div>
                  • Giảm:
                  {{
                    v.typeVoucher === 'PERCENTAGE'
                      ? `${v.discountValue}%`
                      : formatCurrency(v.discountValue || 0)
                  }}
                </div>
                <div v-if="v.maxValue">
                  • Tối đa: {{ formatCurrency(v.maxValue) }}
                </div>
                <div>• Đơn tối thiểu: {{ formatCurrency(v.conditions || 0) }}</div>
              </div>
            </div>
            <div class="flex items-center justify-center h-full pl-3">
              <div class="w-5 h-5 rounded-full border border-gray-300 flex items-center justify-center"
                :class="{ 'bg-red-500 border-red-500': selectedVoucher?.id === v.id }">
                <NIcon v-if="selectedVoucher?.id === v.id" color="white" size="14">
                  <CheckmarkCircle />
                </NIcon>
              </div>
            </div>
          </div>
        </div>
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
  /* background-color: #fff; */
  min-height: 80vh;
}

.main-image-wrapper {
  position: relative;
  padding-top: 75%;
}

.main-image-wrapper img {
  position: absolute;
  top: 0;
  left: 0;
}

.chip {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 12px;
  transition: all 0.2s;
  cursor: pointer;
}

.chip:hover {
  border-color: #10b981;
}

.chip.active {
  border-color: #059669;
  background: #ecfdf5;
  color: #059669;
  font-weight: 600;
}

.chip.disabled {
  opacity: 0.4;
  background: #f3f4f6;
  border-color: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
  pointer-events: none;
  text-decoration: line-through;
}
</style>
