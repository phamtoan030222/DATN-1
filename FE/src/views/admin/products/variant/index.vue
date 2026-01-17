<script lang="ts" setup>
import type { ADProductDetailResponse, ADPRPropertiesComboboxResponse } from '@/service/api/admin/product/productDetail.api'
import { changeProductDetailStatus, getColors, getCPUs, getGPUs, getHardDrives, getMaterials, getMinMaxPrice, getProductDetails, getRAMs } from '@/service/api/admin/product/productDetail.api'
import type { ADProductDetailResponse as ADProductResponse } from '@/service/api/admin/product/product.api'
import { getProductById, getProductsCombobox } from '@/service/api/admin/product/product.api'
import { Icon } from '@iconify/vue'
import { debounce } from 'lodash'
import type { DataTableColumns } from 'naive-ui'
import { NBadge, NButton, NImage, NSpace, NSwitch } from 'naive-ui'
import type { Ref } from 'vue'
import { onMounted, reactive, ref } from 'vue'
import ADProductVariantModal from './component/ADProductVariantModal.vue'
import { exportToExcel } from '@/utils/excel.helper'

const route = useRoute()

const idProduct: Ref<string> = ref(route.query.idProduct as string)

const state = reactive({
  search: {
    q: undefined as string | undefined | null,
    cpu: undefined as string | undefined | null,
    gpu: undefined as string | undefined | null,
    hardDrive: undefined as string | undefined | null,
    ram: undefined as string | undefined | null,
    color: undefined as string | undefined | null,
    material: undefined as string | undefined | null,
    price: [10000, 50000000],
  },
  data: {
    productDetails: [] as ADProductDetailResponse[],
    materials: [] as ADPRPropertiesComboboxResponse[],
    colors: [] as ADPRPropertiesComboboxResponse[],
    rams: [] as ADPRPropertiesComboboxResponse[],
    hardDrives: [] as ADPRPropertiesComboboxResponse[],
    cpus: [] as ADPRPropertiesComboboxResponse[],
    gpus: [] as ADPRPropertiesComboboxResponse[],
    productsCombobox: [] as ADPRPropertiesComboboxResponse[],
  },
  pagination: {
    page: 1,
    size: 10,
    totalPages: undefined as number | undefined,
  },
})

const product: Ref<ADProductResponse | null> = ref(null)

async function fetchProduct() {
  const res = await getProductById(idProduct.value)

  product.value = res.data
}

async function fetchProductDetails() {
  const res = await getProductDetails({
    page: state.pagination.page,
    size: state.pagination.size,
    q: state.search.q as string,
    idProduct: idProduct.value,
    idCPU: state.search.cpu,
    idGPU: state.search.gpu,
    idColor: state.search.color,
    idRAM: state.search.ram,
    idHardDrive: state.search.hardDrive,
    idMaterial: state.search.material,
    minPrice: state.search.price[0],
    maxPrice: state.search.price[1],
  })

  state.data.productDetails = res.data.data
  state.pagination.totalPages = res.data.totalPages

  getMinMaxPriceProduct()
}

async function fetchComboboxProperties() {
  try {
    const [colorProperties, cpuProperties, gpuProperties, materialProperties, ramSystemProperties, hardDriveProperties] = await Promise.all([
      getColors(),
      getCPUs(),
      getGPUs(),
      getMaterials(),
      getRAMs(),
      getHardDrives(),
    ])

    state.data.colors = colorProperties.data
    state.data.cpus = cpuProperties.data
    state.data.gpus = gpuProperties.data
    state.data.materials = materialProperties.data
    state.data.rams = ramSystemProperties.data
    state.data.hardDrives = hardDriveProperties.data
  }
  catch (error) {
    console.log(error)
  }
}

function refreshFilter() {
  state.search.q = null
  state.search.cpu = null
  state.search.gpu = null
  state.search.material = null
  state.search.ram = null
  state.search.color = null
  state.search.hardDrive = null
}

const expandedKeys = ref<Array<string>>([])

