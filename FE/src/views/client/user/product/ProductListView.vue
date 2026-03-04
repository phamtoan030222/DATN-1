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

const minMaxPriceLimit = ref<[number, number]>([0, 100000000])

const selectedPriceDropdown = ref<string | null>(null)
const priceOptions = [
  { label: 'Dưới 15 triệu', value: '0-15000000' },
  { label: 'Từ 15 - 20 triệu', value: '15000000-20000000' },
  { label: 'Từ 20 - 30 triệu', value: '20000000-30000000' },
  { label: 'Trên 30 triệu', value: '30000000-100000000' },
]

// --- STATE BỘ LỌC CHUYỂN SANG MẢNG (ARRAY) ĐỂ CHỌN NHIỀU ---
const filters = ref({
  idBrand: null as string | null,
  idScreen: [] as string[],
  idRAM: [] as string[],
  idCPU: [] as string[],
  idGPU: [] as string[],
  idHardDrive: [] as string[],
  priceRange: [0, 100000000] as [number, number],
})

const pagination = ref({ page: 1, size: 12, totalPages: 1 })

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
    // Chuyển mảng thành chuỗi cách nhau bởi dấu phẩy trước khi gửi cho BE
    const params: ADProductDetailRequest & { idBrand?: string | null, idScreen?: string | null } = {
      page: pagination.value.page,
      size: pagination.value.size,
      idRAM: filters.value.idRAM.length > 0 ? filters.value.idRAM.join(',') : null,
      idCPU: filters.value.idCPU.length > 0 ? filters.value.idCPU.join(',') : null,
      idGPU: filters.value.idGPU.length > 0 ? filters.value.idGPU.join(',') : null,
      idHardDrive: filters.value.idHardDrive.length > 0 ? filters.value.idHardDrive.join(',') : null,
      idScreen: filters.value.idScreen.length > 0 ? filters.value.idScreen.join(',') : null,
      minPrice: filters.value.priceRange[0],
      maxPrice: filters.value.priceRange[1],
      idBrand: filters.value.idBrand,
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

function toggleBrand(brandId: string) {
  if (filters.value.idBrand === brandId) {
    filters.value.idBrand = null
  }
  else {
    filters.value.idBrand = brandId
  }
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
  filters.value = {
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

  if (route.query.brand) {
    filters.value.idBrand = route.query.brand as string
  }

  await fetchProducts()
})

function handleClickProduct(id: string) {
  router.push(`/product-detail/${id}`)
}
</script>

<template>
  <div class="product-page-wrapper">
    <div class="brand-horizontal-section">
      <h2 class="section-title">
        <span class="text-red-500 mr-2">|</span>CHỌN THEO HÃNG
      </h2>
      <NSpin :show="isFetchingOptions">
        <div class="brand-list">
          <button
            v-for="b in brandOptions"
            :key="b.value"
            class="brand-btn"
            :class="{ active: filters.idBrand === b.value }"
            @click="toggleBrand(b.value as string)"
          >
            {{ b.label }}
          </button>
        </div>
      </NSpin>
    </div>

    <div class="main-content-layout">
      <!-- FIX: đổi aside -> div để tránh CSS global đang target thẻ aside -->
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
            <NSelect v-model:value="selectedPriceDropdown" :options="priceOptions" placeholder="Chọn mức giá" clearable @update:value="handlePriceSelect" />
          </div>

          <NDivider style="margin: 16px 0;" />

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
        <div class="list-header">
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
                <NCard hoverable class="product-card" content-style="padding: 16px; display: flex; flex-direction: column; height: 100%;" @click="handleClickProduct(item.id)">
                  <div v-if="item.percentage" class="discount-badge">
                    -{{ item.percentage }}%
                  </div>

                  <div class="image-wrapper">
                    <img :src="item.urlImage || 'https://via.placeholder.com/300'" :alt="item.name" class="product-image">
                  </div>

                  <div class="product-info">
                    <h3 class="product-name">
                      <NEllipsis :line-clamp="2" :tooltip="{ placement: 'top', style: 'max-width: 300px' }">
                        {{ item.name || item.productName }} {{ item.cpu }} {{ item.ram }} {{ item.gpu }}
                      </NEllipsis>
                    </h3>

                    <div class="specs-list">
                      <div v-if="item.cpu" class="spec-item">
                        <span class="spec-label">CPU:</span>
                        <NEllipsis class="spec-value" :tooltip="{ placement: 'top' }">
                          {{ item.cpu }}
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
                      <div v-if="item.gpu" class="spec-item">
                        <span class="spec-label">VGA:</span>
                        <NEllipsis class="spec-value" :tooltip="{ placement: 'top' }">
                          {{ item.gpu }}
                        </NEllipsis>
                      </div>
                    </div>

                    <div class="price-section">
                      <div class="old-price">
                        {{ item.percentage ? formatCurrency(item.price) : '&nbsp;' }}
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
              <NPagination v-model:page="pagination.page" :page-count="pagination.totalPages" size="large" @update:page="handlePageChange" />
            </div>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.product-page-wrapper {
  padding: 5px;
  background-color: #ffff;
  min-height: 100vh;
  max-width: 1400px;
  margin: 0 auto;
}

/* --- HORIZONTAL BRANDS --- */
.brand-horizontal-section {
  background: #fff;
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  margin-bottom: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin: 0 0 16px 0;
}

.brand-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.brand-btn {
  padding: 10px 24px;
  background: #ffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-weight: 600;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 100px;
  text-align: center;
}

.brand-btn:hover {
  border-color: #00a651;
  color: #00a651;
  background: #f0fdf4;
}

.brand-btn.active {
  border-color: #00a651;
  background: #00a651;
  color: #fff;
}

/* --- MAIN LAYOUT --- */
.main-content-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* --- LEFT SIDEBAR --- */
.left-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
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

/* Giới hạn độ cao vùng checkbox nếu có quá nhiều lựa chọn */
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

/* --- CHỈNH MÀU CHECKBOX SANG XANH LÁ THEO THEME --- */
:deep(.n-checkbox.n-checkbox--checked .n-checkbox-box) {
  background-color: #00a651;
  border-color: #00a651;
}

:deep(.n-checkbox .n-checkbox-box:hover) {
  border-color: #00a651;
}

/* --- PRODUCT GRID --- */
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
  border: 1px solid #ebebeb;
  background: #fff;
  position: relative;
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
  border: 1px solid #f0f0f0;
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

:deep(.n-pagination .n-pagination-item--active) {
  color: #00a651;
  border-color: #00a651;
}

:deep(.n-pagination .n-pagination-item:hover) {
  color: #00a651;
  border-color: #00a651;
}

@media (max-width: 992px) {
  .main-content-layout { flex-direction: column; }
  .left-sidebar { width: 100%; }
}
</style>
