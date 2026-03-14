<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NCheckbox,
  NCheckboxGroup,
  NDivider,
  NEllipsis,
  NEmpty,
  NGrid,
  NGridItem,
  NPagination,
  NSelect,
  NSpace,
  NSpin,
} from 'naive-ui'

import NovaIcon from '@/components/common/NovaIcon.vue'

// Import API
import {
  getCPUs,
  getGPUs,
  getHardDrives,
  getMinMaxPrice,
  getProductDetails,
  getRAMs,
} from '@/service/api/client/product/productDetail.api'

import type {
  ADProductDetailRequest,
  ADProductDetailResponse,
  ADPRPropertiesComboboxResponse,
} from '@/service/api/client/product/productDetail.api'

import { getBrands, getScreens } from '@/service/api/client/product/product.api'

const route = useRoute()
const router = useRouter()

// MÀU CHỦ ĐẠO CHO TOÀN BỘ TRANG SẢN PHẨM: XANH LÁ TƯƠI MÁT
const themeOverrides = {
  common: {
    primaryColor: '#16a34a',
    primaryColorHover: '#15803d',
    primaryColorPressed: '#166534',
    primaryColorSuppl: '#15803d',
  },
  Input: {
    borderHover: '1px solid #16a34a',
    borderFocus: '1px solid #16a34a',
    boxShadowFocus: '0 0 0 2px rgba(22, 163, 74, 0.2)',
  },
  Select: {
    peers: {
      InternalSelection: {
        borderHover: '1px solid #16a34a',
        borderFocus: '1px solid #16a34a',
        boxShadowFocus: '0 0 0 2px rgba(22, 163, 74, 0.2)',
      },
    },
  },
}

// --- STATE QUẢN LÝ DỮ LIỆU ---
const productDetails = ref<ADProductDetailResponse[]>([])
const loading = ref(false)
const isFetchingOptions = ref(true)

// --- OPTIONS CHO BỘ LỌC ---
const ramOptions = ref<ADPRPropertiesComboboxResponse[]>([])
const cpuOptions = ref<ADPRPropertiesComboboxResponse[]>([])
const gpuOptions = ref<ADPRPropertiesComboboxResponse[]>([])
const hardDriveOptions = ref<ADPRPropertiesComboboxResponse[]>([])
const brandOptions = ref<ADPRPropertiesComboboxResponse[]>([])
const screenOptions = ref<ADPRPropertiesComboboxResponse[]>([])

// --- MAP HIỂN THỊ HÃNG ---
function normalizeBrandKey(label: string) {
  return (label || '').trim().toLowerCase()
}

function brandVisual(label: string) {
  const key = normalizeBrandKey(label)
  if (key.includes('apple') || key.includes('mac')) {
    return { type: 'icon' as const, icon: 'icon-park-outline:apple', textClass: 'text-black' }
  }
  const map: Record<string, string> = {
    samsung: 'text-blue-700 font-black tracking-tighter',
    xiaomi: 'text-orange-500 font-bold text-xl',
    vivo: 'text-blue-500 font-bold text-xl',
    oppo: 'text-green-600 font-bold text-xl',
    asus: 'text-black font-black text-xl',
    dell: 'text-blue-800 font-black text-xl',
    hp: 'text-blue-500 font-bold text-xl',
    lenovo: 'text-red-600 font-bold text-xl',
    acer: 'text-green-600 font-bold text-xl',
    msi: 'text-red-600 font-black text-xl',
  }
  for (const k of Object.keys(map)) {
    if (key.includes(k))
      return { type: 'text' as const, textClass: map[k] }
  }
  return { type: 'text' as const, textClass: 'text-gray-800 font-bold' }
}