const columns: DataTableColumns<ADProductDetailResponse> = [
  {
    title: '#',
    key: 'orderNumber',
    width: 50,
    fixed: 'left',
    render: (data: ADProductDetailResponse, index: number) => {
      return h('span', { innerText: index + 1 })
    },
  },
  { title: 'Mã', key: 'code', width: 120, fixed: 'left' },
  {
    title: 'Ảnh sản phẩm',
    key: 'brand',
    width: 80,
    align: 'center',
    render: (data: ADProductDetailResponse) => {
      return h(NBadge, { value: data.percentage ? `-${data.percentage}%` : undefined }, [
        h(NImage, { width: 200, src: data.urlImage })
      ])
    },
  },
  {
    title: 'Cấu hình', key: 'configuration', width: 400, align: 'left', titleAlign: 'center',
    render: (rowData: ADProductDetailResponse) => h('div', { class: 'flex gap-1 justify-around' }, [
      h('div', [
        h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'icon-park-outline:platte' }), h('span', { style: { marginLeft: '8px' }, innerText: `Màu: ${rowData.color}` })]),
        h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'icon-park-outline:cpu' }), h('span', { style: { marginLeft: '8px' }, innerText: `CPU: ${rowData.cpu}` })]),
        h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'icon-park-outline:full-screen-play' }), h('span', { style: { marginLeft: '8px' }, innerText: `GPU: ${rowData.gpu}` })]),
      ]),
      h('div', [
        h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'icon-park-outline:memory' }), h('span', { style: { marginLeft: '8px' }, innerText: `RAM: ${rowData.ram}` })]),
        h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'icon-park-outline:loading' }), h('span', { style: { marginLeft: '8px' }, innerText: `Chất liệu: ${rowData.material}` })]),
        h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'icon-park-outline:hdd' }), h('span', { style: { marginLeft: '8px' }, innerText: `Ổ cứng: ${rowData.hardDrive}` })]),
      ]),
    ]),
  },
  {
    title: 'Giá',
    key: 'price',
    width: 150,
    align: 'center',
    render: (data: ADProductDetailResponse) => h('div', { style: { display: 'flex', flexDirection: 'column', justifyContent: 'start' } }, [
      h('span',{ style:  data.percentage ? {textDecoration: 'line-through', color: '#999', fontSize: '11px'} : {textDecoration: 'none', color: 'black', fontSize: '15px'} }, `${(`${data.price}`).split('').reduce((prev, curr, index, arr) => {
        if ((arr.length - index) % 3 == 0)
          return `${prev} ${curr}`
        return prev + curr
      }, '')} vnđ`),
      data.percentage && h('span', { style: {color: '#d03050', fontWeight: 'bold'} }, `${(`${data.price * (100 - data.percentage) / 100}`).split('').reduce((prev, curr, index, arr) => {
        if ((arr.length - index) % 3 == 0)
          return `${prev} ${curr}`
        return prev + curr
      }, '')} vnđ`),
    ]),
  },
  {
    title: 'Tồn kho',
    key: 'quantity',
    width: 150,
    align: 'center',
    render: (data: ADProductDetailResponse) => h('span', `${data.quantity} sản phẩm`),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 70,
    align: 'center',
    render: (data: ADProductDetailResponse) => h(NSwitch, { value: data.status == 'ACTIVE', onUpdateValue: (value: boolean) => { handleChangeStatus(data.id as string) } }),
  },
  {
    title: 'Thao tác',
    key: 'action',
    width: 100,
    fixed: 'right',
    render: (data: ADProductDetailResponse) => {
      return h(NSpace, [
        h(NButton, {
          quaternary: true,
          size: 'small',
          circle: true,
          onClick: () => clickOpenModal(data.id, true),
        }, h(Icon, { icon: 'carbon:edit' })),
        // h(NButton, {
        //     strong: true, circle: true, type: 'warning',
        //     onClick: () => clickOpenModal(data.id)
        // },
        //     h(Icon, { icon: 'carbon:edit' })
        // )
      ])
    },
  },
]

