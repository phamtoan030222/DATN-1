<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NModal, NSelect, NTag, useMessage } from 'naive-ui'
import { CartOutline, SearchOutline } from '@vicons/ionicons5'
import { getProductDetails } from '@/service/api/client/product/productDetail.api'
import { useCartStore } from '@/store/app/cart'

const props = defineProps<{
  show: boolean
  currentProduct: any
  options: any[]
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
}>()

const message = useMessage()
const router = useRouter()
const cartStore = useCartStore()

// State cho Máy so sánh 1
const compareTargetId1 = ref<string | null>(null)
const selectedCompareProduct1 = ref<any>(null)

// State cho Máy so sánh 2
const compareTargetId2 = ref<string | null>(null)
const selectedCompareProduct2 = ref<any>(null)

const isSearching = ref(false)
const searchOptions = ref<any[]>([])
let searchTimeout: any = null

const loadingCart = ref(false)

watch(() => props.show, (newVal) => {
  if (newVal) {
    compareTargetId1.value = null
    selectedCompareProduct1.value = null
    compareTargetId2.value = null
    selectedCompareProduct2.value = null
    searchOptions.value = [...props.options]
  }
})

async function handleSearch(query: string) {
  if (!query) {
    searchOptions.value = [...props.options]
    return
  }

  isSearching.value = true

  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(async () => {
    try {
      const res = await getProductDetails({
        page: 1,
        size: 20,
        q: query,
        minPrice: 0,
        maxPrice: 1000000000,
      })

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

      list = list.filter(item => item.id !== props.currentProduct?.id)

      searchOptions.value = list.map(item => ({
        label: item.name,
        value: item.id,
        data: item,
      }))
    }
    catch (error) {
      console.error('Lỗi tìm kiếm:', error)
      message.error('Không thể tìm kiếm sản phẩm lúc này')
    }
    finally {
      isSearching.value = false
    }
  }, 500)
}

function handleSelectCompareItem1(val: string | null, option: any) {
  selectedCompareProduct1.value = (!val || !option) ? null : option.data
}

function handleSelectCompareItem2(val: string | null, option: any) {
  selectedCompareProduct2.value = (!val || !option) ? null : option.data
}

function getCalculatedPrice(item: any) {
  if (!item)
    return 0
  const original = Number(item.price) || 0
  const pct = Number(item.percentage) || 0
  return pct > 0 ? (original * (100 - pct)) / 100 : original
}

