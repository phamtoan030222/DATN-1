<script setup lang="ts">
import {
  AlertCircleOutline,
  ArrowBack,
  CartOutline,
  CheckmarkCircle,
  CheckmarkCircleOutline,
  Flash,
  GiftOutline,
  HeadsetOutline,
  LocationOutline,
  RefreshOutline,
  ReturnDownBackOutline,
  RocketOutline,
  ShieldCheckmarkOutline,
  TimeOutline,
} from '@vicons/ionicons5'
import {
  NAlert,
  NAvatar,
  NBadge,
  NButton,
  NDescriptions,
  NDescriptionsItem,
  NDivider,
  NEmpty,
  NGrid,
  NGridItem,
  NIcon,
  NInputNumber,
  NModal,
  NPopconfirm,
  NRate,
  NSpace,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'

import {
  getColorsByPD,
  getCpuByPD,
  getGpuByPD,
  getHardDriveByPD,
  getImeiProductDetail,
  getProductDetailById,
  getProductDetails,
  getRamByPD,
  getSPCTBy,
} from '@/service/api/client/product/productDetail.api'

import type { ADVoucherResponse } from '@/service/api/client/discount/api.voucher'
import { getVouchers } from '@/service/api/client/discount/api.voucher'
import { useCartStore } from '@/store/app/cart'

// --- CONFIG ---
const route = useRoute()
const router = useRouter()
const message = useMessage()

// --- STATE ---
const loading = ref(false)
const loadingCart = ref(false)
const isFavorite = ref(false)
const activeTab = ref('description')

// Khởi tạo Store và lấy dữ liệu giỏ hàng
const cartStore = useCartStore()
const { cartItems } = storeToRefs(cartStore)
const { addToCart } = cartStore

// Product Data
const product = ref<any>(null)
const allVariants = ref<any[]>([])
const selectedImage = ref('')
const quantity = ref(1)

// Related Products
const relatedProducts = ref<any[]>([])

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

// Stock Check
const isOutOfStock = ref(false)
const stockQuantity = ref(0)

// Voucher & Timer
const showVoucherModal = ref(false)
const rawVoucherList = ref<ADVoucherResponse[]>([])
const selectedVoucher = ref<ADVoucherResponse | null>(null)
const timeLeft = ref('')
let timerInterval: any = null

// Sale Status
const isFlashSaleEnded = ref(false)
const isUpcomingSale = ref(false)
const isOngoingSale = ref(false)

// ==========================================
// 1. LOGIC GIỎ HÀNG
// ==========================================

function validateCartAddition() {
  const existingItem = cartItems.value.find((item: any) => item.productDetailId === product.value.id)
  const qtyInCart = existingItem ? existingItem.quantity : 0
  const totalWanted = quantity.value + qtyInCart

  if (totalWanted > stockQuantity.value) {
    if (qtyInCart > 0) {
      message.error(`Kho chỉ còn ${stockQuantity.value} sản phẩm. Bạn đã có ${qtyInCart} sản phẩm trong giỏ!`)
    }
    else {
      message.error(`Rất tiếc, kho chỉ còn ${stockQuantity.value} sản phẩm!`)
    }
    return false
  }

  if (totalWanted > 5) {
    if (qtyInCart > 0) {
      message.error(`Tối đa 5 sản phẩm/đơn. Bạn đã có ${qtyInCart} máy cấu hình này trong giỏ!`)
    }
    else {
      message.error(`Chỉ được mua tối đa 5 sản phẩm cho mỗi cấu hình!`)
    }
    return false
  }

  return true
}

async function handleAddToCart() {
  if (!validateCartAddition())
    return

  loadingCart.value = true
  try {
    await addToCart(product.value.id, quantity.value)
    message.success('Đã thêm sản phẩm vào giỏ hàng')
  }
  catch (error: any) {
    message.error(error.message || 'Không thể thêm vào giỏ hàng')
  }
  finally {
    loadingCart.value = false
  }
}

async function handleBuyNow() {
  if (!validateCartAddition())
    return

  loadingCart.value = true
  try {
    await addToCart(product.value.id, quantity.value)
    router.push({ name: 'Cart' })
  }
  catch (error: any) {
    message.error(error.message || 'Không thể thực hiện mua ngay')
  }
  finally {
    loadingCart.value = false
  }
}

// ==========================================
// 2. DATA FETCHING
// ==========================================

async function fetchRelatedProducts(idProduct: string) {
  try {
    const res = await getProductDetails({
      page: 1,
      size: 6,
      idProduct: [idProduct],
      minPrice: 0,
      maxPrice: 1000000000,
    })

    if (res?.data) {
      const svResponse = res.data
      let list: any[] = []

      if (svResponse.data && !Array.isArray(svResponse.data) && (svResponse.data as any).data)
        list = (svResponse.data as any).data
      else if (Array.isArray(svResponse.data))
        list = svResponse.data
      else if (Array.isArray(svResponse))
        list = svResponse

      relatedProducts.value = list.filter(p => p.id !== product.value.id).slice(0, 6)
    }
  }
  catch (e) {
    console.error('Lỗi tải sản phẩm liên quan', e)
  }
}

async function fetchData(id: string) {
  loading.value = true
  try {
    const res = await getProductDetailById(id)
    if (res.data) {
      product.value = res.data

      if (route.query.pct)
        product.value.percentage = Number(route.query.pct)
      if (route.query.sd)
        product.value.startDate = Number(route.query.sd)
      if (route.query.ed)
        product.value.endDate = Number(route.query.ed)

      selectedImage.value = product.value.urlImage || 'https://via.placeholder.com/500'

      selectedGpu.value = product.value.idGPU || null
      selectedCpu.value = product.value.idCPU || null
      selectedRam.value = product.value.idRAM || null
      selectedHardDrive.value = product.value.idHardDrive || null
      selectedColor.value = product.value.idColor || null

      await Promise.all([
        loadGlobalOptions(),
        loadAllVariants(product.value.idProduct),
        fetchRelatedProducts(product.value.idProduct),
      ])

      if (!product.value.endDate && !route.query.ed && allVariants.value.length > 0) {
        const currentVariant = allVariants.value.find((v: any) => v.id === product.value.id)
        if (currentVariant && currentVariant.endDate) {
          product.value.endDate = currentVariant.endDate
          product.value.startDate = currentVariant.startDate
          product.value.percentage = currentVariant.percentage
        }
      }

      startCountdown()
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

async function checkStock(productDetailId: string) {
  isOutOfStock.value = false
  stockQuantity.value = 0
  try {
    const res = await getImeiProductDetail(productDetailId)
    const arr = Array.isArray(res?.data) ? res.data : (res?.data as any)?.data || []

    const count = (arr || []).filter((i: any) => {
      const s = i.status ?? i.imeiStatus
      return s === undefined || s === null || String(s) === '0' || String(s).toUpperCase() === 'ACTIVE'
    }).length

    stockQuantity.value = count
    if (count === 0) {
      isOutOfStock.value = true
    }
  }
  catch (e) {
    console.error('Lỗi check kho', e)
    isOutOfStock.value = true
  }
}

async function loadGlobalOptions() {
  const mapOpt = (res: any) => {
    let arr: any[] = []
    if (res?.data) {
      if (Array.isArray(res.data))
        arr = res.data
      else if (res.data.data && Array.isArray(res.data.data))
        arr = res.data.data
      else if (res.data.content && Array.isArray(res.data.content))
        arr = res.data.content
    }
    else if (Array.isArray(res)) {
      arr = res
    }
    return arr.map((x: any) => ({
      label: x.label || x.name || x.code,
      value: x.value || x.id,
    }))
  }

  try {
    const [gpus, cpus, rams, hds, colors] = await Promise.all([
      getGpuByPD(product.value.id),
      getCpuByPD(product.value.id),
      getRamByPD(product.value.id),
      getHardDriveByPD(product.value.id),
      getColorsByPD(product.value.id),
    ])
    gpuOptions.value = mapOpt(gpus)
    cpuOptions.value = mapOpt(cpus)
    ramOptions.value = mapOpt(rams)
    hardDriveOptions.value = mapOpt(hds)
    colorOptions.value = mapOpt(colors)
  }
  catch (e) { console.warn('Lỗi tải thuộc tính chung', e) }
}

async function loadAllVariants(idProduct: string) {
  try {
    if (!idProduct)
      return
    const res = await getProductDetails({ page: 1, size: 100, idProduct: [idProduct], minPrice: 0, maxPrice: 1000000000 })
    let list: any[] = []
    if (res?.data) {
      const svResponse = res.data
      if (svResponse.data && !Array.isArray(svResponse.data) && (svResponse.data as any).data)
        list = (svResponse.data as any).data
      else if (Array.isArray(svResponse.data))
        list = svResponse.data
      else if (Array.isArray(svResponse))
        list = svResponse
    }
    allVariants.value = list
  }
  catch (e) { console.error('Lỗi tải variants', e) }
}

async function selectVariantOption(type: string, val: string) {
  if (type === 'CPU')
    selectedCpu.value = val
  if (type === 'GPU')
    selectedGpu.value = val
  if (type === 'RAM')
    selectedRam.value = val
  if (type === 'HDD')
    selectedHardDrive.value = val
  if (type === 'COLOR')
    selectedColor.value = val

  loading.value = true

  try {
    const res = await getSPCTBy({
      idProduct: product.value.idProduct,
      idCpu: selectedCpu.value,
      idGpu: selectedGpu.value,
      idRam: selectedRam.value,
      idHardDrive: selectedHardDrive.value,
      idColor: selectedColor.value,
    })

    if (res.data && res.data.id) {
      if (res.data.id !== product.value.id) {
        await router.replace({ params: { id: res.data.id }, query: route.query })
      }
      else {
        loading.value = false
      }
    }
    else {
      message.warning('Phiên bản cấu hình này hiện không khả dụng.')
      loading.value = false
    }
  }
  catch (error) {
    message.error('Không tìm thấy cấu hình phù hợp với lựa chọn của bạn.')
    loading.value = false
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
// 3. PRICE & VOUCHER & TIMER
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
  }
  catch (e) { console.error(e) }
}

const rawPercent = computed(() => Number(product.value?.percentage) || 0)
const currentPercent = computed(() => isOngoingSale.value ? rawPercent.value : 0)

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

function startCountdown() {
  if (timerInterval)
    clearInterval(timerInterval)

  const rawStartDate = product.value?.startDate
  const rawEndDate = product.value?.endDate

  const parseSafeDate = (dateStr: any) => {
    if (!dateStr)
      return 0
    const safeStr = typeof dateStr === 'string' ? dateStr.replace(/-/g, '/') : dateStr
    const timestamp = new Date(safeStr).getTime()
    return Number.isNaN(timestamp) ? 0 : timestamp
  }

  const startTimestamp = parseSafeDate(rawStartDate)
  const endTimestamp = parseSafeDate(rawEndDate)

  if (!endTimestamp) {
    isFlashSaleEnded.value = true
    isUpcomingSale.value = false
    isOngoingSale.value = false
    timeLeft.value = ''
    return
  }

  const updateTimer = () => {
    const now = new Date().getTime()
    let distance = 0

    if (now < startTimestamp) {
      isUpcomingSale.value = true; isOngoingSale.value = false; isFlashSaleEnded.value = false
      distance = startTimestamp - now
    }
    else if (now >= startTimestamp && now <= endTimestamp) {
      isUpcomingSale.value = false; isOngoingSale.value = true; isFlashSaleEnded.value = false
      distance = endTimestamp - now
    }
    else {
      isUpcomingSale.value = false; isOngoingSale.value = false; isFlashSaleEnded.value = true
      clearInterval(timerInterval); timeLeft.value = ''
      return
    }

    const d = Math.floor(distance / (1000 * 60 * 60 * 24))
    const h = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const m = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60))
    const s = Math.floor((distance % (1000 * 60)) / 1000)

    const f = (n: number) => n.toString().padStart(2, '0')
    timeLeft.value = d > 0 ? `${d} Ngày ${f(h)}:${f(m)}:${f(s)}` : `${f(h)}:${f(m)}:${f(s)}`
  }

  updateTimer()
  if (!isFlashSaleEnded.value)
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
  <div class="product-detail-page">
    <div class="container">
      <div class="breadcrumb">
        <span class="breadcrumb-item" @click="router.push('/')">Trang chủ</span>
        <span class="breadcrumb-separator">/</span>
        <span class="breadcrumb-item" @click="router.push('/san-pham')">Sản phẩm</span>
        <span class="breadcrumb-separator">/</span>
        <span class="breadcrumb-item active">{{ product?.name || 'Chi tiết sản phẩm' }}</span>
      </div>

      <div v-if="loading" class="loading-state">
        <NSpin size="large" />
        <p>Đang tải thông tin sản phẩm...</p>
      </div>

      <div v-else-if="product" class="product-wrapper">
        <div class="product-main">
          <div class="product-gallery">
            <div class="main-image">
              <img :src="selectedImage" :alt="product.name" @error="selectedImage = 'https://via.placeholder.com/500'">
              <div class="image-badges">
                <span v-if="isOngoingSale && rawPercent > 0" class="badge discount">-{{ rawPercent }}%</span>
                <span v-if="isUpcomingSale && rawPercent > 0" class="badge upcoming">Sắp giảm</span>
                <span v-if="isOutOfStock" class="badge out-of-stock">Hết hàng</span>
              </div>
            </div>
          </div>

          <div class="product-info">
            <div class="product-header">
              <h1 class="product-title">
                {{ product.name }}
              </h1>
              <div class="product-meta">
                <span class="product-code">Mã: {{ product.code }}</span>
              </div>
            </div>

            <div
              v-if="isOngoingSale || isUpcomingSale" class="sale-timer"
              :class="{ ongoing: isOngoingSale, upcoming: isUpcomingSale }"
            >
              <div class="timer-left">
                <NIcon size="24" :class="{ 'animate-pulse': isOngoingSale, 'animate-bounce': isUpcomingSale }">
                  <Flash v-if="isOngoingSale" />
                  <TimeOutline v-else />
                </NIcon>
                <div class="timer-text">
                  <span class="timer-label">{{ isOngoingSale ? 'FLASH SALE' : 'SẮP DIỄN RA' }}</span>
                  <span class="timer-sub">{{ isOngoingSale ? 'Kết thúc trong' : 'Bắt đầu sau' }}</span>
                </div>
              </div>
              <div class="timer-display">
                <span class="time-value">{{ timeLeft }}</span>
              </div>
            </div>

            <div class="price-section" :class="{ 'has-sale': isOngoingSale || isUpcomingSale }">
              <div class="price-wrapper">
                <div v-if="isOngoingSale && rawPercent > 0" class="price-old">
                  <span class="label">Giá niêm yết:</span>
                  <span class="value">{{ formatCurrency(listPrice) }}</span>
                </div>
                <div class="price-current">
                  <span class="label">Giá bán:</span>
                  <span class="value" :class="{ 'sale-price': isOngoingSale }">{{ formatCurrency(sellingPrice) }}</span>
                  <span v-if="isUpcomingSale && rawPercent > 0" class="upcoming-tag">Dự kiến giảm {{ rawPercent }}%</span>
                </div>
              </div>
            </div>

            <div class="short-specs">
              <div v-if="product.cpu" class="spec-item">
                <span class="spec-label">CPU:</span>
                <span class="spec-value">{{ product.cpu }}</span>
              </div>
              <div v-if="product.ram" class="spec-item">
                <span class="spec-label">RAM:</span>
                <span class="spec-value">{{ product.ram }}</span>
              </div>
              <div v-if="product.hardDrive" class="spec-item">
                <span class="spec-label">Ổ cứng:</span>
                <span class="spec-value">{{ product.hardDrive }}</span>
              </div>
              <div v-if="product.gpu" class="spec-item">
                <span class="spec-label">VGA:</span>
                <span class="spec-value">{{ product.gpu }}</span>
              </div>
              <div v-if="product.screen" class="spec-item">
                <span class="spec-label">Màn hình:</span>
                <span class="spec-value">{{ product.screen }}</span>
              </div>
            </div>

            <div
              v-if="cpuOptions.length > 0 || gpuOptions.length > 0 || ramOptions.length > 0 || hardDriveOptions.length > 0 || colorOptions.length > 0"
              class="variants-section"
            >
              <div class="variants-header">
                <h3 class="variants-title">
                  Tuỳ chọn phiên bản
                </h3>
                <button class="reset-filter-btn" @click="handleResetFilter">
                  <NIcon size="14">
                    <RefreshOutline />
                  </NIcon>
                  Làm mới
                </button>
              </div>

              <div class="variants-grid">
                <div v-if="cpuOptions.length > 0" class="variant-group">
                  <span class="variant-label">CPU:</span>
                  <div class="variant-options">
                    <button
                      v-for="opt in cpuOptions" :key="opt.value" class="variant-chip"
                      :class="{ active: selectedCpu === opt.value }" @click="selectVariantOption('CPU', opt.value)"
                    >
                      {{ opt.label }}
                    </button>
                  </div>
                </div>

                <div v-if="ramOptions.length > 0" class="variant-group">
                  <span class="variant-label">RAM:</span>
                  <div class="variant-options">
                    <button
                      v-for="opt in ramOptions" :key="opt.value" class="variant-chip"
                      :class="{ active: selectedRam === opt.value }" @click="selectVariantOption('RAM', opt.value)"
                    >
                      {{ opt.label }}
                    </button>
                  </div>
                </div>

                <div v-if="hardDriveOptions.length > 0" class="variant-group">
                  <span class="variant-label">Ổ cứng:</span>
                  <div class="variant-options">
                    <button
                      v-for="opt in hardDriveOptions" :key="opt.value" class="variant-chip"
                      :class="{ active: selectedHardDrive === opt.value }"
                      @click="selectVariantOption('HDD', opt.value)"
                    >
                      {{ opt.label }}
                    </button>
                  </div>
                </div>

                <div v-if="gpuOptions.length > 0" class="variant-group">
                  <span class="variant-label">VGA:</span>
                  <div class="variant-options">
                    <button
                      v-for="opt in gpuOptions" :key="opt.value" class="variant-chip"
                      :class="{ active: selectedGpu === opt.value }" @click="selectVariantOption('GPU', opt.value)"
                    >
                      {{ opt.label }}
                    </button>
                  </div>
                </div>

                <div v-if="colorOptions.length > 0" class="variant-group">
                  <span class="variant-label">Màu sắc:</span>
                  <div class="variant-options">
                    <button
                      v-for="opt in colorOptions" :key="opt.value" class="variant-chip"
                      :class="{ active: selectedColor === opt.value }" @click="selectVariantOption('COLOR', opt.value)"
                    >
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div class="actions-section">
              <div class="quantity-box">
                <span class="quantity-label">Số lượng:</span>
                <NInputNumber
                  v-model:value="quantity" :min="1" :max="Math.min(5, stockQuantity)"
                  :disabled="isOutOfStock" button-placement="both" size="large"
                />
                <div v-if="stockQuantity > 0" class="stock-info">
                  <NIcon size="16" color="#16a34a">
                    <CheckmarkCircleOutline />
                  </NIcon>
                  <span>Còn <strong>{{ stockQuantity }}</strong> sản phẩm</span>
                </div>
              </div>

              <div class="action-buttons">
                <NButton
                  v-if="!isOutOfStock" size="large" class="btn-buy-now" :loading="loadingCart"
                  @click="handleBuyNow"
                >
                  <template #icon>
                    <NIcon>
                      <RocketOutline />
                    </NIcon>
                  </template>
                  MUA NGAY
                </NButton>
                <NButton v-else size="large" class="btn-disabled" disabled>
                  <template #icon>
                    <NIcon>
                      <AlertCircleOutline />
                    </NIcon>
                  </template>
                  HẾT HÀNG
                </NButton>

                <NButton
                  size="large" class="btn-add-cart" :disabled="isOutOfStock" :loading="loadingCart"
                  @click="handleAddToCart"
                >
                  <template #icon>
                    <NIcon>
                      <CartOutline />
                    </NIcon>
                  </template>
                  Thêm giỏ hàng
                </NButton>
              </div>
            </div>
          </div>

          <div class="store-sidebar">
            <div class="promise-card">
              <div class="promise-item">
                <NIcon size="18" color="#18a058">
                  <ShieldCheckmarkOutline />
                </NIcon>
                <span>Sản phẩm chính hãng 100%</span>
              </div>
              <div class="promise-item">
                <NIcon size="18" color="#18a058">
                  <ReturnDownBackOutline />
                </NIcon>
                <span>Đổi trả miễn phí trong 30 ngày</span>
              </div>
              <div class="promise-item">
                <NIcon size="18" color="#18a058">
                  <HeadsetOutline />
                </NIcon>
                <span>Hỗ trợ kỹ thuật 24/7</span>
              </div>
              <div class="promise-item">
                <NIcon size="18" color="#18a058">
                  <LocationOutline />
                </NIcon>
                <span>Giao hàng cực nhanh toàn quốc</span>
              </div>
            </div>

            <div class="voucher-card" @click="showVoucherModal = true">
              <div class="voucher-header">
                <NIcon size="20" color="#18a058">
                  <GiftOutline />
                </NIcon>
                <span>Mã giảm giá khả dụng</span>
              </div>
              <div class="voucher-preview">
                <span v-if="validVouchers.length > 0" class="voucher-count">
                  {{ validVouchers.length }} mã khả dụng
                </span>
                <span v-else class="voucher-count">Hiện chưa có mã</span>
                <NIcon size="16">
                  <ArrowBack style="transform: rotate(180deg)" />
                </NIcon>
              </div>
            </div>
          </div>
        </div>

        <div class="product-tabs">
          <NTabs v-model:value="activeTab" type="line" animated>
            <NTabPane name="description" tab="Thông số kỹ thuật">
              <div class="tab-content">
                <NDescriptions
                  bordered :column="1" size="medium"
                  label-style="width: 180px; font-weight: 600; background-color: #f8fafc;"
                >
                  <NDescriptionsItem label="CPU">
                    {{ product.cpuName || product.cpu || 'Đang cập nhật' }}
                  </NDescriptionsItem>
                  <NDescriptionsItem label="RAM">
                    {{ product.ramName || product.ram || 'Đang cập nhật' }}
                  </NDescriptionsItem>
                  <NDescriptionsItem label="Ổ cứng">
                    {{ product.hardDriveName || product.hardDrive || 'Đang cập nhật' }}
                  </NDescriptionsItem>
                  <NDescriptionsItem label="Card đồ họa (GPU)">
                    {{ product.gpuName || product.gpu || 'Đang cập nhật' }}
                  </NDescriptionsItem>
                  <NDescriptionsItem label="Màn hình">
                    {{ product.screenName || product.screen || 'Đang cập nhật' }}
                  </NDescriptionsItem>
                  <NDescriptionsItem label="Màu sắc">
                    {{ product.colorName || product.color || 'Đang cập nhật' }}
                  </NDescriptionsItem>
                  <NDescriptionsItem label="Pin">
                    {{ product.batteryName || product.battery || 'Đang cập nhật' }}
                  </NDescriptionsItem>
                  <NDescriptionsItem label="Hệ điều hành">
                    {{ product.operatingSystem || 'Đang cập nhật' }}
                  </NDescriptionsItem>
                </NDescriptions>
              </div>
            </NTabPane>
          </NTabs>
        </div>

        <div v-if="relatedProducts.length > 0" class="related-products">
          <h2 class="section-title">
            Khám phá thêm các máy tương tự
          </h2>
          <div class="related-grid">
            <div
              v-for="item in relatedProducts" :key="item.id" class="related-card"
              @click="router.push(`/product-detail/${item.id}`)"
            >
              <div class="card-image">
                <img :src="item.urlImage" :alt="item.name">
                <div v-if="item.percentage" class="card-badge">
                  -{{ item.percentage }}%
                </div>
              </div>
              <div class="card-info">
                <h3 class="card-name">
                  {{ item.name }}
                </h3>
                <div class="card-price">
                  <span class="current">{{ formatCurrency(item.price) }}</span>
                  <span v-if="item.percentage" class="old">{{ formatCurrency(item.price) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <NModal v-model:show="showVoucherModal" preset="card" title="Mã giảm giá" style="width: 500px">
      <div v-if="validVouchers.length === 0" class="empty-voucher">
        <NEmpty description="Rất tiếc, chưa có mã phù hợp với đơn hàng" />
      </div>
      <div v-else class="voucher-list">
        <div
          v-for="(v, index) in validVouchers" :key="v.id" class="voucher-item"
          :class="{ selected: selectedVoucher?.id === v.id }" @click="handleSelectVoucher(v)"
        >
          <div v-if="index === 0" class="voucher-badge">
            TỐT NHẤT
          </div>
          <div class="voucher-left">
            <span class="voucher-code">{{ v.code }}</span>
            <span class="voucher-name">{{ v.name }}</span>
            <div class="voucher-desc">
              <span>Giảm: {{ v.typeVoucher === 'PERCENTAGE' ? `${v.discountValue}%` : formatCurrency(v.discountValue
                || 0)
              }}</span>
              <span v-if="v.maxValue"> • Tối đa: {{ formatCurrency(v.maxValue) }}</span>
            </div>
            <span class="voucher-condition">Đơn tối thiểu: {{ formatCurrency(v.conditions || 0) }}</span>
          </div>
          <div class="voucher-right">
            <div class="voucher-check" :class="{ checked: selectedVoucher?.id === v.id }">
              <NIcon v-if="selectedVoucher?.id === v.id" color="white" size="14">
                <CheckmarkCircle />
              </NIcon>
            </div>
          </div>
        </div>
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.product-detail-page {
  background-color: #f5f5f5;
  min-height: 100vh;
  padding: 20px 0 40px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}

/* Breadcrumb */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}

.breadcrumb-item {
  cursor: pointer;
  transition: color 0.2s ease;
}

.breadcrumb-item:hover {
  color: #18a058; /* Chuyển hover sang Xanh lá */
}

.breadcrumb-item.active {
  color: #18a058; /* Chuyển active sang Xanh lá */
  font-weight: 500;
}

.breadcrumb-separator {
  color: #999;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: white;
  border-radius: 12px;
}

.loading-state p {
  margin-top: 16px;
  color: #666;
}

/* Product Main Layout */
.product-main {
  display: grid;
  grid-template-columns: 400px 1fr 280px;
  gap: 20px;
  margin-bottom: 30px;
}

/* Gallery Section */
.product-gallery {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.main-image {
  position: relative;
  padding-top: 100%;
  background: #fafafa;
  border-radius: 8px;
  overflow: hidden;
}

.main-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.image-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 8px;
  z-index: 2;
}

.badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
  color: white;
}

.badge.discount {
  background: #18a058;
}

.badge.upcoming {
  background: #2563eb;
}

.badge.out-of-stock {
  background: #6b7280;
}

/* Product Info */
.product-info {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.product-header {
  margin-bottom: 20px;
}

.product-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
  line-height: 1.3;
}

.product-code {
  font-size: 14px;
  color: #666;
}

/* Sale Timer */
.sale-timer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  color: white;
}

.sale-timer.ongoing {
  background: linear-gradient(135deg, #18a058, #16f92d);
}

.sale-timer.upcoming {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
}

.timer-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.timer-text {
  display: flex;
  flex-direction: column;
}

.timer-label {
  font-weight: 700;
  font-size: 16px;
}

.timer-sub {
  font-size: 12px;
  opacity: 0.9;
}

.timer-display {
  background: rgba(0, 0, 0, 0.2);
  padding: 8px 16px;
  border-radius: 40px;
  backdrop-filter: blur(4px);
}

.time-value {
  font-weight: 700;
  font-size: 18px;
  font-family: monospace;
}

/* Price Section */
.price-section {
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 20px;
}

/* Chuyển nền đỏ thành nền xanh lá nhạt cho khu vực Sale */
.price-section.has-sale {
  background: #f0fdf4;
}

.price-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-old {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.price-old .value {
  text-decoration: line-through;
  color: #999;
}

.price-current {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.price-current .value {
  font-size: 28px;
  font-weight: 700;
}

.price-current .value.sale-price {
  color: #18a058;
}

.upcoming-tag {
  background: #e0f2fe; /* Xanh dương nhẹ phù hợp với badge sắp sale */
  color: #0369a1;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

/* Short Specs */
.short-specs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 20px;
}

.spec-item {
  display: flex;
  gap: 4px;
  font-size: 13px;
}

.spec-label {
  color: #666;
  font-weight: 500;
}

.spec-value {
  color: #1e293b;
  font-weight: 600;
}

/* Variants Section */
.variants-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.variants-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.variants-title {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
}

.reset-filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.reset-filter-btn:hover {
  background: #f1f5f9;
  color: #18a058;
  border-color: #cbd5e1;
}

.variants-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.variant-group {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.variant-label {
  min-width: 60px;
  font-size: 13px;
  font-weight: 600;
  color: #666;
  padding-top: 6px;
}

.variant-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.variant-chip {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.variant-chip:hover {
  border-color: #18a058;
}

/* Chuyển cục cấu hình đang chọn từ nền hồng thành nền xanh lá nhạt */
.variant-chip.active {
  border-color: #18a058;
  background: #f0fdf4;
  color: #18a058;
  font-weight: 600;
}

/* Actions Section */
.actions-section {
  border-top: 1px solid #e2e8f0;
  padding-top: 20px;
}

.quantity-box {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.quantity-label {
  font-weight: 600;
  color: #1e293b;
}

.stock-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #16a34a;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

/* Nút Mua ngay: Giữ màu xanh lá chủ đạo */
.btn-buy-now {
  flex: 1;
  background: #18a058;
  border: none;
  color: white;
  font-weight: 700;
  height: 48px;
}
.btn-buy-now:hover {
  background: #148046;
}

/* *** SỬA NÚT THÊM GIỎ HÀNG THÀNH XANH DƯƠNG THEO YÊU CẦU *** */
.btn-add-cart {
  flex: 1;
  background: #2563eb; /* Màu xanh dương (blue) */
  border: none;
  color: white;
  font-weight: 700;
  height: 48px;
}
.btn-add-cart:hover {
  background: #1d4ed8;
}

.btn-disabled {
  flex: 1;
  background: #e2e8f0;
  border: none;
  color: #999;
  font-weight: 700;
  height: 48px;
}

/* Store Sidebar */
.store-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.promise-card,
.voucher-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.promise-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 13px;
  color: #666;
}

.promise-item:not(:last-child) {
  border-bottom: 1px dashed #e2e8f0;
}

.voucher-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.voucher-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.voucher-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
}

.voucher-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.voucher-count {
  font-size: 14px;
  color: #18a058;
  font-weight: 500;
}

/* Product Tabs */
.product-tabs {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.tab-content {
  padding: 20px 0;
}

/* Related Products */
.related-products {
  margin-top: 30px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.related-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e2e8f0;
}

.related-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #18a058;
}

.card-image {
  position: relative;
  padding-top: 100%;
  background: #f8fafc;
}

.card-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 12px;
}

.card-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #18a058;
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.card-info {
  padding: 12px;
}

.card-name {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}

.card-price {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.card-price .current {
  font-size: 14px;
  font-weight: 700;
  color: #18a058;
}

.card-price .old {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}

/* Voucher Modal */
.voucher-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 4px;
}

.voucher-item {
  position: relative;
  display: flex;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.voucher-item:hover {
  border-color: #18a058;
  background: #f0fdf4;
}

/* Chuyển nền voucher được chọn từ hồng thành nền xanh lá nhạt */
.voucher-item.selected {
  border-color: #18a058;
  background: #f0fdf4;
}

.voucher-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #f59e0b; /* Màu vàng phù hợp hơn màu đỏ cũ */
  color: white;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 12px;
  z-index: 2;
}

.voucher-left {
  flex: 1;
}

.voucher-code {
  display: block;
  font-weight: 700;
  color: #18a058;
  margin-bottom: 4px;
}

.voucher-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.voucher-right {
  display: flex;
  align-items: center;
  padding-left: 16px;
}

.voucher-check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voucher-check.checked {
  background: #18a058;
  border-color: #18a058;
}
</style>