onMounted(async () => {
  await initSearchPrice()
  fetchProductsCombobox()
  if (idProduct.value) {
    fetchProduct()
  }
  fetchComboboxProperties()
  fetchProductDetails()
})

async function initSearchPrice() {
  const res = await getMinMaxPrice()
  state.search.price = [res.data.priceMin as number, res.data.priceMax as number]
}

const isOpenModal = ref<boolean>(false)

const isDetailModal: Ref<boolean> = ref(true)

const productDetailIdSelected = ref<string>()

function clickOpenModal(id?: string, isDetail?: boolean) {
  productDetailIdSelected.value = id
  isOpenModal.value = true
  isDetailModal.value = isDetail ?? false
}

function closeModal() {
  isOpenModal.value = false
}

function handleSuccessModifyModal() {
  fetchProductDetails()
  closeModal()
}

const debounceFetchProducts = debounce(fetchProductDetails, 300)

watch(
  () => [state.search.q, state.search.cpu, state.search.gpu, state.search.hardDrive, state.search.material, state.search.ram, state.search.color, state.pagination.page, state.pagination.size, state.search.price[0], state.search.price[1]],
  () => {
    debounceFetchProducts()
  },
)

const notification = useNotification()

async function handleChangeStatus(id: string) {
  const res = await changeProductDetailStatus(id)

  if (res.success)
    notification.success({ content: 'Thay đổi trạng thái thành công', duration: 3000 })
  else notification.error({ content: 'Thay đổi trạng thái thất bại', duration: 3000 })

  fetchProductDetails()
}

function handlePageChange(page: number) {
  state.pagination.page = page
}

const stateMinMaxPrice = reactive<{ priceMin: number | undefined, priceMax: number | undefined }>({
  priceMin: undefined,
  priceMax: undefined,
})

async function getMinMaxPriceProduct() {
  const res = await getMinMaxPrice()
  stateMinMaxPrice.priceMin = res.data.priceMin
  stateMinMaxPrice.priceMax = res.data.priceMax
}

function formatTooltipRangePrice(value: number) {
  return `${(`${value}`).split('').reduce((prev, curr, index, arr) => {
    if ((arr.length - index) % 3 == 0)
      return `${prev} ${curr}`
    return prev + curr
  }, '')} vnđ`
}

async function fetchProductsCombobox() {
  const res = await getProductsCombobox()

  state.data.productsCombobox = res.data
}

const exportExcelHandler = () => {
  const excelData = state.data.productDetails.map((item, index) => ({
    'STT': index + 1,
    'Mã biến thể': item.code,
    'Tên biến thể': item.name,
    'Màu sắc': item.color,
    'CPU': item.cpu,
    'GPU': item.gpu,
    'RAM': item.ram,
    'Ổ cứng': item.hardDrive,
    'Chất liệu': item.material,
    'Giá': item.price,
    'Giá khuyến mãi': `${item.percentage}%`,
    'Tồn kho': item.quantity,
  }))

  exportToExcel(excelData, `Danh_sach_bien_the_san_pham_${product.value ? product.value.code : ''}`)
}
</script>

