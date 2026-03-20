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
  Heart,
  HeartOutline,
  LocationOutline,
  RefreshOutline,
  ReturnDownBackOutline,
  RocketOutline,
  ShareSocialOutline,
  ShieldCheckmarkOutline,
  Star,
  StorefrontOutline,
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

const finalTotalPrice = computed(() => {
  const total = currentOrderTotal.value
  return total > 0 ? total : 0
})

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
  <div class="pd-page">
    <div class="pd-container">
      <!-- Breadcrumb -->
      <div class="pd-breadcrumb">
        <span @click="router.push('/')">Trang chủ</span>
        <span class="sep">/</span>
        <span @click="router.push('/san-pham')">Sản phẩm</span>
        <span class="sep">/</span>
        <span class="active">{{ product?.name || 'Chi tiết sản phẩm' }}</span>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="pd-loading">
        <NSpin size="large" />
        <p>Đang tải thông tin sản phẩm...</p>
      </div>

      <div v-else-if="product">
        <!-- ══ MAIN LAYOUT ══ -->
        <div class="pd-main">
          <!-- CỘT TRÁI: Ảnh + Thông số kỹ thuật -->
          <div class="pd-left">
            <div class="pd-gallery">
              <div class="pd-image-wrap">
                <img
                  :src="selectedImage"
                  :alt="product.name"
                  @error="selectedImage = 'https://via.placeholder.com/500'"
                >
                <div class="pd-badges">
                  <span v-if="isOngoingSale && rawPercent > 0" class="badge badge--sale">-{{ rawPercent }}%</span>
                  <span v-if="isUpcomingSale && rawPercent > 0" class="badge badge--upcoming">Sắp giảm</span>
                  <span v-if="isOutOfStock" class="badge badge--oos">Hết hàng</span>
                </div>
              </div>
            </div>

            <!-- Thông số kỹ thuật ngay dưới ảnh -->
            <div class="pd-specs-card">
              <div class="pd-specs-card__title">
                Thông số kỹ thuật
              </div>
              <NDescriptions
                bordered :column="1" size="small"
                label-style="width: 120px; font-weight: 600; background: #f9f9f9; font-size: 12.5px;"
                content-style="font-size: 12.5px;"
              >
                <NDescriptionsItem label="CPU">
                  {{ product.cpuName || product.cpu || '—' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="RAM">
                  {{ product.ramName || product.ram || '—' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="Ổ cứng">
                  {{ product.hardDriveName || product.hardDrive || '—' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="GPU">
                  {{ product.gpuName || product.gpu || '—' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="Màn hình">
                  {{ product.screenName || product.screen || '—' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="Màu sắc">
                  {{ product.colorName || product.color || '—' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="Pin">
                  {{ product.batteryName || product.battery || '—' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="HĐH">
                  {{ product.operatingSystem || '—' }}
                </NDescriptionsItem>
              </NDescriptions>
            </div>
          </div>

          <!-- CỘT PHẢI: Thông tin + mua hàng -->
          <div class="pd-info">
            <h1 class="pd-title">
              {{ product.name }}
            </h1>
            <p class="pd-code">
              Mã SP: <strong>{{ product.code }}</strong>
            </p>

            <!-- Flash Sale -->
            <div
              v-if="isOngoingSale || isUpcomingSale" class="pd-timer"
              :class="{ 'pd-timer--ongoing': isOngoingSale, 'pd-timer--upcoming': isUpcomingSale }"
            >
              <div class="pd-timer__left">
                <NIcon size="17">
                  <Flash v-if="isOngoingSale" /><TimeOutline v-else />
                </NIcon>
                <div>
                  <div class="pd-timer__label">
                    {{ isOngoingSale ? 'FLASH SALE' : 'SẮP DIỄN RA' }}
                  </div>
                  <div class="pd-timer__sub">
                    {{ isOngoingSale ? 'Kết thúc trong' : 'Bắt đầu sau' }}
                  </div>
                </div>
              </div>
              <div class="pd-timer__value">
                {{ timeLeft }}
              </div>
            </div>

            <!-- Giá -->
            <div class="pd-price-block" :class="{ 'pd-price-block--sale': isOngoingSale }">
              <div v-if="isOngoingSale && rawPercent > 0" class="pd-price-original">
                {{ formatCurrency(listPrice) }}
              </div>
              <div class="pd-price-row">
                <span class="pd-price-main" :class="{ 'pd-price-main--sale': isOngoingSale }">{{ formatCurrency(sellingPrice) }}</span>
                <span v-if="isOngoingSale && rawPercent > 0" class="pd-price-pct">-{{ rawPercent }}%</span>
                <span v-if="isUpcomingSale && rawPercent > 0" class="pd-upcoming-tag">Dự kiến -{{ rawPercent }}%</span>
              </div>
            </div>

            <!-- Chọn phiên bản -->
            <div v-if="cpuOptions.length > 0 || gpuOptions.length > 0 || ramOptions.length > 0 || hardDriveOptions.length > 0 || colorOptions.length > 0" class="pd-variants">
              <div class="pd-variants__head">
                <span class="pd-variants__title">Tuỳ chọn phiên bản</span>
                <button class="pd-reset-btn" @click="handleResetFilter">
                  <NIcon size="12">
                    <RefreshOutline />
                  </NIcon> Làm mới
                </button>
              </div>
              <div class="pd-variants__body">
                <div v-if="cpuOptions.length > 0" class="pd-vgroup">
                  <span class="pd-vgroup__label">CPU:</span>
                  <div class="pd-vgroup__chips">
                    <button v-for="opt in cpuOptions" :key="opt.value" class="pd-chip" :class="{ 'pd-chip--active': selectedCpu === opt.value }" @click="selectVariantOption('CPU', opt.value)">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
                <div v-if="ramOptions.length > 0" class="pd-vgroup">
                  <span class="pd-vgroup__label">RAM:</span>
                  <div class="pd-vgroup__chips">
                    <button v-for="opt in ramOptions" :key="opt.value" class="pd-chip" :class="{ 'pd-chip--active': selectedRam === opt.value }" @click="selectVariantOption('RAM', opt.value)">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
                <div v-if="hardDriveOptions.length > 0" class="pd-vgroup">
                  <span class="pd-vgroup__label">Ổ cứng:</span>
                  <div class="pd-vgroup__chips">
                    <button v-for="opt in hardDriveOptions" :key="opt.value" class="pd-chip" :class="{ 'pd-chip--active': selectedHardDrive === opt.value }" @click="selectVariantOption('HDD', opt.value)">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
                <div v-if="gpuOptions.length > 0" class="pd-vgroup">
                  <span class="pd-vgroup__label">VGA:</span>
                  <div class="pd-vgroup__chips">
                    <button v-for="opt in gpuOptions" :key="opt.value" class="pd-chip" :class="{ 'pd-chip--active': selectedGpu === opt.value }" @click="selectVariantOption('GPU', opt.value)">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
                <div v-if="colorOptions.length > 0" class="pd-vgroup">
                  <span class="pd-vgroup__label">Màu:</span>
                  <div class="pd-vgroup__chips">
                    <button v-for="opt in colorOptions" :key="opt.value" class="pd-chip" :class="{ 'pd-chip--active': selectedColor === opt.value }" @click="selectVariantOption('COLOR', opt.value)">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Số lượng + tồn kho -->
            <div class="pd-qty-row">
              <span class="pd-qty-label">Số lượng:</span>
              <NInputNumber
                v-model:value="quantity"
                :min="1" :max="Math.min(5, stockQuantity)"
                :disabled="isOutOfStock"
                button-placement="both" size="medium"
                style="width: 110px"
              />
              <span v-if="stockQuantity > 0" class="pd-stock">
                <NIcon size="14" color="#16a34a"><CheckmarkCircleOutline /></NIcon>
                Còn <strong>{{ stockQuantity }}</strong> sp
              </span>
            </div>

            <!-- Nút mua -->
            <div class="pd-btn-row">
              <NButton v-if="!isOutOfStock" size="large" class="pd-btn-buy" :loading="loadingCart" @click="handleBuyNow">
                <template #icon>
                  <NIcon><RocketOutline /></NIcon>
                </template>
                MUA NGAY
              </NButton>
              <NButton v-else size="large" class="pd-btn-oos" disabled>
                <template #icon>
                  <NIcon><AlertCircleOutline /></NIcon>
                </template>
                HẾT HÀNG
              </NButton>
              <NButton size="large" class="pd-btn-cart" :disabled="isOutOfStock" :loading="loadingCart" @click="handleAddToCart">
                <template #icon>
                  <NIcon><CartOutline /></NIcon>
                </template>
                Thêm giỏ hàng
              </NButton>
            </div>

            <!-- Cam kết + Voucher -->
            <div class="pd-promises">
              <div class="pd-promise">
                <NIcon size="14" color="#16a34a">
                  <ShieldCheckmarkOutline />
                </NIcon><span>Giao hàng toàn quốc</span>
              </div>
              <div class="pd-promise">
                <NIcon size="14" color="#16a34a">
                  <ReturnDownBackOutline />
                </NIcon><span>Đổi trả 30 ngày</span>
              </div>
              <div class="pd-promise">
                <NIcon size="14" color="#16a34a">
                  <HeadsetOutline />
                </NIcon><span>Hỗ trợ 24/7</span>
              </div>
              <div class="pd-promise">
                <NIcon size="14" color="#16a34a">
                  <RocketOutline />
                </NIcon><span>Giao nhanh 2H</span>
              </div>
            </div>

            <div class="pd-voucher" @click="showVoucherModal = true">
              <NIcon size="15" color="#16a34a">
                <GiftOutline />
              </NIcon>
              <span>Mã giảm giá:</span>
              <strong>{{ validVouchers.length > 0 ? `${validVouchers.length} mã khả dụng` : 'Chưa có mã' }}</strong>
              <NIcon size="13" style="margin-left: auto">
                <ArrowBack style="transform: rotate(180deg)" />
              </NIcon>
            </div>
          </div>
        </div>

        <!-- Sản phẩm liên quan -->
        <div v-if="relatedProducts.length > 0" class="pd-related">
          <h2 class="pd-related__title">
            Sản phẩm liên quan
          </h2>
          <div class="pd-related__grid">
            <div v-for="item in relatedProducts" :key="item.id" class="pd-rcard" @click="router.push(`/product-detail/${item.id}`)">
              <div class="pd-rcard__img">
                <img :src="item.urlImage" :alt="item.name">
                <div v-if="item.percentage" class="pd-rcard__badge">
                  -{{ item.percentage }}%
                </div>
              </div>
              <div class="pd-rcard__body">
                <p class="pd-rcard__name">
                  {{ item.name }}
                </p>
                <span class="pd-rcard__cur">{{ formatCurrency(item.price) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Voucher Modal -->
    <NModal v-model:show="showVoucherModal" preset="card" title="Mã giảm giá khả dụng" style="width: 480px">
      <div v-if="validVouchers.length === 0" style="padding: 32px; text-align: center">
        <NEmpty description="Chưa có mã giảm giá phù hợp" />
      </div>
      <div v-else class="pd-vmodal-list">
        <div
          v-for="(v, index) in validVouchers" :key="v.id"
          class="pd-vmodal-item" :class="{ 'pd-vmodal-item--selected': selectedVoucher?.id === v.id }"
          @click="handleSelectVoucher(v)"
        >
          <div v-if="index === 0" class="pd-vmodal-best">
            TỐT NHẤT
          </div>
          <div class="pd-vmodal-left">
            <span class="pd-vmodal-code">{{ v.code }}</span>
            <span class="pd-vmodal-name">{{ v.name }}</span>
            <span class="pd-vmodal-desc">Giảm: {{ v.typeVoucher === 'PERCENTAGE' ? `${v.discountValue}%` : formatCurrency(v.discountValue || 0) }}<template v-if="v.maxValue"> · Tối đa: {{ formatCurrency(v.maxValue) }}</template></span>
            <span class="pd-vmodal-cond">Đơn tối thiểu: {{ formatCurrency(v.conditions || 0) }}</span>
          </div>
          <div class="pd-vmodal-check" :class="{ 'pd-vmodal-check--on': selectedVoucher?.id === v.id }">
            <NIcon v-if="selectedVoucher?.id === v.id" color="white" size="13">
              <CheckmarkCircle />
            </NIcon>
          </div>
        </div>
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.pd-page { background: #f5f5f5; min-height: 100vh; padding: 20px 0 48px; }
.pd-container { max-width: 1100px; margin: 0 auto; padding: 0 16px; }

/* Breadcrumb */
.pd-breadcrumb { font-size: 13px; color: #888; margin-bottom: 14px; display: flex; align-items: center; gap: 6px; }
.pd-breadcrumb span { cursor: pointer; }
.pd-breadcrumb span:hover { color: #22c55e; }
.pd-breadcrumb .active { color: #22c55e; font-weight: 500; cursor: default; }
.pd-breadcrumb .sep { color: #ccc; cursor: default; }

/* Loading */
.pd-loading { display: flex; flex-direction: column; align-items: center; padding: 64px; background: #fff; border-radius: 8px; border: 1px solid #e5e5e5; }
.pd-loading p { margin-top: 14px; color: #888; font-size: 14px; }

/* ── MAIN GRID: 420px trái | 1fr phải ── */
.pd-main {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 16px;
  margin-bottom: 16px;
  align-items: start;
}

/* Cột trái */
.pd-left { display: flex; flex-direction: column; gap: 12px; }

/* Ảnh */
.pd-gallery { background: #fff; border-radius: 8px; border: 1px solid #e5e5e5; padding: 12px; }
.pd-image-wrap { position: relative; padding-top: 100%; background: #fafafa; border-radius: 6px; border: 1px solid #f0f0f0; overflow: hidden; }
.pd-image-wrap img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: contain; }
.pd-badges { position: absolute; top: 8px; left: 8px; display: flex; flex-direction: column; gap: 4px; z-index: 2; }
.badge { display: inline-block; padding: 2px 7px; border-radius: 4px; font-size: 11px; font-weight: 700; color: #fff; }
.badge--sale { background: #22c55e; }
.badge--upcoming { background: #3b82f6; }
.badge--oos { background: #9ca3af; }

/* Thông số kỹ thuật */
.pd-specs-card { background: #fff; border-radius: 8px; border: 1px solid #e5e5e5; overflow: hidden; }
.pd-specs-card__title { font-size: 13px; font-weight: 700; color: #333; padding: 10px 14px; background: #fafafa; border-bottom: 1px solid #e5e5e5; }

/* Cột phải */
.pd-info { background: #fff; border-radius: 8px; border: 1px solid #e5e5e5; padding: 20px; display: flex; flex-direction: column; gap: 14px; }

.pd-title { font-size: 18px; font-weight: 700; color: #111; line-height: 1.35; margin: 0; }
.pd-code { font-size: 12px; color: #999; margin: 0; }
.pd-code strong { color: #666; }

/* Timer */
.pd-timer { display: flex; align-items: center; justify-content: space-between; padding: 9px 12px; border-radius: 6px; color: #fff; }
.pd-timer--ongoing { background: #16a34a; }
.pd-timer--upcoming { background: #2563eb; }
.pd-timer__left { display: flex; align-items: center; gap: 8px; }
.pd-timer__label { font-weight: 700; font-size: 12px; }
.pd-timer__sub { font-size: 11px; opacity: 0.85; }
.pd-timer__value { font-family: monospace; font-size: 16px; font-weight: 700; background: rgba(0,0,0,0.15); padding: 4px 10px; border-radius: 4px; }

/* Giá */
.pd-price-block { padding: 12px 14px; background: #f9f9f9; border-radius: 6px; border: 1px solid #f0f0f0; }
.pd-price-block--sale { background: #f0fdf4; border-color: #bbf7d0; }
.pd-price-original { font-size: 12px; color: #aaa; text-decoration: line-through; margin-bottom: 2px; }
.pd-price-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.pd-price-main { font-size: 26px; font-weight: 700; color: #111; }
.pd-price-main--sale { color: #16a34a; }
.pd-price-pct { background: #16a34a; color: #fff; font-size: 11px; font-weight: 700; padding: 2px 7px; border-radius: 4px; }
.pd-upcoming-tag { background: #3b82f6; color: #fff; font-size: 11px; font-weight: 600; padding: 2px 7px; border-radius: 4px; }

/* Variants */
.pd-variants { border: 1px solid #f0f0f0; border-radius: 6px; overflow: hidden; }
.pd-variants__head { display: flex; align-items: center; justify-content: space-between; padding: 9px 12px; background: #f9f9f9; border-bottom: 1px solid #f0f0f0; }
.pd-variants__title { font-size: 12.5px; font-weight: 700; color: #333; }
.pd-reset-btn { display: flex; align-items: center; gap: 4px; padding: 3px 8px; border: 1px solid #e5e5e5; border-radius: 4px; background: #fff; font-size: 11.5px; color: #666; cursor: pointer; }
.pd-reset-btn:hover { background: #f5f5f5; }
.pd-variants__body { padding: 10px 12px; display: flex; flex-direction: column; gap: 8px; }
.pd-vgroup { display: flex; align-items: flex-start; gap: 8px; }
.pd-vgroup__label { min-width: 56px; font-size: 12px; font-weight: 600; color: #666; padding-top: 5px; flex-shrink: 0; }
.pd-vgroup__chips { display: flex; flex-wrap: wrap; gap: 5px; }
.pd-chip { padding: 4px 10px; border: 1px solid #e5e5e5; border-radius: 4px; background: #fff; font-size: 12px; cursor: pointer; }
.pd-chip:hover { border-color: #22c55e; }
.pd-chip--active { border-color: #22c55e; background: #f0fdf4; color: #16a34a; font-weight: 600; }

/* Số lượng */
.pd-qty-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pd-qty-label { font-size: 13px; font-weight: 600; color: #333; }
.pd-stock { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #16a34a; }

/* Nút mua */
.pd-btn-row { display: flex; gap: 8px; }
.pd-btn-buy  { flex: 1; background: #22c55e !important; border: none !important; color: #fff !important; font-weight: 700 !important; height: 44px !important; }
.pd-btn-cart { flex: 1; background: #fff !important; border: 1.5px solid #22c55e !important; color: #16a34a !important; font-weight: 700 !important; height: 44px !important; }
.pd-btn-cart:hover { background: #f0fdf4 !important; }
.pd-btn-oos  { flex: 1; background: #f0f0f0 !important; border: none !important; color: #aaa !important; font-weight: 700 !important; height: 44px !important; }

/* Cam kết */
.pd-promises { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; padding: 10px 12px; background: #f9f9f9; border-radius: 6px; border: 1px solid #f0f0f0; }
.pd-promise { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #555; }

/* Voucher */
.pd-voucher { display: flex; align-items: center; gap: 7px; padding: 9px 12px; border: 1px solid #e5e5e5; border-radius: 6px; cursor: pointer; font-size: 12.5px; color: #333; background: #fff; }
.pd-voucher:hover { background: #f9f9f9; }
.pd-voucher strong { color: #16a34a; }

/* Related */
.pd-related { margin-bottom: 16px; }
.pd-related__title { font-size: 15px; font-weight: 700; color: #111; margin-bottom: 10px; }
.pd-related__grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; }
.pd-rcard { background: #fff; border: 1px solid #e5e5e5; border-radius: 6px; overflow: hidden; cursor: pointer; }
.pd-rcard:hover { border-color: #22c55e; }
.pd-rcard__img { position: relative; padding-top: 100%; background: #f5f5f5; }
.pd-rcard__img img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: contain; padding: 8px; }
.pd-rcard__badge { position: absolute; top: 5px; left: 5px; background: #22c55e; color: #fff; font-size: 10px; font-weight: 700; padding: 2px 5px; border-radius: 3px; }
.pd-rcard__body { padding: 8px 10px; }
.pd-rcard__name { font-size: 12px; font-weight: 500; color: #222; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 32px; margin-bottom: 4px; }
.pd-rcard__cur { font-size: 12.5px; font-weight: 700; color: #16a34a; }

/* Voucher Modal */
.pd-vmodal-list { max-height: 420px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.pd-vmodal-item { position: relative; display: flex; align-items: center; gap: 12px; padding: 12px; border: 1px solid #e5e5e5; border-radius: 6px; cursor: pointer; }
.pd-vmodal-item:hover, .pd-vmodal-item--selected { border-color: #22c55e; background: #f0fdf4; }
.pd-vmodal-best { position: absolute; top: -7px; right: -7px; background: #f59e0b; color: #fff; font-size: 10px; font-weight: 700; padding: 2px 7px; border-radius: 10px; }
.pd-vmodal-left { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.pd-vmodal-code { font-weight: 700; color: #16a34a; font-size: 12.5px; }
.pd-vmodal-name { font-size: 13.5px; font-weight: 600; color: #111; }
.pd-vmodal-desc { font-size: 12.5px; color: #666; }
.pd-vmodal-cond { font-size: 11.5px; color: #aaa; }
.pd-vmodal-check { width: 20px; height: 20px; border-radius: 50%; border: 2px solid #e5e5e5; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.pd-vmodal-check--on { background: #22c55e; border-color: #22c55e; }

/* Responsive */
@media (max-width: 860px) {
  .pd-main { grid-template-columns: 1fr; }
  .pd-related__grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 540px) {
  .pd-related__grid { grid-template-columns: repeat(2, 1fr); }
  .pd-btn-row { flex-direction: column; }
  .pd-promises { grid-template-columns: 1fr; }
}
</style>
