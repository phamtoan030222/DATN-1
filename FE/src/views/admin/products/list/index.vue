<script lang="ts" setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NCol,
  NDataTable,
  NForm,
  NFormItem,
  NIcon,
  NImage,
  NInput,
  NPagination,
  NRow,
  NSelect,
  NSlider,
  NSpace,
  NSwitch,
  NTooltip,
  useNotification,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { debounce } from 'lodash'
import type { DataTableColumns } from 'naive-ui'
import * as XLSX from 'xlsx'

// API
import {
  changeProductStatus,
  getBatteries,
  getBrands,
  getOperatingSystems,
  getProducts,
  getScreens,
} from '@/service/api/admin/product/product.api'
import type { ADProductResponse, ADPRPropertiesComboboxResponse } from '@/service/api/admin/product/product.api'
import { getMinMaxPrice } from '@/service/api/admin/product/productDetail.api'
import ADProductModal from './component/ADProductModal.vue'

// ================= STATE & CONFIG =================
const router = useRouter()
const notification = useNotification()

const state = reactive({
  search: {
    q: null as string | null,
    brand: null as string | null,
    screen: null as string | null,
    battery: null as string | null,
    operatingSystem: null as string | null,
    price: [0, 100000000] as [number, number],
  },
  data: {
    products: [] as ADProductResponse[],
    screens: [] as ADPRPropertiesComboboxResponse[],
    brands: [] as ADPRPropertiesComboboxResponse[],
    batteries: [] as ADPRPropertiesComboboxResponse[],
    operatingSystems: [] as ADPRPropertiesComboboxResponse[],
  },
  pagination: {
    page: 1,
    size: 10,
    totalPages: 0,
    totalElements: 0, // Thêm nếu API trả về tổng số bản ghi
  },
})

const stateMinMaxPrice = reactive({
  priceMin: 0,
  priceMax: 100000000,
})

const expandedKeys = ref<string[]>([])
const loading = ref(false)

// Modal State
const isOpenModal = ref(false)
const productIdSelected = ref<string | undefined>(undefined)

// ================= HELPERS =================
function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

const formatTooltipRangePrice = (value: number) => formatCurrency(value)

// ================= API CALLS =================
async function fetchComboboxProperties() {
  try {
    const [screenRes, brandRes, batteryRes, osRes] = await Promise.all([
      getScreens(),
      getBrands(),
      getBatteries(),
      getOperatingSystems(),
    ])
    state.data.screens = screenRes.data
    state.data.brands = brandRes.data
    state.data.batteries = batteryRes.data
    state.data.operatingSystems = osRes.data
  }
  catch (error) {
    console.error(error)
  }
}

async function getMinMaxPriceProduct() {
  try {
    const res = await getMinMaxPrice()
    stateMinMaxPrice.priceMin = res.data.priceMin ?? 0
    stateMinMaxPrice.priceMax = res.data.priceMax ?? 100000000
    // Chỉ reset slider nếu chưa có giá trị tìm kiếm custom
    if (state.search.price[0] === 0 && state.search.price[1] === 100000000) {
      state.search.price = [stateMinMaxPrice.priceMin, stateMinMaxPrice.priceMax]
    }
  }
  catch (e) {
    console.error(e)
  }
}

async function fetchProducts() {
  loading.value = true
  try {
    const res = await getProducts({
      page: state.pagination.page,
      size: state.pagination.size,
      q: state.search.q || '',
      idBrand: state.search.brand || '',
      idOperatingSystem: state.search.operatingSystem || '',
      idBattery: state.search.battery || '',
      idScreen: state.search.screen || '',
      minPrice: state.search.price[0],
      maxPrice: state.search.price[1],
    })

    state.data.products = res.data.data
    state.pagination.totalPages = res.data.totalPages
    // Nếu API trả về totalElements thì gán vào đây để phân trang chuẩn hơn
    // state.pagination.totalElements = res.data.totalElements
  }
  catch (e) {
    console.error(e)
  }
  finally {
    loading.value = false
  }
}

