<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NBadge,
  NButton,
  NCard,
  NCheckbox,
  NCheckboxGroup,
  NDivider,
  NEllipsis,
  NEmpty,
  NGrid,
  NGridItem,
  NIcon,
  NInput,
  NPagination,
  NScrollbar,
  NSelect,
  NSpace,
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

// --- MAP HIỂN THỊ HÃNG VỚI ICON/LOGO ---
const brandDisplayMap = computed(() => {
  const map: Record<string, { type: 'icon' | 'text', content: string, class?: string }> = {}

  brandOptions.value.forEach((brand) => {
    const label = brand.label?.toString().toLowerCase() || ''

    if (label.includes('apple')) {
      map[brand.value as string] = { type: 'icon', content: 'ion:logo-apple', class: 'text-black' }
    }
    else if (label.includes('samsung')) {
      map[brand.value as string] = { type: 'text', content: 'SAMSUNG', class: 'text-blue-600 font-black' }
    }
    else if (label.includes('xiaomi')) {
      map[brand.value as string] = { type: 'text', content: 'Xiaomi', class: 'text-orange-500 font-bold' }
    }
    else if (label.includes('asus')) {
      map[brand.value as string] = { type: 'text', content: 'ASUS', class: 'text-black font-black' }
    }
    else if (label.includes('dell')) {
      map[brand.value as string] = { type: 'text', content: 'Dell', class: 'text-blue-800 font-black' }
    }
    else if (label.includes('hp')) {
      map[brand.value as string] = { type: 'text', content: 'HP', class: 'text-blue-500 font-bold' }
    }
    else if (label.includes('lenovo')) {
      map[brand.value as string] = { type: 'text', content: 'Lenovo', class: 'text-red-600 font-bold' }
    }
    else if (label.includes('acer')) {
      map[brand.value as string] = { type: 'text', content: 'Acer', class: 'text-green-600 font-bold' }
    }
    else if (label.includes('msi')) {
      map[brand.value as string] = { type: 'text', content: 'MSI', class: 'text-red-600 font-black' }
    }
    else {
      map[brand.value as string] = { type: 'text', content: brand.label || '', class: 'text-gray-800 font-medium' }
    }
  })

  return map
})

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
  if (filters.value.priceRange[0] !== minMaxPriceLimit.value[0]
    || filters.value.priceRange[1] !== minMaxPriceLimit.value[1]) {
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

const brandGridRef = ref<HTMLElement | null>(null)

function scrollBrands(direction: number) {
  if (brandGridRef.value) {
    const scrollAmount = 300 * direction
    brandGridRef.value.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}

function handleWheel(e: WheelEvent) {
  if (brandGridRef.value) {
    e.preventDefault()
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
      <!-- Breadcrumb -->
      <div class="breadcrumb">
        <span class="breadcrumb-item" @click="router.push('/')">Trang chủ</span>
        <span class="breadcrumb-separator">/</span>
        <span class="breadcrumb-item active">Sản phẩm</span>
      </div>

      <div class="brand-section">
        <div class="brand-header">
          <div class="brand-title">
            <span class="brand-icon">
              <NovaIcon icon="ion:ribbon" :size="24" color="#16a34a" />
            </span>
            <h2>Thương hiệu nổi bật</h2>
          </div>

          <div class="brand-controls">
            <NTooltip v-if="filters.idBrand" trigger="hover">
              <template #trigger>
                <NButton text size="small" class="clear-brand-btn" @click="filters.idBrand = null">
                  <NovaIcon icon="ion:close-circle" :size="18" />
                  <span>Bỏ chọn</span>
                </NButton>
              </template>
              Xóa bộ lọc thương hiệu
            </NTooltip>

            <div class="brand-nav">
              <button class="nav-btn prev-btn" @click="scrollBrands(-1)">
                <NovaIcon icon="ion:chevron-back" :size="20" />
              </button>
              <button class="nav-btn next-btn" @click="scrollBrands(1)">
                <NovaIcon icon="ion:chevron-forward" :size="20" />
              </button>
            </div>
          </div>
        </div>

        <NSpin :show="isFetchingOptions">
          <div class="brands-container">
            <div
              ref="brandGridRef"
              class="brands-track"
              @wheel.prevent="handleWheel"
            >
              <div
                v-for="b in brandOptions"
                :key="b.value"
                class="brand-item"
                :class="{ 'brand-item-active': filters.idBrand === b.value }"
                @click="toggleBrand(b.value as string)"
              >
                <div class="brand-logo">
                  <template v-if="brandDisplayMap[b.value as string]?.type === 'icon'">
                    <NovaIcon
                      :icon="brandDisplayMap[b.value as string].content"
                      :size="32"
                      :class="brandDisplayMap[b.value as string].class || 'text-gray-700'"
                    />
                  </template>
                  <template v-else>
                    <span :class="brandDisplayMap[b.value as string]?.class || 'text-gray-800 font-semibold'">
                      {{ brandDisplayMap[b.value as string]?.content || b.label }}
                    </span>
                  </template>
                </div>

                <div v-if="filters.idBrand === b.value" class="brand-checked">
                  <NovaIcon icon="ion:checkmark-circle" :size="20" color="#16a34a" />
                </div>
              </div>
            </div>
          </div>
        </NSpin>
      </div>

      <!-- Main Content với Grid 2 cột: Sidebar và Product List -->
      <div class="main-content-grid">
        <!-- Filter Sidebar - Cột trái -->
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
              <!-- Price Filter -->
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

              <!-- CPU Filter -->
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

              <!-- GPU Filter -->
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

              <!-- RAM Filter -->
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

              <!-- Storage Filter -->
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

              <!-- Screen Filter -->
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

        <!-- Product List - Cột phải -->
        <main class="product-list-section">
          <!-- Search Info Bar -->
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

          <!-- Loading State -->
          <div v-if="loading" class="loading-container">
            <NSpin size="large">
              <div class="loading-content">
                <NovaIcon icon="ion:sync" :size="40" class="spin-icon" color="#16a34a" />
                <p>Đang tải sản phẩm...</p>
              </div>
            </NSpin>
          </div>

          <!-- Empty State -->
          <div v-else-if="productDetails.length === 0" class="empty-state">
            <NovaIcon icon="ion:cube-outline" :size="80" color="#d1d5db" />
            <h3>Không tìm thấy sản phẩm</h3>
            <p>Không có sản phẩm nào phù hợp với tiêu chí tìm kiếm của bạn.</p>
            <NButton type="success" @click="resetFilters">
              Xóa bộ lọc
            </NButton>
          </div>

          <!-- Product Grid -->
          <div v-else>
            <NGrid x-gap="20" y-gap="24" cols="1 s:2 m:3" responsive="screen">
              <NGridItem v-for="item in productDetails" :key="item.id">
                <div class="product-card" @click="handleClickProduct(item)">
                  <!-- Badges -->
                  <div class="product-badges">
                    <div v-if="item.percentage" class="badge discount">
                      -{{ item.percentage }}%
                    </div>
                    <div v-if="!isInStock(item)" class="badge out-of-stock">
                      Hết hàng
                    </div>
                  </div>

                  <!-- Image -->
                  <div class="product-image-wrapper">
                    <img
                      :src="item.urlImage || 'https://via.placeholder.com/300'"
                      :alt="item.name"
                      class="product-image"
                      loading="lazy"
                    >
                    <div class="image-overlay">
                      <NButton circle size="small" class="quick-view-btn">
                        <NovaIcon icon="ion:eye" :size="16" />
                      </NButton>
                    </div>
                  </div>

                  <!-- Info -->
                  <div class="product-info">
                    <h3 class="product-name">
                      <NEllipsis :line-clamp="2">
                        {{ item.name }}
                      </NEllipsis>
                    </h3>

                    <!-- Specs -->
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

                    <!-- Price -->
                    <div class="product-prices">
                      <div v-if="item.percentage" class="old-price">
                        {{ formatCurrency(item.price) }}
                      </div>
                      <div class="current-price" :class="{ 'no-discount': !item.percentage }">
                        {{ formatCurrency(getFinalPrice(item)) }}
                      </div>
                    </div>

                    <!-- Action -->
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

            <!-- Pagination -->
            <div v-if="pagination.totalPages > 1" class="pagination-container">
              <NPagination
                v-model:page="pagination.page"
                :page-count="pagination.totalPages"
                :page-slot="5"
                size="large"
                show-quick-jumper
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

.breadcrumb-item {
  color: #64748b;
  cursor: pointer;
  transition: color 0.2s;
}

.breadcrumb-item:hover {
  color: #16a34a;
}

.breadcrumb-item.active {
  color: #1e293b;
  font-weight: 500;
}

.breadcrumb-separator {
  color: #94a3b8;
}

/* Brand Section */
.brand-horizontal-section {
  background: white;
  padding: 20px 24px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-underline {
  width: 4px;
  height: 20px;
  background: #16a34a;
  border-radius: 2px;
}

.clear-brand-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #64748b;
  font-size: 13px;
}

.clear-brand-btn:hover {
  color: #ef4444;
}

.brand-scroll-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  padding: 0 30px;
}

.scroll-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: white;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #475569;
  transition: all 0.3s;
}

.scroll-btn:hover {
  background: #16a34a;
  color: white;
  border-color: #16a34a;
  transform: translateY(-50%) scale(1.1);
}

.left-btn {
  left: -10px;
}

.right-btn {
  right: -10px;
}

.brand-grid {
  display: grid;
  grid-template-rows: repeat(2, 1fr);
  grid-auto-flow: column;
  gap: 12px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding: 4px 0;
  width: 100%;
  grid-auto-columns: calc((100% - 48px) / 6);
  scrollbar-width: none;
}

.brand-grid::-webkit-scrollbar {
  display: none;
}

.brand-card-wrapper {
  position: relative;
  cursor: pointer;
}

.brand-card {
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  height: 70px;
}

.brand-card-wrapper:hover .brand-card {
  border-color: #16a34a;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(22, 163, 74, 0.1);
}

.brand-card-wrapper.active .brand-card {
  border-color: #16a34a;
  background: #f0fdf4;
}

.brand-check {
  position: absolute;
  top: -6px;
  right: -6px;
  background: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* Main Content Grid - QUAN TRỌNG: Fix lỗi vỡ layout */
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

.filter-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-title h3 {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  color: #1e293b;
}

.reset-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #64748b;
  font-size: 13px;
  flex-shrink: 0;
}

.reset-btn:hover {
  color: #16a34a;
}

.filter-section {
  margin-bottom: 20px;
}

.filter-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
}

.filter-section-content {
  padding-left: 26px;
}

.checkbox-scroll {
  max-height: 200px;
  overflow-y: auto;
  padding-right: 8px;
}

.checkbox-scroll::-webkit-scrollbar {
  width: 4px;
}

.checkbox-scroll::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.checkbox-item {
  font-size: 14px;
}

.price-range-display {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  padding: 8px 12px;
  border-radius: 8px;
}

.price-tag {
  font-size: 13px;
  font-weight: 500;
  color: #16a34a;
}

.price-separator {
  color: #94a3b8;
}

.filter-divider {
  margin: 16px 0;
}

/* Product List Section */
.product-list-section {
  min-width: 0; /* Quan trọng: tránh overflow trong grid */
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.search-info-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.search-query {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
  margin: 0;
}

.search-query span {
  color: #16a34a;
}

.search-info-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-count {
  font-size: 14px;
  color: #64748b;
}

.count-number {
  font-weight: 700;
  color: #16a34a;
  font-size: 16px;
}

.view-options {
  display: flex;
  gap: 4px;
}

.view-btn {
  padding: 4px;
  border-radius: 8px;
  color: #94a3b8;
}

.view-btn.active {
  color: #16a34a;
  background: #f0fdf4;
}

/* Loading State */
.loading-container {
  padding: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.loading-content {
  text-align: center;
}

.spin-icon {
  animation: spin 2s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.loading-content p {
  color: #64748b;
  margin-top: 12px;
}

/* Empty State */
.empty-state {
  padding: 60px 20px;
  text-align: center;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 16px 0 8px;
}

.empty-state p {
  color: #64748b;
  margin-bottom: 20px;
}

/* Product Card */
.product-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  cursor: pointer;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  border: 1px solid #e2e8f0;
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
  border-color: #16a34a;
}

.product-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  display: flex;
  gap: 6px;
}

.badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}

.badge.discount {
  background: #ef4444;
  color: white;
}

.badge.out-of-stock {
  background: #64748b;
  color: white;
}

.product-image-wrapper {
  position: relative;
  padding-top: 75%;
  overflow: hidden;
  background: #f8fafc;
}

.product-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.5s;
  padding: 16px;
}

