<template>
    <div>
        <n-card>
            <NSpace vertical :size="8">
                <NSpace align="center">
                    <div>
                        <NIcon size="24">
                            <Icon icon="icon-park-outline:list" />
                        </NIcon>
                        <span style="font-weight: 600; font-size: 24px">
                            Quản lý biến thể sản phẩm {{ idProduct ? `${product?.code} - ${product?.name}` : '' }}
                        </span>
                    </div>
                    <div>
                        <!-- // chọn sản phẩm -->
                    </div>
                </NSpace>
                <span>Quản lý biến thể sản phẩm trong cửa hàng</span>
            </NSpace>
        </n-card>
        <n-card class="mt-20px" title="Bộ lọc">
            <template #header-extra>
                <n-flex justify="end" strong secondary style="height: 100%;">
                    <n-button @click="refreshFilter" circle type="success">
                        <Icon icon="material-symbols:refresh"></Icon>
                    </n-button>
                </n-flex>
            </template>
            <n-row gutter="12">
                <n-col :span="6">
                    <span>Tìm kiếm</span>
                    <n-input v-model:value="state.search.q" placeholder="Tìm kiếm" clearable />
                </n-col>
                <n-col :span="3">
                    <span>CPU</span>
                    <n-select v-model:value="state.search.cpu" placeholder="Tìm kiếm" :options="state.data.cpus"
                        clearable />
                </n-col>
                <n-col :span="3">
                    <span>GPU</span>
                    <n-select v-model:value="state.search.gpu" placeholder="Tìm kiếm" :options="state.data.gpus"
                        clearable />
                </n-col>
                <n-col :span="3">
                    <span>Màu sắc</span>
                    <n-select v-model:value="state.search.color" placeholder="Tìm kiếm" :options="state.data.colors"
                        clearable />
                </n-col>
                <n-col :span="3">
                    <span>ổ cứng</span>
                    <n-select v-model:value="state.search.hardDrive" placeholder="Tìm kiếm"
                        :options="state.data.hardDrives" clearable />
                </n-col>
                <n-col :span="3">
                    <span>Chất liệu</span>
                    <n-select v-model:value="state.search.material" placeholder="Tìm kiếm"
                        :options="state.data.materials" clearable />
                </n-col>
                <n-col :span="3">
                    <span>RAM</span>
                    <n-select v-model:value="state.search.ram" placeholder="Tìm kiếm" :options="state.data.rams"
                        clearable />
                </n-col>
            </n-row>
            <div class="mt-20px">
                <n-row :gutter="12">
                    <n-col :span="16">
                        <span>Khoảng giá</span>
                        <n-slider v-model:value="state.search.price" :format-tooltip="formatTooltipRangePrice" range
                            :step="1000" :min="stateMinMaxPrice.priceMin ?? 0"
                            :max="stateMinMaxPrice.priceMax ?? 50000000" />
                    </n-col>
                </n-row>

            </div>
        </n-card>
        <n-card class="mt-20px" title="Danh sách biến thể">
            <n-data-table :columns="columns" :data="state.data.productDetails"></n-data-table>

            <n-space justify="center" class="mt-20px">
                <NPagination :page="state.pagination.page" :page-size="state.pagination.size"
                    :page-count="state.pagination.totalPages" @update:page="handlePageChange" />
            </n-space>
        </n-card>

        <ADProductVariantModal :is-open="isOpenModal" :id="productDetailIdSelected" :cpus="state.data.cpus"
            :gpus="state.data.gpus" :hard-drives="state.data.hardDrives" :materials="state.data.materials"
            :colors="state.data.colors" :rams="state.data.rams" @success="handleSuccessModifyModal"
            @close="closeModal" />
    </div>
</template>