// ================= ACTIONS =================
function refreshFilter() {
  state.search.q = null
  state.search.battery = null
  state.search.brand = null
  state.search.screen = null
  state.search.operatingSystem = null
  state.search.price = [stateMinMaxPrice.priceMin, stateMinMaxPrice.priceMax]
  state.pagination.page = 1
  fetchProducts()
}

function handleClickAddProduct(id?: string) {
  if (id)
    router.push({ name: 'products_add', params: { id } }) // Add variant
  else
    router.push({ name: 'products_add' }) // Add new
}

function redirectVariant(id: string) {
  router.push({ name: 'products_variant', query: { idProduct: id } })
}

async function handleChangeStatus(id: string) {
  try {
    loading.value = true
    const res = await changeProductStatus(id)
    if (res.success)
      notification.success({ content: 'Thay đổi trạng thái thành công', duration: 3000 })
    else
      notification.error({ content: 'Thay đổi trạng thái thất bại', duration: 3000 })
    fetchProducts()
  }
  catch (e) {
    notification.error({ content: 'Lỗi hệ thống', duration: 3000 })
  }
  finally {
    loading.value = false
  }
}

function clickOpenModal(id?: string) {
  productIdSelected.value = id
  isOpenModal.value = true
}

function handleCloseModal() {
  isOpenModal.value = false
  productIdSelected.value = undefined
}

// Debounce search
const debounceFetchProducts = debounce(() => {
  state.pagination.page = 1
  fetchProducts()
}, 300)

watch(
  () => [
    state.search.q,
    state.search.operatingSystem,
    state.search.battery,
    state.search.brand,
    state.search.screen,
    state.search.price[0],
    state.search.price[1],
  ],
  () => { debounceFetchProducts() },
)

onMounted(() => {
  fetchComboboxProperties()
  getMinMaxPriceProduct().then(() => fetchProducts())
})