.product-card:hover .product-image {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.product-card:hover .image-overlay {
  opacity: 1;
}

.quick-view-btn {
  background: white;
  color: #1e293b;
  transform: translateY(20px);
  transition: transform 0.3s;
}

.product-card:hover .quick-view-btn {
  transform: translateY(0);
}

.product-info {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  margin: 0 0 12px 0;
  color: #1e293b;
  min-height: 42px;
}

.product-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.spec-chip {
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 4px;
}

.product-prices {
  margin-bottom: 12px;
}

.old-price {
  font-size: 13px;
  color: #94a3b8;
  text-decoration: line-through;
}

.current-price {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
}

.current-price.no-discount {
  color: #1e293b;
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.stars {
  display: flex;
  gap: 2px;
}

.rating-count {
  font-size: 12px;
  color: #64748b;
}

.product-action {
  margin-top: auto;
}

/* Pagination */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 20px 0;
}

:deep(.n-pagination .n-pagination-item--active) {
  color: #16a34a !important;
  border-color: #16a34a !important;
  background: #f0fdf4 !important;
}

:deep(.n-pagination .n-pagination-item:hover) {
  color: #ef4444 !important;
  border-color: #ef4444 !important;
  background: #fff1f2 !important;
}

/* Brand Section - Thiết kế mới */
.brand-section {
  background: white;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  border: 1px solid #f0f0f0;
}

.brand-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.brand-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  width: 40px;
  height: 40px;
  background: #f0fdf4;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-title h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.brand-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.clear-brand-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 30px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  transition: all 0.2s;
}

