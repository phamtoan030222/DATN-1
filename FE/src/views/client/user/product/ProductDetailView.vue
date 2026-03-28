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
  RefreshOutline,
  ReturnDownBackOutline,
  RocketOutline,
  ShieldCheckmarkOutline,
  TimeOutline,
} from '@vicons/ionicons5'
import {
  NButton,
  NDescriptions,
  NDescriptionsItem,
  NEmpty,
  NIcon,
  NInputNumber,
  NModal,
  NSpin,
  useMessage,
} from 'naive-ui'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'

import CompareModal from './CompareModal.vue'

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

// ─── Router & Utilities ─────────────────────────────────
const route = useRoute()
const router = useRouter()
const message = useMessage()

// ─── UI State ───────────────────────────────────────────
const loading = ref(false)
const loadingCart = ref(false)
const activeTab = ref('description')
const showVoucherModal = ref(false)
const showCompareModal = ref(false)

// ─── Cart Store ─────────────────────────────────────────
const cartStore = useCartStore()
const { cartItems } = storeToRefs(cartStore)
const { addToCart } = cartStore

// ─── Product Data ────────────────────────────────────────
const product = ref<any>(null)
const allVariants = ref<any[]>([])
const relatedProducts = ref<any[]>([])
const selectedImage = ref('')
const quantity = ref(1)

// ─── Variant Options ─────────────────────────────────────
const gpuOptions = ref<any[]>([])
const cpuOptions = ref<any[]>([])
const ramOptions = ref<any[]>([])
const hardDriveOptions = ref<any[]>([])
const colorOptions = ref<any[]>([])

// ─── Selected Variants ────────────────────────────────────
const selectedGpu = ref<string | null>(null)
const selectedCpu = ref<string | null>(null)
const selectedRam = ref<string | null>(null)
const selectedHardDrive = ref<string | null>(null)
const selectedColor = ref<string | null>(null)

// ─── Stock ───────────────────────────────────────────────
const isOutOfStock = ref(false)
const stockQuantity = ref(0)

// ─── Voucher ─────────────────────────────────────────────
const rawVoucherList = ref<ADVoucherResponse[]>([])
const selectedVoucher = ref<ADVoucherResponse | null>(null)

// ─── Sale / Timer ────────────────────────────────────────
const timeLeft = ref('')
const isFlashSaleEnded = ref(false)
const isUpcomingSale = ref(false)
const isOngoingSale = ref(false)
let timerInterval: any = null