// ================= COLUMNS =================
const columns: DataTableColumns<ADProductResponse> = [
  {
    title: 'STT',
    key: 'orderNumber',
    align: 'center',
    render: (_, index) => (state.pagination.page - 1) * state.pagination.size + index + 1,
  },
  {
    title: 'Ảnh',
    key: 'image',
    align: 'center',
    render: row => h(NImage, {
      width: 80,
      src: row.urlImage || 'https://via.placeholder.com/50',
      style: { borderRadius: '4px' },
    }),
  },
  {
    title: 'Mã sản phẩm',
    key: 'code',
    render: row => h('strong', { class: 'text-primary' }, row.code),
  },
  {
    title: 'Tên sản phẩm',
    key: 'name',
    ellipsis: { tooltip: true },
  },
  {
    title: 'Cấu hình',
    key: 'configuration',
    width: 300,
    align: 'left',
    render: (row: ADProductResponse) => h('div',
      [
        h('div', { span: 6 }, [h('div', { class: 'flex items-center gap-2' }, [h(Icon, { icon: 'carbon:tag' }), h('span', `Hãng: ${row.brand}`)])]),
        h('div', { span: 6 }, [h('div', { class: 'flex items-center gap-2' }, [h(Icon, { icon: 'carbon:battery-half' }), h('span', `Pin: ${row.battery}`)])]),
        h('div', { span: 6 }, [h('div', { class: 'flex items-center gap-2' }, [h(Icon, { icon: 'carbon:carbon-for-ibm-dotcom' }), h('span', `OS: ${row.operatingSystem}`)])]),
        h('div', { span: 6 }, [h('div', { class: 'flex items-center gap-2' }, [h(Icon, { icon: 'icon-park-outline:monitor' }), h('span', `Màn hình: ${row.screen}`)])]),
      ],
    )
  },
  {
    title: 'Khoảng giá',
    key: 'price',
    width: 130,
    render: (row) => {
      if (row.minPrice === row.maxPrice)
        return h('span', { class: 'font-medium text-red-600' }, formatCurrency(row.minPrice))
      return h('div', { class: 'flex flex-col text-xs' }, [
        h('span', `Từ: ${formatCurrency(row.minPrice)}`),
        h('span', `Đến: ${formatCurrency(row.maxPrice)}`),
      ])
    },
  },
  {
    title: 'Số lượng',
    key: 'quantity',
    width: 70,
    align: 'center',
    render: row => h('span', { class: 'font-medium' }, row.quantity),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    width: 70,
    render: row => h(NSwitch, {
      value: row.status === 'ACTIVE',
      size: 'small',
      onUpdateValue: () => handleChangeStatus(row.id as string),
    }),
  },
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    width: 150,
    fixed: 'right',
    render: (row) => {
      return h('div', { class: 'flex justify-center gap-3' }, [
        // 1. Xem phiên bản (Info)
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            size: 'small',
            circle: true,
            secondary: true, // Dùng secondary đẹp hơn quaternary
            type: 'info',
            class: 'transition-all duration-200 hover:scale-125 hover:shadow-md', // Hiệu ứng scale
            onClick: () => redirectVariant(row.id as string),
          }, { icon: () => h(Icon, { icon: 'carbon:list-boxes' }) }),
          default: () => 'Xem phiên bản',
        }),

        // 2. Sửa nhanh (Warning)
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            size: 'small',
            circle: true,
            secondary: true,
            type: 'warning',
            class: 'transition-all duration-200 hover:scale-125 hover:shadow-md',
            onClick: () => clickOpenModal(row.id),
          }, { icon: () => h(Icon, { icon: 'carbon:edit' }) }),
          default: () => 'Sửa nhanh',
        }),

        // 3. Thêm phiên bản (Success)
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            size: 'small',
            circle: true,
            secondary: true,
            type: 'success',
            class: 'transition-all duration-200 hover:scale-125 hover:shadow-md',
            onClick: () => handleClickAddProduct(row.id as string),
          }, { icon: () => h(Icon, { icon: 'carbon:add-alt' }) }),
          default: () => 'Thêm phiên bản',
        }),
      ])
    },
  },
]

function exportToExcel(dataList: ADProductResponse[], fileName: string) {
  const excelData = dataList.map((item, index) => {
    return {
      'STT': index + 1,
      'Mã sản phẩm': item.code,
      'Tên sản phẩm': item.name,
      'Hãng': item.brand,
      'Pin': item.battery,
      'Màn hình': item.screen,
      'Hệ điều hành': item.operatingSystem,
      'Giá nhỏ nhất': item.minPrice,
      'Giá lớn nhất': item.maxPrice,
      'Số lượng': item.quantity,
      'Link ảnh sản phẩm': item.urlImage,
    }
  })

  const ws = XLSX.utils.json_to_sheet(excelData)
  ws['!cols'] = [{ wch: 5 }, { wch: 15 }, { wch: 25 }, { wch: 10 }, { wch: 15 }, { wch: 25 }, { wch: 40 }, { wch: 15 }, { wch: 15 }, { wch: 15 }, { wch: 20 }, { wch: 20 }, { wch: 30 }]

  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Data')
  XLSX.writeFile(wb, `${fileName}_${new Date().getTime()}.xlsx`)
  notification.success({ content: 'Xuất Excel thành công', duration: 3000 })
}

const exportExcelHandler = () => {
  exportToExcel(state.data.products, 'Danh_sach_san_pham')
}
</script>