function formatCurrency(val: number | undefined) {
  return val === undefined ? '0₫' : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

function formatDate(timestamp: number | undefined) {
  if (!timestamp)
    return ''
  const date = new Date(timestamp)
  return date.toLocaleDateString('vi-VN')
}

// --- HÀM XỬ LÝ MUA NGAY ---
async function handleBuyNow(item: any) {
  if (!item)
    return

  loadingCart.value = true
  try {
    // Thêm 1 sản phẩm vào giỏ hàng
    await cartStore.addToCart(item.id, 1)

    // Đóng Modal so sánh
    emit('update:show', false)

    // Chuyển hướng sang trang giỏ hàng
    router.push({ name: 'Cart' })
  }
  catch (error: any) {
    message.error(error.message || 'Không thể thêm vào giỏ hàng, có thể sản phẩm đã hết!')
  }
  finally {
    loadingCart.value = false
  }
}
</script>

<template>
  <NModal :show="show" preset="card" title="So Sánh Sản Phẩm" style="width: 1100px; max-width: 95vw"
    @update:show="(val) => emit('update:show', val)">
    <div v-if="currentProduct" class="compare-container">
      <table class="compare-table">
        <thead>
          <tr>
            <th class="prop-col">
              Thuộc tính
            </th>

            <th class="product-col">
              <div class="compare-header-info">
                <NTag type="success" size="small" style="margin-bottom: 8px; width: fit-content;">
                  Máy hiện tại
                </NTag>
                <p class="compare-name">
                  {{ currentProduct.name }}
                </p>
              </div>
            </th>

            <th class="product-col">
              <div class="compare-header-info">
                <NSelect v-model:value="compareTargetId1" :options="searchOptions" placeholder="🔍 Gõ tên sản phẩm 1..."
                  filterable clearable remote :loading="isSearching" @search="handleSearch"
                  @update:value="handleSelectCompareItem1">
                  <template #arrow>
                    <NIcon>
                      <SearchOutline />
                    </NIcon>
                  </template>
                </NSelect>
                <p v-if="selectedCompareProduct1" class="compare-name" style="margin-top: 12px;">
                  {{ selectedCompareProduct1.name }}
                </p>
              </div>
            </th>

            <th class="product-col">
              <div class="compare-header-info">
                <NSelect v-model:value="compareTargetId2" :options="searchOptions" placeholder="🔍 Gõ tên sản phẩm 2..."
                  filterable clearable remote :loading="isSearching" @search="handleSearch"
                  @update:value="handleSelectCompareItem2">
                  <template #arrow>
                    <NIcon>
                      <SearchOutline />
                    </NIcon>
                  </template>
                </NSelect>
                <p v-if="selectedCompareProduct2" class="compare-name" style="margin-top: 12px;">
                  {{ selectedCompareProduct2.name }}
                </p>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Hình ảnh</td>
            <td class="center">
              <img :src="currentProduct.urlImage" class="compare-img">
            </td>
            <td class="center">
              <img v-if="selectedCompareProduct1" :src="selectedCompareProduct1.urlImage" class="compare-img">
              <div v-else class="empty-placeholder">
                <span class="text-gray">Trống</span>
              </div>
            </td>
            <td class="center">
              <img v-if="selectedCompareProduct2" :src="selectedCompareProduct2.urlImage" class="compare-img">
              <div v-else class="empty-placeholder">
                <span class="text-gray">Trống</span>
              </div>
            </td>
          </tr>

          <tr>
            <td>Giá bán</td>
            <td class="center">
              <div class="price-wrapper">
                <span v-if="currentProduct.percentage > 0" class="old-price">{{ formatCurrency(currentProduct.price)
                  }}</span>
                <span class="compare-price">{{ formatCurrency(getCalculatedPrice(currentProduct)) }}</span>
              </div>
            </td>
            <td class="center">
              <div v-if="selectedCompareProduct1" class="price-wrapper">
                <span v-if="selectedCompareProduct1.percentage > 0" class="old-price">{{
                  formatCurrency(selectedCompareProduct1.price) }}</span>
                <span class="compare-price">{{ formatCurrency(getCalculatedPrice(selectedCompareProduct1)) }}</span>
              </div>
              <span v-else class="text-gray">-</span>
            </td>
            <td class="center">
              <div v-if="selectedCompareProduct2" class="price-wrapper">
                <span v-if="selectedCompareProduct2.percentage > 0" class="old-price">{{
                  formatCurrency(selectedCompareProduct2.price) }}</span>
                <span class="compare-price">{{ formatCurrency(getCalculatedPrice(selectedCompareProduct2)) }}</span>
              </div>
              <span v-else class="text-gray">-</span>
            </td>
          </tr>

          <tr>
            <td>Khuyến mãi</td>
            <td class="center">
              <div v-if="currentProduct.percentage > 0" class="promo-box">
                <NTag type="error" size="small" round>
                  🔥 Giảm {{ currentProduct.percentage }}%
                </NTag>
                <span v-if="currentProduct.endDate" class="promo-date">Đến: {{ formatDate(currentProduct.endDate)
                  }}</span>
              </div>
              <span v-else class="text-gray">-</span>
            </td>
            <td class="center">
              <div v-if="selectedCompareProduct1 && selectedCompareProduct1.percentage > 0" class="promo-box">
                <NTag type="error" size="small" round>
                  🔥 Giảm {{ selectedCompareProduct1.percentage }}%
                </NTag>
                <span v-if="selectedCompareProduct1.endDate" class="promo-date">Đến: {{
                  formatDate(selectedCompareProduct1.endDate) }}</span>
              </div>
              <span v-else class="text-gray">-</span>
            </td>
            <td class="center">
              <div v-if="selectedCompareProduct2 && selectedCompareProduct2.percentage > 0" class="promo-box">
                <NTag type="error" size="small" round>
                  🔥 Giảm {{ selectedCompareProduct2.percentage }}%
                </NTag>
                <span v-if="selectedCompareProduct2.endDate" class="promo-date">Đến: {{
                  formatDate(selectedCompareProduct2.endDate) }}</span>
              </div>
              <span v-else class="text-gray">-</span>
            </td>
          </tr>

          <tr>
            <td>CPU</td>
            <td>{{ currentProduct.cpuName || currentProduct.cpu || 'N/A' }}</td>
            <td>
              {{ selectedCompareProduct1 ? (selectedCompareProduct1.cpuName || selectedCompareProduct1.cpu || 'N/A')
                : '-' }}
            </td>
            <td>
              {{ selectedCompareProduct2 ? (selectedCompareProduct2.cpuName || selectedCompareProduct2.cpu || 'N/A')
                : '-' }}
            </td>
          </tr>
          <tr>
            <td>RAM</td>
            <td>{{ currentProduct.ramName || currentProduct.ram || 'N/A' }}</td>
            <td>
              {{ selectedCompareProduct1 ? (selectedCompareProduct1.ramName || selectedCompareProduct1.ram || 'N/A')
                : '-' }}
            </td>
            <td>
              {{ selectedCompareProduct2 ? (selectedCompareProduct2.ramName || selectedCompareProduct2.ram || 'N/A')
                : '-' }}
            </td>
          </tr>
          <tr>
            <td>Ổ cứng</td>
            <td>{{ currentProduct.hardDriveName || currentProduct.hardDrive || 'N/A' }}</td>
            <td>
              {{ selectedCompareProduct1 ? (selectedCompareProduct1.hardDriveName || selectedCompareProduct1.hardDrive
                || 'N/A') : '-' }}
            </td>
            <td>
              {{ selectedCompareProduct2 ? (selectedCompareProduct2.hardDriveName || selectedCompareProduct2.hardDrive
                || 'N/A') : '-' }}
            </td>
          </tr>
          <tr>
            <td>Card đồ hoạ (VGA)</td>
            <td>{{ currentProduct.gpuName || currentProduct.gpu || 'N/A' }}</td>
            <td>
              {{ selectedCompareProduct1 ? (selectedCompareProduct1.gpuName || selectedCompareProduct1.gpu || 'N/A')
                : '-' }}
            </td>
            <td>
              {{ selectedCompareProduct2 ? (selectedCompareProduct2.gpuName || selectedCompareProduct2.gpu || 'N/A')
                : '-' }}
            </td>
          </tr>
          <tr>
            <td>Màn hình</td>
            <td>{{ currentProduct.screenName || currentProduct.screen || 'N/A' }}</td>
            <td>
              {{ selectedCompareProduct1 ? (selectedCompareProduct1.screenName || selectedCompareProduct1.screen
                || 'N/A')
                : '-' }}
            </td>
            <td>
              {{ selectedCompareProduct2 ? (selectedCompareProduct2.screenName || selectedCompareProduct2.screen
                || 'N/A')
                : '-' }}
            </td>
          </tr>
          <tr>
            <td>Màu sắc</td>
            <td>{{ currentProduct.colorName || currentProduct.color || 'N/A' }}</td>
            <td>
              {{ selectedCompareProduct1 ? (selectedCompareProduct1.colorName || selectedCompareProduct1.color
                || 'N/A') : '-'
              }}
            </td>
            <td>
              {{ selectedCompareProduct2 ? (selectedCompareProduct2.colorName || selectedCompareProduct2.color
                || 'N/A') : '-'
              }}
            </td>
          </tr>

          <tr>
            <td>Thao tác</td>
            <td class="center">
              <NButton type="primary" color="#16a34a" size="medium" strong :loading="loadingCart"
                @click="handleBuyNow(currentProduct)">
                <template #icon>
                  <NIcon>
                    <CartOutline />
                  </NIcon>
                </template>
                Mua ngay
              </NButton>
            </td>
            <td class="center">
              <NButton v-if="selectedCompareProduct1" type="primary" size="medium" strong :loading="loadingCart"
                @click="handleBuyNow(selectedCompareProduct1)">
                <template #icon>
                  <NIcon>
                    <CartOutline />
                  </NIcon>
                </template>
                Mua ngay
              </NButton>
              <span v-else class="text-gray">-</span>
            </td>
            <td class="center">
              <NButton v-if="selectedCompareProduct2" type="primary" size="medium" strong :loading="loadingCart"
                @click="handleBuyNow(selectedCompareProduct2)">
                <template #icon>
                  <NIcon>
                    <CartOutline />
                  </NIcon>
                </template>
                Mua ngay
              </NButton>
              <span v-else class="text-gray">-</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </NModal>
</template>

<style scoped>
.compare-container {
  overflow-x: auto;
}

.compare-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  background-color: white;
}