// ─── Helpers ─────────────────────────────────────────────
function formatCurrency(val: number | undefined) {
  return val === undefined
    ? '0₫'
    : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

function calcDiscount(v: ADVoucherResponse, total: number) {
  if (v.typeVoucher === 'FIXED_AMOUNT') return v.discountValue || 0
  let disc = (total * (v.discountValue || 0)) / 100
  if (v.maxValue && disc > v.maxValue) disc = v.maxValue
  return disc
}

// ─── Computed ────────────────────────────────────────────
const rawPercent = computed(() => Number(product.value?.percentage) || 0)
const currentPercent = computed(() => (isOngoingSale.value ? rawPercent.value : 0))
const listPrice = computed(() => Number(product.value?.price) || 0)
const sellingPrice = computed(() => {
  const p = currentPercent.value
  return p <= 0 ? listPrice.value : (listPrice.value * (100 - p)) / 100
})
const currentOrderTotal = computed(() => sellingPrice.value * quantity.value)

const validVouchers = computed(() => {
  const total = currentOrderTotal.value
  return rawVoucherList.value
    .filter(v => !v.conditions || total >= v.conditions)
    .sort((a, b) => calcDiscount(b, total) - calcDiscount(a, total))
})

const hasVariants = computed(() =>
  cpuOptions.value.length > 0
  || gpuOptions.value.length > 0
  || ramOptions.value.length > 0
  || hardDriveOptions.value.length > 0
  || colorOptions.value.length > 0,
)

const compareOptions = computed(() => {
  const opts: any[] = []
  allVariants.value.forEach((v) => {
    if (v.id !== product.value?.id)
      opts.push({ label: `[Cùng loại] ${v.name}`, value: v.id, data: v })
  })
  relatedProducts.value.forEach((p) => {
    if (p.id !== product.value?.id && !opts.find(o => o.value === p.id))
      opts.push({ label: `[Khác] ${p.name}`, value: p.id, data: p })
  })
  return opts
})

// ─── Cart Logic ──────────────────────────────────────────
function validateCartAddition(): boolean {
  const existing = cartItems.value.find((i: any) => i.productDetailId === product.value.id)
  const inCart = existing ? existing.quantity : 0
  const total = quantity.value + inCart

  if (total > stockQuantity.value) {
    message.error(
      inCart > 0
        ? `Kho chỉ còn ${stockQuantity.value} sản phẩm. Bạn đã có ${inCart} sản phẩm trong giỏ!`
        : `Rất tiếc, kho chỉ còn ${stockQuantity.value} sản phẩm!`,
    )
    return false
  }
  if (total > 5) {
    message.error(
      inCart > 0
        ? `Tối đa 5 sản phẩm/đơn. Bạn đã có ${inCart} máy cấu hình này trong giỏ!`
        : `Chỉ được mua tối đa 5 sản phẩm cho mỗi cấu hình!`,
    )
    return false
  }
  return true
}

async function handleAddToCart() {
  if (!validateCartAddition()) return
  loadingCart.value = true
  try {
    await addToCart(product.value.id, quantity.value)
    message.success('Đã thêm sản phẩm vào giỏ hàng')
  }
  catch (e: any) {
    message.error(e.message || 'Không thể thêm vào giỏ hàng')
  }
  finally {
    loadingCart.value = false
  }
}

async function handleBuyNow() {
  if (!validateCartAddition()) return
  loadingCart.value = true
  try {
    await addToCart(product.value.id, quantity.value)
    router.push({ name: 'Cart' })
  }
  catch (e: any) {
    message.error(e.message || 'Không thể thực hiện mua ngay')
  }
  finally {
    loadingCart.value = false
  }
}

// ─── Voucher Logic ───────────────────────────────────────
async function fetchAvailableVouchers() {
  try {
    const res = await getVouchers({ page: 1, size: 50, status: 'ACTIVE' })
    const now = Date.now()
    rawVoucherList.value = res.content.filter((v: any) => {
      const inTime = (!v.startDate || v.startDate <= now) && (!v.endDate || v.endDate >= now)
      return inTime && (v.remainingQuantity === null || v.remainingQuantity > 0)
    })
  }
  catch (e) { console.error(e) }
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

// ─── Sale Timer ──────────────────────────────────────────
function startCountdown() {
  if (timerInterval) clearInterval(timerInterval)

  const parseSafe = (d: any): number => {
    if (!d) return 0
    const t = new Date(typeof d === 'string' ? d.replace(/-/g, '/') : d).getTime()
    return Number.isNaN(t) ? 0 : t
  }

  const startTs = parseSafe(product.value?.startDate)
  const endTs = parseSafe(product.value?.endDate)

  if (!endTs) {
    isFlashSaleEnded.value = true
    isUpcomingSale.value = false
    isOngoingSale.value = false
    timeLeft.value = ''
    return
  }

  const tick = () => {
    const now = Date.now()
    let dist = 0

    if (now < startTs) {
      isUpcomingSale.value = true; isOngoingSale.value = false; isFlashSaleEnded.value = false
      dist = startTs - now
    }
    else if (now <= endTs) {
      isUpcomingSale.value = false; isOngoingSale.value = true; isFlashSaleEnded.value = false
      dist = endTs - now
    }
    else {
      isUpcomingSale.value = false; isOngoingSale.value = false; isFlashSaleEnded.value = true
      clearInterval(timerInterval); timeLeft.value = ''; return
    }

    const pad = (n: number) => n.toString().padStart(2, '0')
    const d = Math.floor(dist / 86400000)
    const h = Math.floor((dist % 86400000) / 3600000)
    const m = Math.floor((dist % 3600000) / 60000)
    const s = Math.floor((dist % 60000) / 1000)
    timeLeft.value = d > 0 ? `${d} Ngày ${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(h)}:${pad(m)}:${pad(s)}`
  }

  tick()
  if (!isFlashSaleEnded.value) timerInterval = setInterval(tick, 1000)
}

// ─── Data Fetching ───────────────────────────────────────
function mapOptions(res: any) {
  let arr: any[] = []
  if (res?.data) {
    if (Array.isArray(res.data)) arr = res.data
    else if (res.data.data && Array.isArray(res.data.data)) arr = res.data.data
    else if (res.data.content && Array.isArray(res.data.content)) arr = res.data.content
  }
  else if (Array.isArray(res)) { arr = res }
  return arr.map((x: any) => ({ label: x.label || x.name || x.code, value: x.value || x.id }))
}

function extractList(res: any): any[] {
  if (!res?.data) return Array.isArray(res) ? res : []
  const d = res.data
  if (Array.isArray(d)) return d
  if (d.data && Array.isArray(d.data)) return d.data
  if (Array.isArray(res)) return res
  return []
}

async function loadGlobalOptions() {
  try {
    const [gpus, cpus, rams, hds, colors] = await Promise.all([
      getGpuByPD(product.value.id),
      getCpuByPD(product.value.id),
      getRamByPD(product.value.id),
      getHardDriveByPD(product.value.id),
      getColorsByPD(product.value.id),
    ])
    gpuOptions.value = mapOptions(gpus)
    cpuOptions.value = mapOptions(cpus)
    ramOptions.value = mapOptions(rams)
    hardDriveOptions.value = mapOptions(hds)
    colorOptions.value = mapOptions(colors)
  }
  catch (e) { console.warn('Lỗi tải thuộc tính', e) }
}

async function loadAllVariants(idProduct: string) {
  if (!idProduct) return
  try {
    const res = await getProductDetails({ page: 1, size: 100, idProduct, minPrice: 0, maxPrice: 1000000000 })
    allVariants.value = extractList(res)
  }
  catch (e) { console.error('Lỗi tải variants', e) }
}

async function fetchRelatedProducts(idProduct: string) {
  try {
    const res = await getProductDetails({ page: 1, size: 6, idProduct, minPrice: 0, maxPrice: 1000000000 })
    relatedProducts.value = extractList(res).filter((p: any) => p.id !== product.value.id).slice(0, 6)
  }
  catch (e) { console.error('Lỗi sản phẩm liên quan', e) }
}

async function checkStock(productDetailId: string) {
  isOutOfStock.value = false
  stockQuantity.value = 0
  try {
    const res = await getImeiProductDetail(productDetailId)
    const arr = Array.isArray(res?.data) ? res.data : (res?.data as any)?.data || []
    const count = arr.filter((i: any) => {
      const s = i.status ?? i.imeiStatus
      return s === undefined || s === null || String(s) === '0' || String(s).toUpperCase() === 'ACTIVE'
    }).length
    stockQuantity.value = count
    if (count === 0) isOutOfStock.value = true
  }
  catch (e) { isOutOfStock.value = true }
}

async function selectVariantOption(type: string, val: string) {
  const map: Record<string, any> = {
    CPU: selectedCpu, GPU: selectedGpu, RAM: selectedRam, HDD: selectedHardDrive, COLOR: selectedColor,
  }
  if (map[type]) map[type].value = val
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
    if (res.data?.id && res.data.id !== product.value.id) {
      await router.replace({ params: { id: res.data.id }, query: route.query })
    }
    else {
      loading.value = false
      if (!res.data?.id) message.warning('Phiên bản cấu hình này hiện không khả dụng.')
    }
  }
  catch {
    message.error('Không tìm thấy cấu hình phù hợp.')
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

async function fetchData(id: string) {
  loading.value = true
  try {
    const res = await getProductDetailById(id)
    if (!res.data) return
    product.value = res.data

    // Apply sale params from query
    if (route.query.pct) product.value.percentage = Number(route.query.pct)
    if (route.query.sd) product.value.startDate = Number(route.query.sd)
    if (route.query.ed) product.value.endDate = Number(route.query.ed)

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

    // Fallback: get sale info from variants
    if (!product.value.endDate && !route.query.ed) {
      const cur = allVariants.value.find((v: any) => v.id === product.value.id)
      if (cur?.endDate) {
        product.value.endDate = cur.endDate
        product.value.startDate = cur.startDate
        product.value.percentage = cur.percentage
      }
    }

    startCountdown()
    await checkStock(product.value.id)
    fetchAvailableVouchers()
  }
  catch {
    message.error('Không thể tải thông tin sản phẩm')
  }
  finally {
    loading.value = false
  }
}

// ─── Lifecycle ───────────────────────────────────────────
watch(() => route.params.id, (id) => { if (id) fetchData(id as string) })
onMounted(() => fetchData(route.params.id as string))
onUnmounted(() => { if (timerInterval) clearInterval(timerInterval) })
</script>

<template>
  <div class="pdp">
    <div class="pdp__container">

      <!-- ── Breadcrumb ── -->
      <nav class="pdp__breadcrumb">
        <span @click="router.push('/')">Trang chủ</span>
        <span class="pdp__breadcrumb-sep">/</span>
        <span @click="router.push('/san-pham')">Sản phẩm</span>
        <span class="pdp__breadcrumb-sep">/</span>
        <span class="pdp__breadcrumb-active">{{ product?.name || 'Chi tiết sản phẩm' }}</span>
      </nav>

      <!-- ── Loading ── -->
      <div v-if="loading" class="pdp__loading">
        <NSpin size="large" />
        <p>Đang tải thông tin sản phẩm...</p>
      </div>

      <!-- ── Main Content ── -->
      <template v-else-if="product">

        <!-- ═══ PRODUCT MAIN GRID ═══ -->
        <div class="pdp__main">

          <!-- LEFT: Image + Specs -->
          <div class="pdp__left">

            <!-- Image Gallery -->
            <div class="pdp__gallery">
              <div class="pdp__img-wrap">
                <img
                  :src="selectedImage"
                  :alt="product.name"
                  @error="selectedImage = 'https://via.placeholder.com/500'"
                >
                <!-- Badges -->
                <div class="pdp__badges">
                  <span v-if="isOngoingSale && rawPercent > 0" class="pdp__badge pdp__badge--sale">-{{ rawPercent }}%</span>
                  <span v-if="isUpcomingSale && rawPercent > 0" class="pdp__badge pdp__badge--upcoming">Sắp giảm</span>
                  <span v-if="isOutOfStock" class="pdp__badge pdp__badge--oos">Hết hàng</span>
                </div>
              </div>
            </div>

            <!-- Specifications Table -->
            <div class="pdp__specs">
              <div class="pdp__specs-header">
                <span>Thông số kỹ thuật</span>
              </div>
              <NDescriptions
                bordered :column="1" size="small"
                label-style="width:110px; font-weight:600; background:#f8f8f8; font-size:12.5px; color:#555;"
                content-style="font-size:12.5px; color:#222;"
              >
                <NDescriptionsItem label="CPU">{{ product.cpuName || product.cpu || '—' }}</NDescriptionsItem>
                <NDescriptionsItem label="RAM">{{ product.ramName || product.ram || '—' }}</NDescriptionsItem>
                <NDescriptionsItem label="Ổ cứng">{{ product.hardDriveName || product.hardDrive || '—' }}</NDescriptionsItem>
                <NDescriptionsItem label="GPU">{{ product.gpuName || product.gpu || '—' }}</NDescriptionsItem>
                <NDescriptionsItem label="Màn hình">{{ product.screenName || product.screen || '—' }}</NDescriptionsItem>
                <NDescriptionsItem label="Màu sắc">{{ product.colorName || product.color || '—' }}</NDescriptionsItem>
                <NDescriptionsItem label="Pin">{{ product.batteryName || product.battery || '—' }}</NDescriptionsItem>
                <NDescriptionsItem label="Hệ điều hành">{{ product.operatingSystem || '—' }}</NDescriptionsItem>
              </NDescriptions>
            </div>
          </div>

          <!-- RIGHT: Info + Purchase -->
          <div class="pdp__right">

            <!-- Product Name & Code -->
            <div class="pdp__header">
              <h1 class="pdp__title">{{ product.name }}</h1>
              <span class="pdp__code">Mã SP: <strong>{{ product.code }}</strong></span>
            </div>

            <!-- Flash Sale Timer -->
            <div
              v-if="isOngoingSale || isUpcomingSale"
              class="pdp__timer"
              :class="isOngoingSale ? 'pdp__timer--sale' : 'pdp__timer--upcoming'"
            >
              <div class="pdp__timer-left">
                <NIcon size="16"><Flash v-if="isOngoingSale" /><TimeOutline v-else /></NIcon>
                <div>
                  <div class="pdp__timer-label">{{ isOngoingSale ? 'FLASH SALE' : 'SẮP DIỄN RA' }}</div>
                  <div class="pdp__timer-sub">{{ isOngoingSale ? 'Kết thúc trong' : 'Bắt đầu sau' }}</div>
                </div>
              </div>
              <div class="pdp__timer-value">{{ timeLeft }}</div>
            </div>

            <!-- Price Block -->
            <div class="pdp__price" :class="{ 'pdp__price--sale': isOngoingSale }">
              <span v-if="isOngoingSale && rawPercent > 0" class="pdp__price-original">
                {{ formatCurrency(listPrice) }}
              </span>
              <div class="pdp__price-row">
                <span class="pdp__price-main" :class="{ 'pdp__price-main--sale': isOngoingSale }">
                  {{ formatCurrency(sellingPrice) }}
                </span>
                <span v-if="isOngoingSale && rawPercent > 0" class="pdp__price-badge">-{{ rawPercent }}%</span>
                <span v-if="isUpcomingSale && rawPercent > 0" class="pdp__price-badge pdp__price-badge--blue">
                  Dự kiến -{{ rawPercent }}%
                </span>
              </div>
            </div>

            <!-- Variant Selector -->
            <div v-if="hasVariants" class="pdp__variants">
              <div class="pdp__variants-header">
                <span class="pdp__variants-title">Tuỳ chọn phiên bản</span>
                <button class="pdp__reset-btn" @click="handleResetFilter">
                  <NIcon size="12"><RefreshOutline /></NIcon> Làm mới
                </button>
              </div>
              <div class="pdp__variants-body">
                <div v-if="cpuOptions.length" class="pdp__vgroup">
                  <span class="pdp__vgroup-label">CPU</span>
                  <div class="pdp__chips">
                    <button
                      v-for="opt in cpuOptions" :key="opt.value"
                      class="pdp__chip" :class="{ 'pdp__chip--active': selectedCpu === opt.value }"
                      @click="selectVariantOption('CPU', opt.value)"
                    >{{ opt.label }}</button>
                  </div>
                </div>
                <div v-if="ramOptions.length" class="pdp__vgroup">
                  <span class="pdp__vgroup-label">RAM</span>
                  <div class="pdp__chips">
                    <button
                      v-for="opt in ramOptions" :key="opt.value"
                      class="pdp__chip" :class="{ 'pdp__chip--active': selectedRam === opt.value }"
                      @click="selectVariantOption('RAM', opt.value)"
                    >{{ opt.label }}</button>
                  </div>
                </div>
                <div v-if="hardDriveOptions.length" class="pdp__vgroup">
                  <span class="pdp__vgroup-label">Ổ cứng</span>
                  <div class="pdp__chips">
                    <button
                      v-for="opt in hardDriveOptions" :key="opt.value"
                      class="pdp__chip" :class="{ 'pdp__chip--active': selectedHardDrive === opt.value }"
                      @click="selectVariantOption('HDD', opt.value)"
                    >{{ opt.label }}</button>
                  </div>
                </div>
                <div v-if="gpuOptions.length" class="pdp__vgroup">
                  <span class="pdp__vgroup-label">GPU</span>
                  <div class="pdp__chips">
                    <button
                      v-for="opt in gpuOptions" :key="opt.value"
                      class="pdp__chip" :class="{ 'pdp__chip--active': selectedGpu === opt.value }"
                      @click="selectVariantOption('GPU', opt.value)"
                    >{{ opt.label }}</button>
                  </div>
                </div>
                <div v-if="colorOptions.length" class="pdp__vgroup">
                  <span class="pdp__vgroup-label">Màu sắc</span>
                  <div class="pdp__chips">
                    <button
                      v-for="opt in colorOptions" :key="opt.value"
                      class="pdp__chip" :class="{ 'pdp__chip--active': selectedColor === opt.value }"
                      @click="selectVariantOption('COLOR', opt.value)"
                    >{{ opt.label }}</button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Quantity + Stock -->
            <div class="pdp__qty">
              <span class="pdp__qty-label">Số lượng</span>
              <NInputNumber
                v-model:value="quantity"
                :min="1" :max="Math.min(5, stockQuantity)"
                :disabled="isOutOfStock"
                button-placement="both" size="medium"
                style="width: 110px"
              />
              <span v-if="stockQuantity > 0" class="pdp__stock">
                <NIcon size="13" color="#16a34a"><CheckmarkCircleOutline /></NIcon>
                Còn <strong>{{ stockQuantity }}</strong> sản phẩm
              </span>
            </div>

            <!-- Purchase Buttons -->
            <div class="pdp__actions">
              <NButton
                v-if="!isOutOfStock" size="large" class="pdp__btn pdp__btn--buy"
                :loading="loadingCart" @click="handleBuyNow"
              >
                <template #icon><NIcon><RocketOutline /></NIcon></template>
                Mua ngay
              </NButton>
              <NButton v-else size="large" class="pdp__btn pdp__btn--oos" disabled>
                <template #icon><NIcon><AlertCircleOutline /></NIcon></template>
                Hết hàng
              </NButton>
              <NButton
                size="large" class="pdp__btn pdp__btn--cart"
                :disabled="isOutOfStock" :loading="loadingCart" @click="handleAddToCart"
              >
                <template #icon><NIcon><CartOutline /></NIcon></template>
                Thêm vào giỏ
              </NButton>
            </div>

            <!-- Voucher Banner -->
            <div class="pdp__voucher" @click="showVoucherModal = true">
              <div class="pdp__voucher-left">
                <NIcon size="15" color="#16a34a"><GiftOutline /></NIcon>
                <span>Mã giảm giá</span>
              </div>
              <strong class="pdp__voucher-count">
                {{ validVouchers.length > 0 ? `${validVouchers.length} mã khả dụng` : 'Chưa có mã' }}
              </strong>
              <NIcon size="13" class="pdp__voucher-arrow"><ArrowBack style="transform: rotate(180deg)" /></NIcon>
            </div>

            <!-- Promises -->
            <div class="pdp__promises">
              <div class="pdp__promise">
                <NIcon size="15" color="#16a34a"><ShieldCheckmarkOutline /></NIcon>
                <span>Giao hàng toàn quốc</span>
              </div>
              <div class="pdp__promise">
                <NIcon size="15" color="#16a34a"><ReturnDownBackOutline /></NIcon>
                <span>Đổi trả 30 ngày</span>
              </div>
              <div class="pdp__promise">
                <NIcon size="15" color="#16a34a"><HeadsetOutline /></NIcon>
                <span>Hỗ trợ 24/7</span>
              </div>
              <div class="pdp__promise">
                <NIcon size="15" color="#16a34a"><RocketOutline /></NIcon>
                <span>Giao nhanh 2H</span>
              </div>
            </div>

          </div>
        </div>

        <!-- ═══ RELATED PRODUCTS ═══ -->
        <div v-if="relatedProducts.length > 0" class="pdp__related">
          <h2 class="pdp__related-title">Sản phẩm liên quan</h2>
          <div class="pdp__related-grid">
            <div
              v-for="item in relatedProducts" :key="item.id"
              class="pdp__rcard"
              @click="router.push(`/product-detail/${item.id}`)"
            >
              <div class="pdp__rcard-img">
                <img :src="item.urlImage" :alt="item.name">
                <div v-if="item.percentage" class="pdp__rcard-badge">-{{ item.percentage }}%</div>
              </div>
              <div class="pdp__rcard-body">
                <p class="pdp__rcard-name">{{ item.name }}</p>
                <span class="pdp__rcard-price">{{ formatCurrency(item.price) }}</span>
              </div>
            </div>
          </div>
        </div>

      </template>
    </div>

    <!-- ═══ VOUCHER MODAL ═══ -->
    <NModal v-model:show="showVoucherModal" preset="card" title="Mã giảm giá khả dụng" style="width: 480px">
      <div v-if="validVouchers.length === 0" class="pdp__vmodal-empty">
        <NEmpty description="Chưa có mã giảm giá phù hợp" />
      </div>
      <div v-else class="pdp__vmodal-list">
        <div
          v-for="(v, idx) in validVouchers" :key="v.id"
          class="pdp__vmodal-item" :class="{ 'pdp__vmodal-item--active': selectedVoucher?.id === v.id }"
          @click="handleSelectVoucher(v)"
        >
          <span v-if="idx === 0" class="pdp__vmodal-best">TỐT NHẤT</span>
          <div class="pdp__vmodal-info">
            <span class="pdp__vmodal-code">{{ v.code }}</span>
            <span class="pdp__vmodal-name">{{ v.name }}</span>
            <span class="pdp__vmodal-desc">
              Giảm: {{ v.typeVoucher === 'PERCENTAGE' ? `${v.discountValue}%` : formatCurrency(v.discountValue || 0) }}
              <template v-if="v.maxValue"> · Tối đa: {{ formatCurrency(v.maxValue) }}</template>
            </span>
            <span class="pdp__vmodal-cond">Đơn tối thiểu: {{ formatCurrency(v.conditions || 0) }}</span>
          </div>
          <div class="pdp__vmodal-check" :class="{ 'pdp__vmodal-check--on': selectedVoucher?.id === v.id }">
            <NIcon v-if="selectedVoucher?.id === v.id" color="white" size="13"><CheckmarkCircle /></NIcon>
          </div>
        </div>
      </div>
    </NModal>

    <!-- Compare Modal -->
    <CompareModal v-model:show="showCompareModal" :current-product="product" :options="compareOptions" />
  </div>
</template>

<style scoped>
/* ─── CSS Variables ──────────────────────────────────── */
.pdp {
  --green: #22c55e;
  --green-dark: #16a34a;
  --green-bg: #f0fdf4;
  --green-border: #bbf7d0;
  --blue: #3b82f6;
  --border: #e5e7eb;
  --bg-soft: #f9fafb;
  --text-primary: #111827;
  --text-secondary: #6b7280;
  --text-muted: #9ca3af;
  --radius: 10px;
  --radius-sm: 6px;
  --shadow: 0 1px 3px rgba(0,0,0,.06), 0 1px 2px rgba(0,0,0,.04);
  --shadow-md: 0 4px 6px -1px rgba(0,0,0,.07), 0 2px 4px -1px rgba(0,0,0,.04);

  background: #f3f4f6;
  min-height: 100vh;
  padding: 20px 0 56px;
}

/* ─── Container ──────────────────────────────────────── */
.pdp__container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 16px;
}

/* ─── Breadcrumb ─────────────────────────────────────── */
.pdp__breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.breadcrumb-item { cursor: pointer; transition: color 0.2s; }
.breadcrumb-item:hover { color: #8ac7a0; }
.breadcrumb-item.active { color: #16a34a; font-weight: 600; }
.breadcrumb-separator { color: #cbd5e1; }

/* ─── Loading ────────────────────────────────────────── */
.pdp__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px;
  background: #fff;
  border-radius: var(--radius);
  border: 1px solid var(--border);
}
.pdp__loading p { color: var(--text-secondary); font-size: 14px; margin: 0; }

/* ─── Main Grid ──────────────────────────────────────── */
.pdp__main {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 16px;
  margin-bottom: 16px;
  align-items: start;
}

/* ─── Left Column ────────────────────────────────────── */
.pdp__left { display: flex; flex-direction: column; gap: 12px; }

.pdp__gallery {
  background: #fff;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  padding: 14px;
  box-shadow: var(--shadow);
}

.pdp__img-wrap {
  position: relative;
  padding-top: 100%;
  background: var(--bg-soft);
  border-radius: var(--radius-sm);
  border: 1px solid #f3f4f6;
  overflow: hidden;
}
.pdp__img-wrap img {
  position: absolute;
  inset: 0; width: 100%; height: 100%;
  object-fit: contain;
  transition: transform .3s ease;
}
.pdp__img-wrap:hover img { transform: scale(1.03); }

.pdp__badges {
  position: absolute;
  top: 10px; left: 10px;
  display: flex; flex-direction: column; gap: 5px;
  z-index: 2;
}
.pdp__badge {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 11px; font-weight: 700; color: #fff;
  letter-spacing: .3px;
}
.pdp__badge--sale     { background: var(--green); }
.pdp__badge--upcoming { background: var(--blue); }
.pdp__badge--oos      { background: var(--text-muted); }

/* Specs */
.pdp__specs {
  background: #fff;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  overflow: hidden;
  box-shadow: var(--shadow);
}
.pdp__specs-header {
  padding: 10px 14px;
  font-size: 13px; font-weight: 700; color: var(--text-primary);
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
.sale-timer.ongoing { background: linear-gradient(135deg, rgb(244, 64, 64), #f47940); }
.sale-timer.upcoming { background: linear-gradient(135deg, #1717c6, #1e58af); }
.timer-left { display: flex; align-items: center; gap: 12px; }
.timer-text { display: flex; flex-direction: column; }
.timer-label { font-weight: 800; font-size: 16px; letter-spacing: 0.5px; }
.timer-sub { font-size: 12px; opacity: 0.9; }
.timer-display { background: rgba(0, 0, 0, 0.2); padding: 8px 16px; border-radius: 40px; }
.time-value { font-weight: 700; font-size: 18px; font-family: monospace; }

/* PRICE */
.price-section { padding: 20px; background: #f8fafc; border-radius: 12px; margin-bottom: 24px; }
.price-section.has-sale { background: #f0fdf4; }
.price-wrapper { display: flex; flex-direction: column; gap: 8px; }
.price-current { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.price-current .value { font-size: 32px; font-weight: 800; color: #1e293b; }
.price-current .value.sale-price { color: #16a34a; }
.price-old { display: flex; align-items: center; gap: 8px; font-size: 15px; }
.price-old .value { text-decoration: line-through; color: #94a3b8; }
.upcoming-tag { background: #dbeafe; color: #1d4ed8; padding: 6px 10px; border-radius: 6px; font-size: 13px; font-weight: 600; }

/* VOUCHER CARD */
.highlight-voucher { background: #f0fdf4; border: 1px dashed #16a34a; padding: 16px 20px; border-radius: 12px; margin-bottom: 24px; cursor: pointer; transition: all 0.2s; }
.highlight-voucher:hover { background: #dcfce7; border-style: solid; }
.voucher-header { display: flex; align-items: center; gap: 8px; color: #15803d; font-weight: 700; margin-bottom: 8px; }
.voucher-preview { display: flex; align-items: center; justify-content: space-between; }
.voucher-count { color: #16a34a; font-weight: 600; font-size: 14px; }
.text-gray { color: #64748b !important; font-weight: 400 !important; }

/* SHORT SPECS */
.short-specs { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; padding: 20px; background: #f8fafc; border-radius: 12px; margin-bottom: 24px; border: 1px solid #e2e8f0; }
.spec-item { display: flex; gap: 6px; font-size: 14px; }
.spec-label { color: #64748b; font-weight: 500; min-width: 70px; }
.spec-value { color: #0f172a; font-weight: 600; }

/* VARIANTS */
.variants-section { margin-bottom: 24px; }
.variants-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.variants-title { font-size: 16px; font-weight: 700; color: #0f172a; margin: 0; }
.reset-filter-btn { display: flex; align-items: center; gap: 6px; padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: white; font-size: 13px; color: #64748b; cursor: pointer; transition: all 0.2s; font-weight: 500; }
.reset-filter-btn:hover { background: #f8fafc; color: #0f172a; border-color: #cbd5e1; }
.variants-grid { display: flex; flex-direction: column; gap: 16px; }
.variant-group { display: flex; align-items: flex-start; gap: 12px; }
.variant-label { min-width: 70px; font-size: 14px; font-weight: 600; color: #475569; padding-top: 8px; }
.variant-options { display: flex; flex-wrap: wrap; gap: 10px; flex: 1; }
.variant-chip { padding: 8px 16px; border: 1px solid #cbd5e1; border-radius: 8px; background: white; font-size: 14px; font-weight: 500; color: #334155; cursor: pointer; transition: all 0.2s; }
.variant-chip:hover { border-color: #16a34a; color: #16a34a; }
.variant-chip.active { border-color: #16a34a; background: #f0fdf4; color: #15803d; font-weight: 700; box-shadow: 0 0 0 1px #16a34a; }

/* ACTIONS (MUA NGAY - THÊM GIỎ HÀNG) */
.actions-section { border-top: 1px solid #e2e8f0; padding-top: 24px; margin-top: 24px; }
.quantity-box { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.quantity-label { font-weight: 600; color: #0f172a; }
.stock-info { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #16a34a; background: #f0fdf4; padding: 6px 12px; border-radius: 20px; font-weight: 500; }

.action-buttons { display: flex; gap: 16px; }

.btn-buy-now { flex: 2; background: #16a34a !important; color: white !important; border: none !important; font-weight: 800; font-size: 16px; height: 56px; border-radius: 12px; transition: all 0.2s ease; box-shadow: 0 4px 12px rgba(22, 163, 74, 0.2); }
.btn-buy-now:hover { background: #15803d !important; transform: translateY(-2px); box-shadow: 0 6px 16px rgba(22, 163, 74, 0.3); }

.btn-add-cart { flex: 1; background: transparent !important; border: 2px solid #2563eb !important; color: #2563eb !important; font-weight: 700; height: 56px; border-radius: 12px; transition: all 0.2s ease; }
.btn-add-cart:hover { background: #eff6ff !important; border-color: #1d4ed8 !important; color: #1d4ed8 !important; }

.btn-disabled { flex: 1; background: #f8fafc !important; border: 2px dashed #cbd5e1 !important; color: #94a3b8 !important; font-weight: 700; height: 56px; border-radius: 12px; cursor: not-allowed; }

/* TABS */
.product-tabs { background: white; border-radius: 16px; padding: 24px 32px; margin-bottom: 40px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03); border: 1px solid #f1f5f9; }
.tab-content { padding: 24px 0; }

/* RELATED PRODUCTS */
.section-title { font-size: 22px; font-weight: 800; color: #0f172a; margin-bottom: 24px; }
.related-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 16px; }
.related-card { background: white; border-radius: 12px; overflow: hidden; cursor: pointer; transition: all 0.2s ease; border: 1px solid #e2e8f0; }
.related-card:hover { transform: translateY(-4px); box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05); border-color: #16a34a; }
.card-image { position: relative; padding-top: 100%; background: #f8fafc; }
.card-image img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: contain; padding: 16px; transition: transform 0.3s ease; }
.related-card:hover .card-image img { transform: scale(1.05); }
.card-badge { position: absolute; top: 8px; left: 8px; background: #ef4444; color: white; font-size: 11px; font-weight: 700; padding: 4px 8px; border-radius: 6px; }
.card-info { padding: 16px; }
.card-name { font-size: 14px; font-weight: 600; color: #0f172a; margin-bottom: 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 42px; line-height: 1.4; }
.card-price { display: flex; flex-direction: column; gap: 4px; }
.card-price .current { font-size: 15px; font-weight: 800; color: #16a34a; }
.card-price .old { font-size: 13px; color: #94a3b8; text-decoration: line-through; }

/* VOUCHER MODAL LIST */
.voucher-list { max-height: 450px; overflow-y: auto; padding: 4px; }
.voucher-item { position: relative; display: flex; padding: 16px; border: 1px solid #e2e8f0; border-radius: 12px; margin-bottom: 16px; cursor: pointer; transition: all 0.2s; background: white; }
.voucher-item:hover { border-color: #16a34a; background: #c7eed3; box-shadow: 0 4px 12px rgba(22, 163, 74, 0.05); }
.voucher-item.selected { border-color: #16a34a; background: #f0fdf4; }
.voucher-badge { position: absolute; top: -10px; right: -10px; background: #f59e0b; color: white; font-size: 10px; font-weight: 800; padding: 4px 10px; border-radius: 12px; z-index: 2; box-shadow: 0 2px 4px rgba(245, 158, 11, 0.2); }
.voucher-left { flex: 1; }
.voucher-code { display: block; font-weight: 800; color: #16a34a; margin-bottom: 6px; font-size: 15px; }
.voucher-name { display: block; font-size: 14px; font-weight: 600; color: #0f172a; margin-bottom: 8px; }
.voucher-desc { font-size: 13px; color: #475569; margin-bottom: 4px; }
.voucher-condition { font-size: 12px; color: #64748b; }
.voucher-right { display: flex; align-items: center; padding-left: 20px; }
.voucher-check { width: 24px; height: 24px; border-radius: 50%; border: 2px solid #cbd5e1; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.voucher-check.checked { background: #16a34a; border-color: #16a34a; }

/* RESPONSIVE */
@media (max-width: 1024px) {
  .product-main { grid-template-columns: 1fr; }
  .related-grid { grid-template-columns: repeat(3, 1fr); }
}

/* ─── Right Column ───────────────────────────────────── */
.pdp__right {
  background: #fff;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  padding: 22px;
  display: flex; flex-direction: column; gap: 16px;
  box-shadow: var(--shadow);
}

/* Header */
.pdp__header { display: flex; flex-direction: column; gap: 4px; }
.pdp__title {
  font-size: 19px; font-weight: 700;
  color: var(--text-primary); line-height: 1.4; margin: 0;
}
.pdp__code { font-size: 12px; color: var(--text-muted); }
.pdp__code strong { color: var(--text-secondary); }

/* Timer */
.pdp__timer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; border-radius: var(--radius-sm); color: #fff;
}
.pdp__timer--sale     { background: linear-gradient(135deg, #16a34a 0%, #22c55e 100%); }
.pdp__timer--upcoming { background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%); }
.pdp__timer-left  { display: flex; align-items: center; gap: 10px; }
.pdp__timer-label { font-weight: 700; font-size: 12px; letter-spacing: .5px; }
.pdp__timer-sub   { font-size: 11px; opacity: .85; margin-top: 1px; }
.pdp__timer-value {
  font-family: 'Courier New', monospace;
  font-size: 17px; font-weight: 700;
  background: rgba(0,0,0,.18);
  padding: 4px 12px; border-radius: 5px;
  letter-spacing: 1px;
}

/* Price */
.pdp__price {
  padding: 14px 16px;
  background: var(--bg-soft);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}
.pdp__price--sale { background: var(--green-bg); border-color: var(--green-border); }
.pdp__price-original { font-size: 12.5px; color: var(--text-muted); text-decoration: line-through; margin-bottom: 3px; }
.pdp__price-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.pdp__price-main { font-size: 28px; font-weight: 800; color: var(--text-primary); line-height: 1; }
.pdp__price-main--sale { color: var(--green-dark); }
.pdp__price-badge {
  background: var(--green); color: #fff;
  font-size: 11px; font-weight: 700;
  padding: 3px 8px; border-radius: 20px;
}
.pdp__price-badge--blue { background: var(--blue); }

/* Variants */
.pdp__variants {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.pdp__variants-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 9px 12px;
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
}
.pdp__variants-title { font-size: 12.5px; font-weight: 700; color: var(--text-primary); }
.pdp__reset-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 3px 9px; border: 1px solid var(--border); border-radius: 4px;
  background: #fff; font-size: 11.5px; color: var(--text-secondary);
  cursor: pointer; transition: background .15s;
}
.pdp__reset-btn:hover { background: var(--bg-soft); }
.pdp__variants-body { padding: 10px 12px; display: flex; flex-direction: column; gap: 10px; }
.pdp__vgroup { display: flex; align-items: flex-start; gap: 10px; }
.pdp__vgroup-label {
  min-width: 60px; flex-shrink: 0;
  font-size: 12px; font-weight: 600; color: var(--text-secondary);
  padding-top: 5px;
}
.pdp__chips { display: flex; flex-wrap: wrap; gap: 6px; }
.pdp__chip {
  padding: 4px 11px;
  border: 1px solid var(--border); border-radius: 5px;
  background: #fff; font-size: 12px; color: var(--text-primary);
  cursor: pointer; transition: all .15s;
}
.pdp__chip:hover { border-color: var(--green); color: var(--green-dark); }
.pdp__chip--active {
  border-color: var(--green);
  background: var(--green-bg);
  color: var(--green-dark);
  font-weight: 600;
}

/* Quantity */
.pdp__qty { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.pdp__qty-label { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.pdp__stock {
  display: flex; align-items: center; gap: 5px;
  font-size: 12.5px; color: var(--green-dark);
}

/* Actions */
.pdp__actions { display: flex; gap: 10px; }
.pdp__btn {
  flex: 1;
  font-weight: 700 !important;
  height: 46px !important;
  border-radius: 8px !important;
  font-size: 14px !important;
  letter-spacing: .3px;
  transition: opacity .15s, transform .1s !important;
}
.pdp__btn:active { transform: scale(.98); }
.pdp__btn--buy  { background: var(--green) !important; border: none !important; color: #fff !important; }
.pdp__btn--buy:hover  { background: var(--green-dark) !important; }
.pdp__btn--cart { background: #fff !important; border: 1.5px solid var(--green) !important; color: var(--green-dark) !important; }
.pdp__btn--cart:hover { background: var(--green-bg) !important; }
.pdp__btn--oos  { background: #f3f4f6 !important; border: none !important; color: var(--text-muted) !important; }

/* Voucher */
.pdp__voucher {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border: 1px dashed var(--green-border);
  border-radius: var(--radius-sm);
  background: var(--green-bg);
  cursor: pointer; font-size: 13px; color: var(--text-primary);
  transition: background .15s;
}
.pdp__voucher:hover { background: #dcfce7; }
.pdp__voucher-left { display: flex; align-items: center; gap: 6px; }
.pdp__voucher-count { color: var(--green-dark); margin-left: 2px; }
.pdp__voucher-arrow { margin-left: auto; color: var(--text-muted); }

/* Promises */
.pdp__promises {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  padding: 12px 14px;
  background: var(--bg-soft);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}
.pdp__promise {
  display: flex; align-items: center; gap: 7px;
  font-size: 12.5px; color: var(--text-secondary);
}

/* ─── Related Products ───────────────────────────────── */
.pdp__related { margin-bottom: 16px; }
.pdp__related-title {
  font-size: 16px; font-weight: 700;
  color: var(--text-primary); margin-bottom: 12px;
}
.pdp__related-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}
.pdp__rcard {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  overflow: hidden; cursor: pointer;
  transition: border-color .15s, box-shadow .15s, transform .15s;
}
.pdp__rcard:hover {
  border-color: var(--green);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.pdp__rcard-img { position: relative; padding-top: 100%; background: #f9fafb; }
.pdp__rcard-img img {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  object-fit: contain; padding: 8px;
}
.pdp__rcard-badge {
  position: absolute; top: 6px; left: 6px;
  background: var(--green); color: #fff;
  font-size: 10px; font-weight: 700;
  padding: 2px 6px; border-radius: 4px;
}
.pdp__rcard-body { padding: 8px 10px; }
.pdp__rcard-name {
  font-size: 12px; font-weight: 500; color: var(--text-primary);
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  min-height: 32px; margin-bottom: 5px;
}
.pdp__rcard-price { font-size: 13px; font-weight: 700; color: var(--green-dark); }

/* ─── Voucher Modal ──────────────────────────────────── */
.pdp__vmodal-empty { padding: 32px; text-align: center; }
.pdp__vmodal-list {
  max-height: 420px; overflow-y: auto;
  display: flex; flex-direction: column; gap: 8px;
}
.pdp__vmodal-item {
  position: relative; display: flex; align-items: center; gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  cursor: pointer; transition: all .15s;
}
.pdp__vmodal-item:hover,
.pdp__vmodal-item--active { border-color: var(--green); background: var(--green-bg); }

.pdp__vmodal-best {
  position: absolute; top: -8px; right: 10px;
  background: #f59e0b; color: #fff;
  font-size: 10px; font-weight: 700;
  padding: 2px 8px; border-radius: 10px;
  letter-spacing: .3px;
}
.pdp__vmodal-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.pdp__vmodal-code { font-weight: 700; color: var(--green-dark); font-size: 12.5px; }
.pdp__vmodal-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.pdp__vmodal-desc { font-size: 12.5px; color: var(--text-secondary); }
.pdp__vmodal-cond { font-size: 11.5px; color: var(--text-muted); }

.pdp__vmodal-check {
  width: 22px; height: 22px; border-radius: 50%;
  border: 2px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; transition: all .15s;
}
.pdp__vmodal-check--on { background: var(--green); border-color: var(--green); }

/* ─── Responsive ─────────────────────────────────────── */
@media (max-width: 860px) {
  .pdp__main { grid-template-columns: 1fr; }
  .pdp__related-grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 600px) {
  .pdp__related-grid { grid-template-columns: repeat(3, 1fr); }
  .pdp__actions { flex-direction: column; }
  .pdp__promises { grid-template-columns: 1fr; }
}
@media (max-width: 400px) {
  .pdp__related-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>