const minMaxPriceLimit = ref<[number, number]>([0, 100000000])
const selectedPriceDropdown = ref<string | null>(null)
const priceOptions = [
  { label: 'Dưới 15 triệu', value: '0-15000000' },
  { label: 'Từ 15 - 20 triệu', value: '15000000-20000000' },
  { label: 'Từ 20 - 30 triệu', value: '20000000-30000000' },
  { label: 'Từ 30 - 50 triệu', value: '30000000-50000000' },
  { label: 'Trên 50 triệu', value: '50000000-1000000000' },
]

// --- BỘ LỌC TÌM KIẾM ---
const filters = ref({
  q: (route.query.q as string) || '',
  idBrand: null as string | null,
  idScreen: [] as string[],
  idRAM: [] as string[],
  idCPU: [] as string[],
  idGPU: [] as string[],
  idHardDrive: [] as string[],
  priceRange: [0, 100000000] as [number, number],
})

const pagination = ref({ page: 1, size: 12, totalPages: 1 })

// --- THEO DÕI TOÀN BỘ QUERY URL ---
watch(
  () => route.query,
  (newQuery) => {
    filters.value.q = (newQuery.q as string) || ''
    filters.value.idBrand = (newQuery.brand as string) || null
  },
  { immediate: true },
)

// --- HÀM LẤY OPTIONS ---
async function fetchAllOptions() {
  try {
    isFetchingOptions.value = true
    const [rams, cpus, gpus, drives, brands, screens, priceRange] = await Promise.all([
      getRAMs(),
      getCPUs(),
      getGPUs(),
      getHardDrives(),
      getBrands(),
      getScreens(),
      getMinMaxPrice(),
    ])
    if (rams.data)
      ramOptions.value = rams.data
    if (cpus.data)
      cpuOptions.value = cpus.data
    if (gpus.data)
      gpuOptions.value = gpus.data
    if (drives.data)
      hardDriveOptions.value = drives.data
    if (brands.data)
      brandOptions.value = brands.data
    if (screens.data)
      screenOptions.value = screens.data
    if (priceRange.data) {
      const { priceMin, priceMax } = priceRange.data
      minMaxPriceLimit.value = [priceMin, priceMax]
      filters.value.priceRange = [priceMin, priceMax]
    }
  }
  catch (error) {
    console.error('Lỗi khi tải bộ lọc:', error)
  }
  finally {
    isFetchingOptions.value = false
  }
}

// --- HÀM FETCH SẢN PHẨM ---
async function fetchProducts() {
  loading.value = true
  try {
    const params: ADProductDetailRequest = {
      page: pagination.value.page,
      size: pagination.value.size,
      q: filters.value.q || undefined,
      idRAM: filters.value.idRAM.length > 0 ? filters.value.idRAM.join(',') : null,
      idCPU: filters.value.idCPU.length > 0 ? filters.value.idCPU.join(',') : null,
      idGPU: filters.value.idGPU.length > 0 ? filters.value.idGPU.join(',') : null,
      idHardDrive: filters.value.idHardDrive.length > 0 ? filters.value.idHardDrive.join(',') : null,
      idScreen: filters.value.idScreen.length > 0 ? filters.value.idScreen.join(',') : null,
      minPrice: filters.value.priceRange[0],
      maxPrice: filters.value.priceRange[1],
      idBrand: filters.value.idBrand,
      // [THÊM]: Gửi kèm thời gian hiện tại để backend lọc discount chuẩn
      time: new Date().getTime(),
    }

    const res = await getProductDetails(params)
    if (res?.data?.data) {
      productDetails.value = res.data.data
      pagination.value.totalPages = res.data.totalPages || 1
    }
    else {
      productDetails.value = []
      pagination.value.totalPages = 1
    }
  }
  catch (error) {
    console.error('Lỗi khi tải sản phẩm:', error)
    productDetails.value = []
  }
  finally {
    loading.value = false
  }
}

// Thêm ref để trỏ tới thẻ div chứa danh sách hãng
const brandGridRef = ref<HTMLElement | null>(null)

