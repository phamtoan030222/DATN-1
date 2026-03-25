<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NBadge,
  NButton,
  NCheckbox,
  NCheckboxGroup,
  NConfigProvider,
  NDivider,
  NEllipsis,
  NGrid,
  NGridItem,
  NPagination,
  NScrollbar,
  NSelect,
  NSpin,
  NTag,
  NTooltip,
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

// Theme màu sắc chuẩn thương mại điện tử
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
  Button: {
    textColorHover: '#16a34a',
    borderHover: '1px solid #16a34a',
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

// --- CẤU HÌNH THƯƠNG HIỆU ---
const brandStyleMap: Record<string, { icon: string, color: string }> = {
  apple: { icon: 'simple-icons:apple', color: '#000000' },
  samsung: { icon: 'simple-icons:samsung', color: '#1428A0' },
  xiaomi: { icon: 'simple-icons:xiaomi', color: '#FF6900' },
  asus: { icon: 'simple-icons:asus', color: '#00539B' },
  dell: { icon: 'simple-icons:dell', color: '#007DB8' },
  hp: { icon: 'simple-icons:hp', color: '#0096D6' },
  lenovo: { icon: 'simple-icons:lenovo', color: '#E2231A' },
  acer: { icon: 'simple-icons:acer', color: '#83B81A' },
  msi: { icon: 'simple-icons:msi', color: '#FF0000' },
  vivo: { icon: 'simple-icons:vivo', color: '#411445' },
  lg: { icon: 'simple-icons:lg', color: '#A50034' },
}

const brandDisplayMap = computed(() => {
  const map: Record<string, { type: 'icon' | 'text', content: string, color?: string }> = {}

  brandOptions.value.forEach((brand) => {
    const label = brand.label?.toString().toLowerCase() || ''
    let matched = false

    for (const [key, val] of Object.entries(brandStyleMap)) {
      if (label.includes(key)) {
        map[brand.value as string] = { type: 'icon', content: val.icon, color: val.color }
        matched = true
        break
      }
    }

    if (!matched) {
      map[brand.value as string] = { type: 'text', content: brand.label || '' }
    }
  })

  return map
})

// --- BỘ LỌC GIÁ ---
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
const activeFilterCount = computed(() => {
  let count = 0
  if (filters.value.idBrand)
    count++
  if (filters.value.idScreen.length)
    count++
  if (filters.value.idRAM.length)
    count++
  if (filters.value.idCPU.length)
    count++
  if (filters.value.idGPU.length)
    count++
  if (filters.value.idHardDrive.length)
    count++
  if (filters.value.priceRange[0] !== minMaxPriceLimit.value[0] || filters.value.priceRange[1] !== minMaxPriceLimit.value[1]) {
    count++
  }
  return count
})

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

// Tính giá sau giảm giá
function getFinalPrice(item: ADProductDetailResponse) {
  if (item.percentage) {
    return item.price * (1 - item.percentage / 100)
  }
  return item.price
}

// Kiểm tra còn hàng hay không
function isInStock(item: ADProductDetailResponse) {
  return (item.quantity || 0) > 0
}
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <div class="product-page-wrapper">
      <div class="breadcrumb">
        <span class="breadcrumb-item" @click="router.push('/')">Trang chủ</span>
        <span class="breadcrumb-separator">/</span>
        <span class="breadcrumb-item active">Sản phẩm</span>
      </div>

      <div class="modern-brand-section">
        <div class="brand-header">
          <div class="brand-title">
            <h2 class="title-text">
              Chọn theo thương hiệu
            </h2>
          </div>
          <NButton
            v-if="filters.idBrand"
            text
            type="error"
            size="small"
            class="clear-brand-btn"
            @click="filters.idBrand = null"
          >
            <template #icon>
              <NovaIcon icon="ion:close-circle-outline" :size="18" />
            </template>
            Bỏ chọn
          </NButton>
        </div>

        <NSpin :show="isFetchingOptions">
          <div class="brand-grid-container">
            <div
              v-for="b in brandOptions"
              :key="b.value"
              class="brand-box"
              :class="{ 'is-active': filters.idBrand === b.value }"
              @click="toggleBrand(b.value as string)"
            >
              <div v-if="filters.idBrand === b.value" class="active-tick">
                <NovaIcon icon="ion:checkmark" :size="14" color="white" />
              </div>

              <template v-if="brandDisplayMap[b.value as string]?.type === 'icon'">
                <NovaIcon
                  :icon="brandDisplayMap[b.value as string].content"
                  :size="40"
                  class="brand-logo-icon"
                  :color="filters.idBrand === b.value ? '#16a34a' : brandDisplayMap[b.value as string].color"
                />
              </template>
              <template v-else>
                <span class="brand-text">{{ brandDisplayMap[b.value as string]?.content }}</span>
              </template>
            </div>
          </div>
        </NSpin>
      </div>

      <div class="main-content-grid">
        <aside class="filter-sidebar">
          <div class="filter-header">
            <div class="filter-title">
              <NovaIcon icon="ion:filter" :size="20" color="#16a34a" />
              <h3>Bộ lọc sản phẩm</h3>
              <NBadge v-if="activeFilterCount > 0" :value="activeFilterCount" type="success" />
            </div>
            <NButton
              v-if="activeFilterCount > 0"
              text
              size="small"
              class="reset-btn"
              @click="resetFilters"
            >
              <NovaIcon icon="ion:refresh" :size="16" />
              <span>Xóa tất cả</span>
            </NButton>
          </div>

          <NScrollbar style="max-height: calc(100vh - 200px);">
            <NSpin :show="isFetchingOptions">
              <div class="filter-section">
                <div class="filter-section-header">
                  <NovaIcon icon="ion:pricetag" :size="18" color="#16a34a" />
                  <span>Mức giá</span>
                </div>
                <div class="filter-section-content">
                  <NSelect
                    v-model:value="selectedPriceDropdown"
                    :options="priceOptions"
                    placeholder="Chọn khoảng giá"
                    clearable
                    size="medium"
                    @update:value="handlePriceSelect"
                  />
                  <div v-if="filters.priceRange[0] !== minMaxPriceLimit[0] || filters.priceRange[1] !== minMaxPriceLimit[1]" class="price-range-display">
                    <span class="price-tag">{{ formatCurrency(filters.priceRange[0]) }}</span>
                    <span class="price-separator">-</span>
                    <span class="price-tag">{{ formatCurrency(filters.priceRange[1]) }}</span>
                  </div>
                </div>
              </div>

              <NDivider class="filter-divider" />

              <div class="filter-section">
                <div class="filter-section-header">
                  <NovaIcon icon="ion:hardware-chip" :size="18" color="#16a34a" />
                  <span>Dòng CPU</span>
                  <NTag v-if="filters.idCPU.length" size="small" type="success" round>
                    {{ filters.idCPU.length }}
                  </NTag>
                </div>
                <div class="filter-section-content checkbox-scroll">
                  <NCheckboxGroup v-model:value="filters.idCPU">
                    <div class="checkbox-group">
                      <div v-for="opt in cpuOptions" :key="opt.value" class="checkbox-item">
                        <NCheckbox :value="opt.value">
                          {{ opt.label }}
                        </NCheckbox>
                      </div>
                    </div>
                  </NCheckboxGroup>
                </div>
              </div>

              <div class="filter-section">
                <div class="filter-section-header">
                  <NovaIcon icon="ion:videocam" :size="18" color="#16a34a" />
                  <span>Card đồ họa</span>
                  <NTag v-if="filters.idGPU.length" size="small" type="success" round>
                    {{ filters.idGPU.length }}
                  </NTag>
                </div>
                <div class="filter-section-content checkbox-scroll">
                  <NCheckboxGroup v-model:value="filters.idGPU">
                    <div class="checkbox-group">
                      <div v-for="opt in gpuOptions" :key="opt.value" class="checkbox-item">
                        <NCheckbox :value="opt.value">
                          {{ opt.label }}
                        </NCheckbox>
                      </div>
                    </div>
                  </NCheckboxGroup>
                </div>
              </div>

              <div class="filter-section">
                <div class="filter-section-header">
                  <NovaIcon icon="ion:memory" :size="18" color="#16a34a" />
                  <span>Dung lượng RAM</span>
                  <NTag v-if="filters.idRAM.length" size="small" type="success" round>
                    {{ filters.idRAM.length }}
                  </NTag>
                </div>
                <div class="filter-section-content checkbox-scroll">
                  <NCheckboxGroup v-model:value="filters.idRAM">
                    <div class="checkbox-group">
                      <div v-for="opt in ramOptions" :key="opt.value" class="checkbox-item">
                        <NCheckbox :value="opt.value">
                          {{ opt.label }}
                        </NCheckbox>
                      </div>
                    </div>
                  </NCheckboxGroup>
                </div>
              </div>

              <div class="filter-section">
                <div class="filter-section-header">
                  <NovaIcon icon="ion:save" :size="18" color="#16a34a" />
                  <span>Ổ cứng</span>
                  <NTag v-if="filters.idHardDrive.length" size="small" type="success" round>
                    {{ filters.idHardDrive.length }}
                  </NTag>
                </div>
                <div class="filter-section-content checkbox-scroll">
                  <NCheckboxGroup v-model:value="filters.idHardDrive">
                    <div class="checkbox-group">
                      <div v-for="opt in hardDriveOptions" :key="opt.value" class="checkbox-item">
                        <NCheckbox :value="opt.value">
                          {{ opt.label }}
                        </NCheckbox>
                      </div>
                    </div>
                  </NCheckboxGroup>
                </div>
              </div>

              <div class="filter-section">
                <div class="filter-section-header">
                  <NovaIcon icon="ion:phone-portrait" :size="18" color="#16a34a" />
                  <span>Màn hình</span>
                  <NTag v-if="filters.idScreen.length" size="small" type="success" round>
                    {{ filters.idScreen.length }}
                  </NTag>
                </div>
                <div class="filter-section-content checkbox-scroll">
                  <NCheckboxGroup v-model:value="filters.idScreen">
                    <div class="checkbox-group">
                      <div v-for="opt in screenOptions" :key="opt.value" class="checkbox-item">
                        <NCheckbox :value="opt.value">
                          {{ opt.label }}
                        </NCheckbox>
                      </div>
                    </div>
                  </NCheckboxGroup>
                </div>
              </div>
            </NSpin>
          </NScrollbar>
        </aside>

        <main class="product-list-section">
          <div class="search-info-bar">
            <div class="search-info-left">
              <h3 v-if="filters.q" class="search-query">
                <NovaIcon icon="ion:search" :size="18" color="#16a34a" />
                Kết quả cho: <span>"{{ filters.q }}"</span>
              </h3>
              <div v-else class="search-query-placeholder" />
            </div>
            <div class="search-info-right">
              <span class="result-count">
                <span class="count-number">{{ productDetails.length }}</span> sản phẩm
              </span>
              <div class="view-options">
                <NTooltip trigger="hover">
                  <template #trigger>
                    <NButton text class="view-btn active">
                      <NovaIcon icon="ion:grid" :size="20" />
                    </NButton>
                  </template>
                  Hiển thị dạng lưới
                </NTooltip>
              </div>
            </div>
          </div>

          <div v-if="loading" class="loading-container">
            <NSpin size="large">
              <div class="loading-content">
                <NovaIcon icon="ion:sync" :size="40" class="spin-icon" color="#16a34a" />
                <p>Đang tải sản phẩm...</p>
              </div>
            </NSpin>
          </div>

          <div v-else-if="productDetails.length === 0" class="empty-state">
            <NovaIcon icon="ion:cube-outline" :size="80" color="#d1d5db" />
            <h3>Không tìm thấy sản phẩm</h3>
            <p>Không có sản phẩm nào phù hợp với tiêu chí tìm kiếm của bạn.</p>
            <NButton type="success" @click="resetFilters">
              Xóa bộ lọc
            </NButton>
          </div>

          <div v-else>
            <NGrid x-gap="20" y-gap="24" cols="1 s:2 m:3" responsive="screen">
              <NGridItem v-for="item in productDetails" :key="item.id">
                <div class="product-card" @click="handleClickProduct(item)">
                  <div class="product-badges">
                    <div v-if="item.percentage" class="badge discount">
                      -{{ item.percentage }}%
                    </div>
                    <div v-if="!isInStock(item)" class="badge out-of-stock">
                      Hết hàng
                    </div>
                  </div>

                  <div class="product-image-wrapper">
                    <img
                      :src="item.urlImage || 'https://via.placeholder.com/300'"
                      :alt="item.name"
                      class="product-image"
                      loading="lazy"
                    >
                  </div>

                  <div class="product-info">
                    <h3 class="product-name">
                      <NEllipsis :line-clamp="2">
                        {{ item.name }}
                      </NEllipsis>
                    </h3>

                    <div class="product-specs">
                      <div v-if="item.cpu" class="spec-chip">
                        <NovaIcon icon="ion:hardware-chip" :size="12" />
                        <span>{{ item.cpu }}</span>
                      </div>
                      <div v-if="item.ram" class="spec-chip">
                        <NovaIcon icon="ion:memory" :size="12" />
                        <span>{{ item.ram }}</span>
                      </div>
                      <div v-if="item.hardDrive" class="spec-chip">
                        <NovaIcon icon="ion:save" :size="12" />
                        <span>{{ item.hardDrive }}</span>
                      </div>
                    </div>

                    <div class="product-prices">
                      <div v-if="item.percentage" class="old-price">
                        {{ formatCurrency(item.price) }}
                      </div>
                      <div class="current-price" :class="{ 'no-discount': !item.percentage }">
                        {{ formatCurrency(getFinalPrice(item)) }}
                      </div>
                    </div>

                    <div class="product-action">
                      <NButton
                        block
                        :type="isInStock(item) ? 'success' : 'default'"
                        :disabled="!isInStock(item)"
                        size="small"
                      >
                        <template #icon>
                          <NovaIcon :icon="isInStock(item) ? 'ion:cart' : 'ion:alert-circle'" :size="16" />
                        </template>
                        {{ isInStock(item) ? 'Chọn mua' : 'Hết hàng' }}
                      </NButton>
                    </div>
                  </div>
                </div>
              </NGridItem>
            </NGrid>

            <div v-if="pagination.totalPages > 1" class="pagination-container">
              <NPagination
                v-model:page="pagination.page"
                :page-count="pagination.totalPages"
                :page-slot="5"
                size="large"
                @update:page="handlePageChange"
              />
            </div>
          </div>
        </main>
      </div>
    </div>
  </NConfigProvider>
</template>

<style scoped>
.product-page-wrapper {
  background-color: #f8fafc;
  min-height: 100vh;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 24px;
}

/* Breadcrumb */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}
.breadcrumb-item { color: #64748b; cursor: pointer; transition: color 0.2s; }
.breadcrumb-item:hover { color: #16a34a; }
.breadcrumb-item.active { color: #1e293b; font-weight: 500; }
.breadcrumb-separator { color: #94a3b8; }

/* --- BRAND SECTION MỚI --- */
.modern-brand-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  border: 1px solid #f1f5f9;
}

.brand-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title-text {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.clear-brand-btn {
  font-weight: 600;
  transition: all 0.2s;
}

.brand-grid-container {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.brand-box {
  width: 130px;
  height: 56px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
}

.brand-box:hover {
  border-color: #16a34a;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.1);
}

.brand-box.is-active {
  border-color: #16a34a;
  background: #f0fdf4;
  box-shadow: inset 0 0 0 1px #16a34a;
}

.brand-logo-icon {
  transition: color 0.2s ease;
}

.brand-text {
  font-size: 14px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  transition: color 0.2s ease;
}

.brand-box.is-active .brand-text {
  color: #16a34a;
}

.active-tick {
  position: absolute;
  top: -1px;
  right: -1px;
  width: 22px;
  height: 22px;
  background: #16a34a;
  border-radius: 0 8px 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

/* Main Content Grid */
.main-content-grid {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  align-items: start;
}

/* Filter Sidebar */
.filter-sidebar {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 20px;
  position: sticky;
  top: 20px;
  height: fit-content;
  max-height: calc(100vh - 40px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.filter-title { display: flex; align-items: center; gap: 8px; }
.filter-title h3 { font-size: 16px; font-weight: 700; margin: 0; color: #1e293b; }

.reset-btn { display: flex; align-items: center; gap: 4px; color: #64748b; font-size: 13px; flex-shrink: 0; }
.reset-btn:hover { color: #16a34a; }

.filter-section { margin-bottom: 20px; }
.filter-section-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; font-weight: 600; font-size: 14px; color: #1e293b; }
.filter-section-content { padding-left: 26px; }

.checkbox-scroll { max-height: 200px; overflow-y: auto; padding-right: 8px; }
.checkbox-scroll::-webkit-scrollbar { width: 4px; }
.checkbox-scroll::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.checkbox-group { display: flex; flex-direction: column; gap: 8px; }
.checkbox-item { font-size: 14px; }

.price-range-display { margin-top: 8px; display: flex; align-items: center; gap: 8px; background: #f8fafc; padding: 8px 12px; border-radius: 8px; }
.price-tag { font-size: 13px; font-weight: 500; color: #16a34a; }
.price-separator { color: #94a3b8; }
.filter-divider { margin: 16px 0; }

/* Product List Section */
.product-list-section {
  min-width: 0;
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.search-info-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #e2e8f0; }
.search-query { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 500; color: #1e293b; margin: 0; }
.search-query span { color: #16a34a; }
.search-info-right { display: flex; align-items: center; gap: 12px; }
.result-count { font-size: 14px; color: #64748b; }
.count-number { font-weight: 700; color: #16a34a; font-size: 16px; }
.view-options { display: flex; gap: 4px; }
.view-btn { padding: 4px; border-radius: 8px; color: #94a3b8; }
.view-btn.active { color: #16a34a; background: #f0fdf4; }

/* Loading State */
.loading-container { padding: 60px; display: flex; justify-content: center; align-items: center; min-height: 400px; }
.loading-content { text-align: center; }
.spin-icon { animation: spin 2s linear infinite; margin-bottom: 16px; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.loading-content p { color: #64748b; margin-top: 12px; }

/* Empty State */
.empty-state { padding: 60px 20px; text-align: center; min-height: 400px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.empty-state h3 { font-size: 18px; font-weight: 600; color: #1e293b; margin: 16px 0 8px; }
.empty-state p { color: #64748b; margin-bottom: 20px; }

/* Product Card */
.product-card { background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); transition: all 0.3s; cursor: pointer; height: 100%; display: flex; flex-direction: column; position: relative; border: 1px solid #e2e8f0; }
.product-card:hover { transform: translateY(-6px); box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12); border-color: #16a34a; }

.product-badges { position: absolute; top: 12px; left: 12px; z-index: 10; display: flex; gap: 6px; }
.badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 700; text-transform: uppercase; }
.badge.discount { background: #ef4444; color: white; }
.badge.out-of-stock { background: #64748b; color: white; }

.product-image-wrapper { position: relative; padding-top: 75%; overflow: hidden; background: #f8fafc; }
.product-image { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: contain; transition: transform 0.5s; padding: 16px; }
.product-card:hover .product-image { transform: scale(1.05); }

.image-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.2); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.3s; }
.product-card:hover .image-overlay { opacity: 1; }
.quick-view-btn { background: white; color: #1e293b; transform: translateY(20px); transition: transform 0.3s; }
.product-card:hover .quick-view-btn { transform: translateY(0); }

.product-info { padding: 16px; flex: 1; display: flex; flex-direction: column; }
.product-name { font-size: 15px; font-weight: 600; line-height: 1.4; margin: 0 0 12px 0; color: #1e293b; min-height: 42px; }
.product-specs { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.spec-chip { background: #f1f5f9; padding: 4px 8px; border-radius: 4px; font-size: 11px; color: #475569; display: flex; align-items: center; gap: 4px; }
.product-prices { margin-bottom: 12px; }
.old-price { font-size: 13px; color: #94a3b8; text-decoration: line-through; }
.current-price { font-size: 18px; font-weight: 700; color: #ef4444; }
.current-price.no-discount { color: #1e293b; }
.product-action { margin-top: auto; }

/* Pagination */
.pagination-container { display: flex; justify-content: center; margin-top: 40px; padding: 20px 0; }
:deep(.n-pagination .n-pagination-item--active) { color: #16a34a !important; border-color: #16a34a !important; background: #f0fdf4 !important; }
:deep(.n-pagination .n-pagination-item:hover) { color: #ef4444 !important; border-color: #ef4444 !important; background: #fff1f2 !important; }

/* Responsive */
@media (max-width: 992px) {
  .main-content-grid { grid-template-columns: 1fr; gap: 16px; }
  .filter-sidebar { position: static; max-height: none; }
}

@media (max-width: 768px) {
  .product-page-wrapper { padding: 12px; }
  .search-info-bar { flex-direction: column; align-items: flex-start; gap: 8px; }
  .search-info-right { width: 100%; justify-content: space-between; }
  .brand-grid-container {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }
  .brand-box { width: 100%; }
}
</style>