.compare-table th,
.compare-table td {
  border: 1px solid #e2e8f0;
  padding: 16px;
  vertical-align: middle;
}

.compare-table th {
  background-color: #f8fafc;
  font-weight: normal;
}

.compare-table .prop-col {
  width: 16%;
  font-weight: 700;
  background-color: #f1f5f9;
  color: #475569;
}

.compare-table .product-col {
  width: 28%;
}

.compare-header-info {
  display: flex;
  flex-direction: column;
}

.compare-name {
  font-weight: 700;
  font-size: 15px;
  color: #1e293b;
  margin: 0;
  line-height: 1.4;
}

.compare-img {
  width: 110px;
  height: 110px;
  object-fit: contain;
  border-radius: 8px;
  background-color: #f8fafc;
  padding: 8px;
  margin: 0 auto;
  display: block;
}

.price-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.old-price {
  font-size: 13px;
  color: #94a3b8;
  text-decoration: line-through;
  line-height: 1;
}

.compare-price {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
  line-height: 1;
}

.promo-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.promo-date {
  font-size: 12px;
  color: #ef4444;
  font-weight: 500;
}

.center {
  text-align: center;
}

.text-gray {
  color: #94a3b8;
  font-style: italic;
}

.empty-placeholder {
  width: 110px;
  height: 110px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  background: #f8fafc;
}
</style>