// Hàm xử lý khi bấm nút trái/phải
function scrollBrands(direction: number) {
  if (brandGridRef.value) {
    // Mỗi lần bấm sẽ cuộn khoảng 300px (tương đương 2 cột)
    const scrollAmount = 300 * direction
    brandGridRef.value.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}

// Hàm chuyển đổi lăn chuột dọc thành cuộn ngang
function handleWheel(e: WheelEvent) {
  if (brandGridRef.value) {
    // Chặn cuộn trang dọc khi đang lăn chuột trong khu vực hãng
    e.preventDefault()
    // Cộng dồn khoảng cách lăn chuột vào thanh cuộn ngang
    brandGridRef.value.scrollLeft += e.deltaY
  }
}

function toggleBrand(brandId: string) {
  filters.value.idBrand = filters.value.idBrand === brandId ? null : brandId
}

function handlePriceSelect(val: string | null) {
  if (!val) {
    filters.value.priceRange = [...minMaxPriceLimit.value] as [number, number]
  }
  else {
    const [min, max] = val.split('-').map(Number)
    filters.value.priceRange = [min, max]
  }
}

function handlePageChange(newPage: number) {
  pagination.value.page = newPage
  fetchProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

function resetFilters() {
  selectedPriceDropdown.value = null
  if (route.query.q || route.query.brand) {
    router.replace({ path: '/san-pham' })
  }
  filters.value = {
    q: '',
    idBrand: null,
    idScreen: [],
    idRAM: [],
    idCPU: [],
    idGPU: [],
    idHardDrive: [],
    priceRange: [...minMaxPriceLimit.value] as [number, number],
  }
  pagination.value.page = 1
}

watch(() => filters.value, () => {
  pagination.value.page = 1
  fetchProducts()
}, { deep: true })

onMounted(async () => {
  await fetchAllOptions()
  await fetchProducts()
})

// [SỬA]: Truyền toàn bộ thông tin giảm giá (nếu có) sang URL trang chi tiết
function handleClickProduct(item: ADProductDetailResponse) {
  router.push({
    path: `/product-detail/${item.id}`,
    query: {
      pct: item.percentage,
      sd: item.startDate,
      ed: item.endDate,
    },
  })
}
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <div class="product-page-wrapper">
      <div class="brand-horizontal-section">
        <h2 class="section-title">
          <span class="text-red-500 mr-2">|</span>CHỌN THEO HÃNG
        </h2>
        <NSpin :show="isFetchingOptions">
          <div class="brand-scroll-wrapper">
            <button class="scroll-btn left-btn" @click="scrollBrands(-1)">
              <NovaIcon icon="icon-park-outline:left" :size="20" />
            </button>

            <div
              ref="brandGridRef"
              class="brand-grid"
              @wheel.prevent="handleWheel"
            >
              <button
                v-for="b in brandOptions" :key="b.value" class="brand-card"
                :class="{ active: filters.idBrand === b.value }" @click="toggleBrand(b.value as string)"
              >
                <template v-if="brandVisual(b.label as string).type === 'icon'">
                  <NovaIcon :icon="brandVisual(b.label as string).icon" :size="26" class="text-black" />
                </template>
                <template v-else>
                  <span :class="brandVisual(b.label as string).textClass">{{ b.label }}</span>
                </template>
              </button>
            </div>

            <button class="scroll-btn right-btn" @click="scrollBrands(1)">
              <NovaIcon icon="icon-park-outline:right" :size="20" />
            </button>
          </div>
        </NSpin>
      </div>

      <div class="main-content-layout">
        <div class="left-sidebar">
          <div class="sidebar-header">
            <h3 class="sidebar-title">
              BỘ LỌC SẢN PHẨM
            </h3>
            <NButton text type="error" style="font-size: 13px;" @click="resetFilters">
              Xóa tất cả
            </NButton>
          </div>

          <NSpin :show="isFetchingOptions">
            <div class="filter-group">
              <label class="filter-label">Mức giá</label>
              <NSelect
                v-model:value="selectedPriceDropdown" :options="priceOptions" placeholder="Chọn mức giá" clearable
                @update:value="handlePriceSelect"
              />
            </div>

            <NDivider style="margin: 16px 0;" />

            <div class="filter-group">
              <label class="filter-label">Dòng CPU</label>
              <div class="checkbox-scroll-area">
                <NCheckboxGroup v-model:value="filters.idCPU">
                  <NSpace vertical>
                    <NCheckbox v-for="opt in cpuOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </NCheckbox>
                  </NSpace>
                </NCheckboxGroup>
              </div>
            </div>

            <div class="filter-group">
              <label class="filter-label">Card đồ họa (VGA)</label>
              <div class="checkbox-scroll-area">
                <NCheckboxGroup v-model:value="filters.idGPU">
                  <NSpace vertical>
                    <NCheckbox v-for="opt in gpuOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </NCheckbox>
                  </NSpace>
                </NCheckboxGroup>
              </div>
            </div>

            <div class="filter-group">
              <label class="filter-label">Dung lượng RAM</label>
              <div class="checkbox-scroll-area">
                <NCheckboxGroup v-model:value="filters.idRAM">
                  <NSpace vertical>
                    <NCheckbox v-for="opt in ramOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </NCheckbox>
                  </NSpace>
                </NCheckboxGroup>
              </div>
            </div>

            <div class="filter-group">
              <label class="filter-label">Ổ cứng</label>
              <div class="checkbox-scroll-area">
                <NCheckboxGroup v-model:value="filters.idHardDrive">
                  <NSpace vertical>
                    <NCheckbox v-for="opt in hardDriveOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </NCheckbox>
                  </NSpace>
                </NCheckboxGroup>
              </div>
            </div>

            <div class="filter-group">
              <label class="filter-label">Màn hình</label>
              <div class="checkbox-scroll-area">
                <NCheckboxGroup v-model:value="filters.idScreen">
                  <NSpace vertical>
                    <NCheckbox v-for="opt in screenOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </NCheckbox>
                  </NSpace>
                </NCheckboxGroup>
              </div>
            </div>
          </NSpin>
        </div>

        <main class="product-list-section">
          <div class="list-header" style="justify-content: space-between; align-items: center;">
            <h3 v-if="filters.q" style="margin: 0; font-size: 16px; color: #333;">
              Kết quả cho: <span style="color: #049d14;">"{{ filters.q }}"</span>
            </h3>
            <div v-else />
            <span class="result-text">Tìm thấy <strong>{{ productDetails.length }}</strong> sản phẩm</span>
          </div>

          <div v-if="loading" class="loading-container">
            <NSpin size="large" description="Đang tải danh sách sản phẩm..." />
          </div>

          <template v-else>
            <div v-if="productDetails.length === 0" class="empty-state">
              <NEmpty description="Không có sản phẩm nào khớp với tiêu chí tìm kiếm." />
            </div>

            <div v-else>
              <NGrid x-gap="16" y-gap="16" cols="1 s:2 m:3 lg:4" responsive="screen">
                <NGridItem v-for="item in productDetails" :key="item.id">
                  <NCard
                    hoverable class="product-card"
                    content-style="padding: 16px; display: flex; flex-direction: column; height: 100%;"
                    @click="handleClickProduct(item)"
                  >
                    <div v-if="item.percentage" class="discount-badge">
                      -{{ item.percentage }}%
                    </div>

                    <div class="image-wrapper">
                      <img
                        :src="item.urlImage || 'https://via.placeholder.com/300'" :alt="item.name"
                        class="product-image"
                      >
                    </div>

                    <div class="product-info">
                      <h3 class="product-name">
                        <NEllipsis :line-clamp="2" :tooltip="{ placement: 'top', style: 'max-width: 300px' }">
                          {{ item.name }}
                        </NEllipsis>
                      </h3>

                      <div class="specs-list">
                        <div v-if="item.cpu" class="spec-item">
                          <span class="spec-label">CPU:</span>
                          <NEllipsis class="spec-value" :tooltip="{ placement: 'top' }">
                            {{ item.cpu }}
                          </NEllipsis>
                        </div>

                        <div v-if="item.gpu" class="spec-item">
                          <span class="spec-label">GPU:</span>
                          <NEllipsis class="spec-value" :tooltip="{ placement: 'top' }">
                            {{ item.gpu }}
                          </NEllipsis>
                        </div>

                        <div v-if="item.ram" class="spec-item">
                          <span class="spec-label">RAM:</span>
                          <NEllipsis class="spec-value" :tooltip="{ placement: 'top' }">
                            {{ item.ram }}
                          </NEllipsis>
                        </div>

                        <div v-if="item.hardDrive" class="spec-item">
                          <span class="spec-label">Ổ cứng:</span>
                          <NEllipsis class="spec-value" :tooltip="{ placement: 'top' }">
                            {{ item.hardDrive }}
                          </NEllipsis>
                        </div>
                      </div>

                      <div class="price-section">
                        <div class="old-price">
                          {{ item.percentage ? formatCurrency(item.price) : '' }}
                        </div>
                        <div class="current-price">
                          {{ formatCurrency(item.price * (1 - (item.percentage || 0) / 100)) }}
                        </div>
                      </div>
                    </div>
                  </NCard>
                </NGridItem>
              </NGrid>

              <div v-if="pagination.totalPages > 1" class="pagination-container">
                <NPagination
                  v-model:page="pagination.page" :page-count="pagination.totalPages" size="large"
                  @update:page="handlePageChange"
                />
              </div>
            </div>
          </template>
        </main>
      </div>
    </div>
  </NConfigProvider>
</template>

<style scoped>
.product-page-wrapper {
  padding: 5px;
  background-color: #ffffff;
  min-height: 100vh;
  max-width: 1400px;
  margin: 0 auto;
}

.brand-horizontal-section {
  background: #fff;
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 800;
  color: #333;
  margin: 0 0 16px 0;
}

/* =========================================
   WRAPPER VÀ NÚT BẤM CUỘN NGANG (CHỮ NHẬT ĐỨNG, RA XA HƠN)
   ========================================= */
.brand-scroll-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  /* Tăng khoảng lề hai bên để có không gian nhét nút bấm ra xa */
  padding: 0 24px;
}

.scroll-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 30px;  /* Nút hẹp lại */
  height: 50px; /* Nút cao lên thành hình chữ nhật đứng */
  border-radius: 10px; /* Bo góc mềm mại */
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.scroll-btn:hover {
  background-color: #00a651;
  color: #ffffff;
  border-color: #54b683;
  box-shadow: 0 4px 12px rgba(0, 166, 81, 0.25);
  /* Hiệu ứng trồi lên một chút khi hover cho sống động */
  transform: translateY(-50%) scale(1.15);
}

/* Kéo nút ra hẳn bên ngoài mép để thoáng thẻ bên trong */
.left-btn {
  left: -15px;
}

.right-btn {
  right: -15px;
}

/* =========================================
   GIAO DIỆN LƯỚI 5x2 CUỘN NGANG
   ========================================= */
.brand-grid {
  display: grid;
  grid-template-rows: repeat(2, 60px);
  grid-auto-flow: column;
  gap: 12px;

  overflow-x: auto;
  scroll-behavior: smooth;
  scroll-snap-type: x mandatory;
  padding: 4px 0;
  width: 100%;

  grid-auto-columns: calc((100% - 24px) / 2.5);

  -ms-overflow-style: none;
  scrollbar-width: none;
}

@media (min-width: 768px) {
  .brand-grid {
    grid-auto-columns: calc((100% - 48px) / 5);
  }
}

.brand-grid::-webkit-scrollbar {
  display: none;
}

/* =========================================
   STYLE CHO TỪNG Ô (BO TRÒN, MỀM MẠI)
   ========================================= */
.brand-card {
  scroll-snap-align: start;
  background: #ffffff;
  border-radius: 16px;
  border: 1.5px solid #e5e7eb;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
}

.brand-card span {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.2;
}

.brand-card:hover {
  border-color: #00a651;
  background-color: #f8fafc;
  box-shadow: 0 4px 12px rgba(0, 166, 81, 0.1);
  transform: translateY(-2px);
}

.brand-card.active {
  background: #ecfdf5;
  border-color: #00a651;
  box-shadow: 0 0 0 1px #00a651;
}

.main-content-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.left-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  height: fit-content;
  align-self: flex-start;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  color: #1f2937;
}