<script lang="ts" setup>
import { ADProductDetailResponse, ADPRPropertiesComboboxResponse, changeProductDetailStatus, getColors, getCPUs, getGPUs, getHardDrives, getMaterials, getMinMaxPrice, getProductDetails, getRAMs } from '@/service/api/admin/product/productDetail.api'
import { ADProductDetailResponse as ADProductResponse } from '@/service/api/admin/product/product.api'
import { Icon } from '@iconify/vue'
import { debounce } from 'lodash'
import { DataTableColumns, NButton, NImage, NSpace, NSwitch } from 'naive-ui'
import { onMounted, reactive, ref, Ref } from 'vue'
import ADProductVariantModal from './component/ADProductVariantModal.vue'
import { getProductById } from '@/service/api/admin/product/product.api'

const route = useRoute();

const idProduct: Ref<string> = ref(route.query.idProduct as string)

const state = reactive({
    search: {
        q: undefined as string | undefined,
        cpu: undefined as string | undefined,
        gpu: undefined as string | undefined,
        hardDrive: undefined as string | undefined,
        ram: undefined as string | undefined,
        color: undefined as string | undefined,
        material: undefined as string | undefined,
        price: [10000, 50000000]
    },
    data: {
        productDetails: [] as ADProductDetailResponse[],
        materials: [] as ADPRPropertiesComboboxResponse[],
        colors: [] as ADPRPropertiesComboboxResponse[],
        rams: [] as ADPRPropertiesComboboxResponse[],
        hardDrives: [] as ADPRPropertiesComboboxResponse[],
        cpus: [] as ADPRPropertiesComboboxResponse[],
        gpus: [] as ADPRPropertiesComboboxResponse[],
    },
    pagination: {
        page: 1,
        size: 10,
        totalPages: undefined as number | undefined,
    },
})

const product: Ref<ADProductResponse | null> = ref(null)

const fetchProduct = async () => {
    const res = await getProductById(idProduct.value)

    product.value = res.data
}