<template>
  <div>
    <NCard class="mb-3">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-orange-600">
            <Icon icon="icon-park-outline:ad-product" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý Sản phẩm
          </span>
        </NSpace>
        <span>Quản lý danh sách sản phẩm và các biến thể trong cửa hàng</span>
      </NSpace>
    </NCard>

    <NCard title="Bộ lọc tìm kiếm" class="rounded-2xl shadow-md mb-4">
      <template #header-extra>
        <div class="mr-5">
          <NTooltip trigger="hover" placement="top">
            <template #trigger>
              <NButton size="large" circle secondary type="primary"
                class="transition-all duration-200 hover:scale-110 hover:shadow-md" @click="refreshFilter">
                <NIcon size="24">
                  <Icon icon="carbon:filter-reset" />
                </NIcon>
              </NButton>
            </template>
            Làm mới bộ lọc
          </NTooltip>
        </div>
      </template>

      <NForm label-placement="top">
        <NRow :gutter="24">
          <NCol :span="8">
            <NFormItem label="Tìm kiếm">
              <NInput v-model:value="state.search.q" placeholder="Tên sản phẩm, mã sản phẩm..." clearable>
                <template #prefix>
                  <Icon icon="carbon:search" />
                </template>
              </NInput>
            </NFormItem>
          </NCol>

          <NCol :span="4">
            <NFormItem label="Thương hiệu">
              <NSelect v-model:value="state.search.brand" :options="state.data.brands" placeholder="Chọn hãng" clearable
                filterable />
            </NFormItem>
          </NCol>

          <NCol :span="4">
            <NFormItem label="Hệ điều hành">
              <NSelect v-model:value="state.search.operatingSystem" :options="state.data.operatingSystems"
                placeholder="Chọn OS" clearable filterable />
            </NFormItem>
          </NCol>

          <NCol :span="4">
            <NFormItem label="Loại Pin">
              <NSelect v-model:value="state.search.battery" :options="state.data.batteries" placeholder="Chọn pin"
                clearable filterable />
            </NFormItem>
          </NCol>

          <NCol :span="4">
            <NFormItem label="Màn hình">
              <NSelect v-model:value="state.search.screen" :options="state.data.screens" placeholder="Chọn màn hình"
                clearable filterable />
            </NFormItem>
          </NCol>

          <NCol :span="24">
            <NFormItem label="Khoảng giá">
              <NSlider v-model:value="state.search.price" range :step="100000" :min="stateMinMaxPrice.priceMin"
                :max="stateMinMaxPrice.priceMax" :format-tooltip="formatTooltipRangePrice" />
            </NFormItem>
          </NCol>
        </NRow>
      </NForm>
    </NCard>

    <NCard title="Danh sách sản phẩm" class="border rounded-3">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton type="success" @click="exportExcelHandler" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="file-icons:microsoft-excel" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Xuất Excel
              </span>
            </NButton>
            <NButton type="primary" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="handleClickAddProduct()">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:add" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Thêm sản phẩm
              </span>
            </NButton>
            <NButton type="info" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="fetchProducts">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Tải lại
              </span>
            </NButton>
          </NSpace>
        </div>
      </template>

      <NDataTable v-model:expanded-row-keys="expandedKeys" :columns="columns" :data="state.data.products"
        :loading="loading" :row-key="(row) => row.id" :pagination="false" striped />

      <div class="flex justify-end mt-4">
        <NPagination v-model:page="state.pagination.page" v-model:page-size="state.pagination.size"
          :page-count="state.pagination.totalPages" :page-sizes="[5, 10, 20, 50]" show-size-picker
          @update:page="fetchProducts"
          @update:page-size="(val) => { state.pagination.size = val; state.pagination.page = 1; fetchProducts() }" />
      </div>
    </NCard>

    <ADProductModal :id="productIdSelected" :is-open="isOpenModal" :is-detail="true" :screens="state.data.screens"
      :batteries="state.data.batteries" :brands="state.data.brands" :operating-systems="state.data.operatingSystems"
      @close="handleCloseModal" @success="fetchProducts" />
  </div>
</template>

<style scoped>
/* Không cần style thủ công nhiều vì đã dùng Tailwind/Naive UI class */
</style>