.filter-group {
  margin-bottom: 20px;
}

.filter-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 12px;
  text-transform: uppercase;
}

.checkbox-scroll-area {
  max-height: 200px;
  overflow-y: auto;
  padding-right: 8px;
}

.checkbox-scroll-area::-webkit-scrollbar {
  width: 4px;
}

.checkbox-scroll-area::-webkit-scrollbar-thumb {
  background-color: #e5e7eb;
  border-radius: 4px;
}

/* =========================================
   CUSTOM CSS: CHECKBOX HOVER MÀU XANH LÁ
   ========================================= */
:deep(.n-checkbox.n-checkbox--checked .n-checkbox-box) {
  background-color: #00a651;
  border-color: #00a651;
}

:deep(.n-checkbox .n-checkbox-box:hover) {
  border-color: #00a651;
}

.product-list-section {
  flex-grow: 1;
  min-width: 0;
}

.list-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.result-text {
  font-size: 14px;
  color: #6b7280;
}

.loading-container,
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  background: #ffffff;
  border-radius: 12px;
}

.product-card {
  height: 100%;
  border-radius: 12px;
  border: 1px solid #2c53172e;
  background: #fff;
  position: relative;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
  border-color: #00a651;
}

.discount-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background-color: #ef4444;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 4px;
  z-index: 10;
}