const fetchProductDetails = async () => {
    const res = await getProductDetails({
        page: state.pagination.page,
        size: state.pagination.size,
        q: state.search.q,
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

    getMinMaxPriceProduct();
}

const fetchComboboxProperties = async () => {
    try {
        const [colorProperties, cpuProperties, gpuProperties, materialProperties, ramSystemProperties, hardDriveProperties] = await Promise.all([
            getColors(),
            getCPUs(),
            getGPUs(),
            getMaterials(),
            getRAMs(),
            getHardDrives()
        ])

        state.data.colors = colorProperties.data
        state.data.cpus = cpuProperties.data
        state.data.gpus = gpuProperties.data
        state.data.materials = materialProperties.data
        state.data.rams = ramSystemProperties.data
        state.data.hardDrives = hardDriveProperties.data
    } catch (error) {
        console.log(error)
    }
}

const refreshFilter = () => {
    state.search.q = undefined
    state.search.cpu = undefined
    state.search.gpu = undefined
    state.search.material = undefined
    state.search.ram = undefined
    state.search.color = undefined
    state.search.hardDrive = undefined
}

const columns: DataTableColumns<ADProductDetailResponse> = [
    { type: 'selection', fixed: 'left' },
    {
        title: '#', key: 'orderNumber', width: 50, fixed: 'left', render: (data: ADProductDetailResponse, index: number) => {
            return h('span', { innerText: index + 1 })
        }
    },
    { title: 'Mã', key: 'code', width: 100, fixed: 'left', },
    { title: 'Tên', key: 'name', width: 100 },
    {
        title: 'Ảnh sản phẩm', key: 'brand', width: 150, align: 'center',
        render: (data: ADProductDetailResponse) => {
            return h(NImage, { width: 200, src: data.urlImage })
        }
    },
    {
        title: 'Giá', key: 'price', width: 150, align: 'center',
        render: (data: ADProductDetailResponse) => h('span', (data.price + '').split('').reduce((prev, curr, index, arr) => {
            if ((arr.length - index) % 3 == 0) return prev + '.' + curr
            return prev + curr
        }, '') + ' vnđ')
    },
    {
        title: 'Tồn kho', key: 'quantity', width: 150, align: 'center',
        render: (data: ADProductDetailResponse) => h('span', data.quantity + ' sản phẩm')
    },
    { title: 'CPU', key: 'cpu', width: 150, align: 'center', },
    { title: 'GPU', key: 'gpu', width: 150, align: 'center', },
    { title: 'RAM', key: 'ram', width: 200, align: 'center', },
    { title: 'Ổ cứng', key: 'hardDrive', width: 200, align: 'center', },
    { title: 'Màu', key: 'color', width: 150, align: 'center', },
    { title: 'Chất liệu', key: 'material', width: 150, align: 'center', },
    {
        title: 'Trạng thái', key: 'status', width: 70, align: 'center',
        render: (data: ADProductDetailResponse) => h(NSwitch, { value: data.status == 'ACTIVE', onUpdateValue: (value: boolean) => { handleChangeStatus(data.id as string) } })
    },
    {
        title: 'Thao tác', key: 'action', width: 100, fixed: 'right',
        render: (data: ADProductDetailResponse) => {
            return h(NSpace,
                [
                    h(NButton, {
                        quaternary: true, size: 'small', circle: true,
                        onClick: () => clickOpenModal(data.id, true)
                    },
                        h(Icon, { icon: 'carbon:edit' })
                    ),
                    // h(NButton, {
                    //     strong: true, circle: true, type: 'warning',
                    //     onClick: () => clickOpenModal(data.id)
                    // },
                    //     h(Icon, { icon: 'carbon:edit' })
                    // )
                ]
            )
        }
    },
]

onMounted(() => {
    fetchProductDetails()
    if (idProduct.value) {
        fetchProduct()
    }
    fetchComboboxProperties()
    initSearchPrice()

})

const initSearchPrice = async () => {
    const res = await getMinMaxPrice()
    state.search.price = [res.data.priceMin as number, res.data.priceMax as number]
}

const isOpenModal = ref<boolean>(false)

const isDetailModal: Ref<boolean> = ref(true)

const productDetailIdSelected = ref<string>()

const clickOpenModal = (id?: string, isDetail?: boolean) => {
    productDetailIdSelected.value = id
    isOpenModal.value = true
    isDetailModal.value = isDetail ?? false
}

const closeModal = () => {
    isOpenModal.value = false
}

const handleSuccessModifyModal = () => {
    fetchProductDetails()
    closeModal()
}

const debounceFetchProducts = debounce(fetchProductDetails, 300)

watch(
    () => [state.search.q, state.search.cpu, state.search.gpu, state.search.hardDrive, state.search.material, state.search.ram, state.search.color, state.pagination.page, state.pagination.size, state.search.price[0], state.search.price[1]],
    () => {
        debounceFetchProducts()
    }
)

const notification = useNotification()

const handleChangeStatus = async (id: string) => {
    const res = await changeProductDetailStatus(id)

    if (res.success) notification.success({ content: 'Thay đổi trạng thái thành công', duration: 3000 })
    else notification.error({ content: 'Thay đổi trạng thái thất bại', duration: 3000 })

    fetchProductDetails();
}

const handlePageChange = (page: number) => {
    state.pagination.page = page
}

const stateMinMaxPrice = reactive<{ priceMin: number | undefined, priceMax: number | undefined }>({
    priceMin: undefined,
    priceMax: undefined,
})

const getMinMaxPriceProduct = async () => {
    const res = await getMinMaxPrice()
    stateMinMaxPrice.priceMin = res.data.priceMin
    stateMinMaxPrice.priceMax = res.data.priceMax
}

const formatTooltipRangePrice = (value: number) => (value + '').split('').reduce((prev, curr, index, arr) => {
    if ((arr.length - index) % 3 == 0) return prev + ' ' + curr
    return prev + curr
}, '') + ' vnđ'
</script>

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