<template>
  <div>
    <n-card>
      <NSpace vertical :size="8">
        <NSpace align="center" justify="space-between">
          <div>
            <NIcon size="24">
              <Icon icon="icon-park-outline:list" />
            </NIcon>
            <span style="font-weight: 600; font-size: 24px">
              Quản lý biến thể sản phẩm {{ idProduct ? `${product?.code} - ${product?.name}` : '' }}
            </span>
          </div>
          <div>
            <n-select v-if="state.data.productsCombobox.length > 0" v-model:value="idProduct"
              placeholder="Chọn sản phẩm" style="width: 500px;" clearable :options="state.data.productsCombobox"
              @update:value="() => { fetchProductDetails(); fetchProduct(); }" />
          </div>
        </NSpace>
        <span>Quản lý biến thể sản phẩm trong cửa hàng</span>
      </NSpace>
    </n-card>
    <n-card class="mt-20px" title="Bộ lọc">
      <template #header-extra>
        <n-flex justify="end" strong secondary style="height: 100%;">
          <NButton circle type="success" @click="refreshFilter">
            <Icon icon="material-symbols:refresh" />
          </NButton>
        </n-flex>
      </template>
      <n-row gutter="12">
        <n-col :span="8">
          <span>Tìm kiếm</span>
          <n-input v-model:value="state.search.q" placeholder="Tìm kiếm" clearable />
        </n-col>
        <n-col :span="4">
          <span>CPU</span>
          <n-select v-model:value="state.search.cpu" placeholder="Tìm kiếm CPU" :options="state.data.cpus" clearable />
        </n-col>
        <n-col :span="4">
          <span>GPU</span>
          <n-select v-model:value="state.search.gpu" placeholder="Tìm kiếm GPU" :options="state.data.gpus" clearable />
        </n-col>
        <n-col :span="4">
          <span>Màu sắc</span>
          <n-select v-model:value="state.search.color" placeholder="Tìm kiếm màu sắc" :options="state.data.colors"
            clearable />
        </n-col>
        <n-col :span="4">
          <span>ổ cứng</span>
          <n-select v-model:value="state.search.hardDrive" placeholder="Tìm kiếm ổ cứng"
            :options="state.data.hardDrives" clearable />
        </n-col>
      </n-row>
      <div class="mt-20px">
        <n-row :gutter="12">
          <n-col :span="16">
            <span>Khoảng giá</span>
            <n-slider v-model:value="state.search.price" :format-tooltip="formatTooltipRangePrice" range :step="1000"
              :min="stateMinMaxPrice.priceMin ?? 0" :max="stateMinMaxPrice.priceMax ?? 50000000" />
          </n-col>
          <n-col :span="4">
            <span>Chất liệu</span>
            <n-select v-model:value="state.search.material" placeholder="Tìm kiếm chất liệu"
              :options="state.data.materials" clearable />
          </n-col>
          <n-col :span="4">
            <span>RAM</span>
            <n-select v-model:value="state.search.ram" placeholder="Tìm kiếm RAM" :options="state.data.rams"
              clearable />
          </n-col>
        </n-row>
      </div>
    </n-card>
    <n-card class="mt-20px" title="Danh sách biến thể">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton @click="exportExcelHandler" type="success" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out">
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
          </NSpace>
        </div>
      </template>
      <n-data-table :columns="columns" :data="state.data.productDetails" :expanded-row-keys="expandedKeys"
        :row-key="(row) => row.id" @update:expanded-row-keys="key => expandedKeys = key as Array<string>" />

      <NSpace justify="end" class="mt-20px">
        <NPagination :page="state.pagination.page" :page-size="state.pagination.size" :page-sizes="[5, 10, 20, 50]"
          show-size-picker :page-count="state.pagination.totalPages" @update:page="handlePageChange" />
      </NSpace>
    </n-card>

    <ADProductVariantModal :id="productDetailIdSelected" :is-open="isOpenModal" :cpus="state.data.cpus"
      :gpus="state.data.gpus" :hard-drives="state.data.hardDrives" :materials="state.data.materials"
      :colors="state.data.colors" :rams="state.data.rams" @success="handleSuccessModifyModal" @close="closeModal" />
  </div>
</template>

<style scoped>
.container {
  padding: 0 20px 20px;
}

.size-upload-image {
  width: 100% !important;
  height: 100% !important;
}

.ml-32px {
  margin-left: 32px;
}

.mb-64px {
  margin-bottom: 64px;
}

.mb-32px {
  margin-bottom: 32px;
}

.ml-8px {
  margin-left: 8px;
}

.mt-20px {
  margin-top: 20px;
}

.size-icon {
  width: 16px;
  height: 16px;
}

.price-hover:hover {
  cursor: pointer;
}
</style>