.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 80%;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.product-info {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  margin: 0 0 12px 0;
  color: #222;
  min-height: 42px;
}

.specs-list {
  background-color: #ffff;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 16px;
  flex-grow: 1;
  border: 1px solid #ccf6e1;
}

.spec-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  margin-bottom: 6px;
}

.spec-item:last-child {
  margin-bottom: 0;
}

.spec-label {
  font-weight: 600;
  color: #888;
  width: 65px;
  flex-shrink: 0;
}

.spec-value {
  flex-grow: 1;
  width: 0;
  color: #333;
  font-weight: 500;
}

.price-section {
  margin-top: auto;
}

.old-price {
  font-size: 13px;
  color: #9ca3af;
  text-decoration: line-through;
  min-height: 19px;
  margin-bottom: 2px;
}

.current-price {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
}

.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 40px;
  padding-bottom: 20px;
}

/* =========================================
   CUSTOM CSS: PAGINATION THEO YÊU CẦU
   ========================================= */

/* Trạng thái trang đang được chọn (Active) -> Xanh lá */
:deep(.n-pagination .n-pagination-item--active) {
  color: #00a651 !important;
  border-color: #00a651 !important;
  background-color: #ecfdf5 !important;
}

/* Trạng thái Hover vào bất kỳ trang nào (kể cả trang Active) -> Đỏ */
:deep(.n-pagination .n-pagination-item:hover) {
  color: #ef4444 !important;
  border-color: #ef4444 !important;
  background-color: #fff1f2 !important;
  transition: all 0.3s ease;
}

/* Đổi màu hover cho 2 nút Prev / Next (Mũi tên sang trái/phải) thành Đỏ */
:deep(.n-pagination .n-pagination-quick-jumper:hover),
:deep(.n-pagination .n-pagination-item--button:hover) {
  color: #ef4444 !important;
  border-color: #ef4444 !important;
}

@media (max-width: 992px) {
  .main-content-layout {
    flex-direction: column;
  }

  .left-sidebar {
    width: 100%;
  }
}
</style>