.clear-brand-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

.brand-nav {
  display: flex;
  gap: 8px;
}

.nav-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: white;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #475569;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.nav-btn:hover {
  background: #16a34a;
  border-color: #16a34a;
  color: white;
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.2);
}

.nav-btn:active {
  transform: scale(0.95);
}

.brands-container {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.brands-track {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding: 8px 4px 16px 4px;
  scrollbar-width: thin;
  scrollbar-color: #16a34a #e2e8f0;
}

.brands-track::-webkit-scrollbar {
  height: 6px;
}

.brands-track::-webkit-scrollbar-track {
  background: #e2e8f0;
  border-radius: 10px;
}

.brands-track::-webkit-scrollbar-thumb {
  background: #16a34a;
  border-radius: 10px;
}

.brands-track::-webkit-scrollbar-thumb:hover {
  background: #15803d;
}

.brand-item {
  flex: 0 0 140px;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 16px;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.brand-item:hover {
  border-color: #16a34a;
  transform: translateY(-4px);
  box-shadow: 0 12px 24px -8px rgba(22, 163, 74, 0.2);
}

.brand-item-active {
  border-color: #16a34a;
  background: #f0fdf4;
  box-shadow: 0 8px 20px -4px rgba(22, 163, 74, 0.15);
}

.brand-logo {
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-logo span {
  font-size: 18px;
  font-weight: 600;
}

.brand-name {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-item-active .brand-name {
  color: #16a34a;
  font-weight: 600;
}

.brand-checked {
  position: absolute;
  top: -8px;
  right: -8px;
  background: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes popIn {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  80% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* Responsive */
@media (max-width: 992px) {
  .brand-section {
    padding: 20px;
  }

  .brand-item {
    flex: 0 0 120px;
    padding: 12px 8px;
  }

  .brand-logo {
    height: 50px;
  }

  .brand-logo span {
    font-size: 16px;
  }
}

@media (max-width: 768px) {
  .brand-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .brand-controls {
    width: 100%;
    justify-content: space-between;
  }

  .brand-item {
    flex: 0 0 100px;
  }

  .brand-name {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .brand-section {
    padding: 16px;
  }

  .brand-title h2 {
    font-size: 18px;
  }

  .brand-item {
    flex: 0 0 90px;
    padding: 10px 6px;
  }

  .brand-logo {
    height: 40px;
  }

  .brand-logo span {
    font-size: 14px;
  }

  .brand-logo :deep(svg) {
    width: 24px;
    height: 14px;
  }
}

/* Responsive */
@media (max-width: 992px) {
  .main-content-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .filter-sidebar {
    position: static;
    max-height: none;
  }

  .brand-grid {
    grid-auto-columns: calc((100% - 24px) / 3);
  }
}

@media (max-width: 768px) {
  .product-page-wrapper {
    padding: 12px;
  }

  .brand-grid {
    grid-auto-columns: calc((100% - 12px) / 2);
  }

  .search-info-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .search-info-right {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 480px) {
  .brand-grid {
    grid-auto-columns: calc(100% - 24px);
  }
}
</style>
