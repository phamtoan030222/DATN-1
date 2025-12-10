<template>
    <div>
        <n-card>
            <NSpace vertical :size="8">
                <NSpace align="center">
                    <NIcon size="24">
                        <Icon icon="icon-park-outline:list" />
                    </NIcon>
                    <span style="font-weight: 600; font-size: 24px">
                        Quản lý sản phẩm
                    </span>
                </NSpace>
                <span>Quản lý sản phẩm trong cửa hàng</span>
            </NSpace>
        </n-card>
        <n-card title="Bộ lọc" class="mt-20px">
            <template #header-extra>
                <n-button @click="refreshFilter" strong secondary circle type="success">
                    <template #icon>
                        <n-icon>
                            <svg version="1.1" xmlns="http://www.w3.org/2000/svg"
                                xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 512 512"
                                enable-background="new 0 0 512 512" xml:space="preserve">
                                <path d="M433,288.8c-7.7,0-14.3,5.9-14.9,13.6c-6.9,83.1-76.8,147.9-161.8,147.9c-89.5,0-162.4-72.4-162.4-161.4
                                    c0-87.6,70.6-159.2,158.2-161.4c2.3-0.1,4.1,1.7,4.1,4v50.3c0,12.6,13.9,20.2,24.6,13.5L377,128c10-6.3,10-20.8,0-27.1l-96.1-66.4
                                    c-10.7-6.7-24.6,0.9-24.6,13.5v45.7c0,2.2-1.7,4-3.9,4C148,99.8,64,184.6,64,288.9C64,394.5,150.1,480,256.3,480
                                    c100.8,0,183.4-76.7,191.6-175.1C448.7,296.2,441.7,288.8,433,288.8L433,288.8z">
                                </path>
                            </svg>
                        </n-icon>
                    </template>
                </n-button>
            </template>
            <n-space vertical>
                <n-row :gutter="24">
                    <n-col :span="12">
                        <span>Tìm kiếm</span>
                        <n-input v-model:value="state.search.q" placeholder="Tìm kiếm sản phẩm theo tên hoặc mã"
                            clearable />
                    </n-col>
                    <n-col :span="6">
                        <span>Hãng</span>
                        <n-select v-model:value="state.search.brand" :options="state.data.brands"
                            placeholder="Chọn hãng" clearable></n-select>
                    </n-col>
                    <n-col :span="6">
                        <span>Pin</span>
                        <n-select v-model:value="state.search.battery" :options="state.data.batteries"
                            placeholder="Chọn pin" clearable></n-select>
                    </n-col>

                </n-row>
                <div class="mt-20px">
                    <n-row :gutter="12">
                        <n-col :span="12">
                            <span>Khoảng giá</span>
                            <n-slider v-model:value="state.search.price" range :step="1000" :min="1000" :max="50000000"
                                clearable />
                        </n-col>
                        <n-col :span="6">
                            <span>Màn hình</span>
                            <n-select v-model:value="state.search.screen" :options="state.data.screens"
                                placeholder="Chọn màn hình" clearable></n-select>
                        </n-col>
                        <n-col :span="6">
                            <span>Hệ điều hành</span>
                            <n-select v-model:value="state.search.operatingSystem"
                                :options="state.data.operatingSystems" clearable
                                placeholder="Chọn hệ điều hành"></n-select>
                        </n-col>
                    </n-row>
                </div>
            </n-space>
        </n-card>
        <n-card title="Danh sách sản phẩm" class="mt-20px">
            <template #header-extra>
                <n-space justify="end">
                    <n-button circle type="primary" @click="handleClickAddProduct()">
                        <Icon icon="material-symbols:add" />
                    </n-button>
                </n-space>
            </template>
            <n-data-table :columns="columns" :data="state.data.products" :pagination="{
                page: state.pagination.page,
                pageSize: state.pagination.size,
                showSizePicker: true,
                pageSizes: [10, 20, 25, 50, 100],
                onChange: (page: number) => {
                    state.pagination.page = page
                },
                onUpdatePageSize: (pageSize: number) => {
                    state.pagination.page = 1
                    state.pagination.size = pageSize
                }
            }"
            :ellipsis="true"
            :bordered="false" :row-key="(row) => row.id" 
            :expanded-row-keys="expandedKeys"
            @update:expanded-row-keys="key => expandedKeys = key as Array<string>"
            />
        </n-card>
    </div>
</template>

<script lang="ts" setup>
import {
    ADProductResponse,
    ADPRPropertiesComboboxResponse,
    changeProductStatus,
    getBatteries,
    getBrands,
    getOperatingSystems,
    getProducts,
    getScreens,
} from '@/service/api/admin/product/product.api'
import { Icon } from '@iconify/vue'
import { debounce } from 'lodash'
import { DataTableColumns, NButton, NImage, NSpace, NSwitch } from 'naive-ui'
import { onMounted, reactive, Ref, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const state = reactive({
    search: {
        q: '',
        brand: undefined as string | undefined,
        screen: undefined as string | undefined,
        battery: undefined as string | undefined,
        operatingSystem: undefined as string | undefined,
        price: [10000, 50000000]
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
        totalPages: undefined as number | undefined,
    },
})

const expandedKeys = ref<Array<string>>([])

const fetchProducts = async () => {
    const res = await getProducts({
        page: state.pagination.page,
        size: state.pagination.size,
        q: state.search.q,
        idBrand: state.search.brand as string,
        idOperatingSystem: state.search.operatingSystem as string,
        idBattery: state.search.battery as string,
        idScreen: state.search.screen as string,
        minPrice: state.search.price[0],
        maxPrice: state.search.price[1],
    })

    state.data.products = res.data.data
    state.pagination.totalPages = res.data.totalPages
}

const fetchComboboxProperties = async () => {
    try {
        const [screenProperties, brandProperties, batteryProperties, operatingSystemProperties] = await Promise.all([
            getScreens(),
            getBrands(),
            getBatteries(),
            getOperatingSystems(),
        ])

        state.data.screens = screenProperties.data
        state.data.batteries = batteryProperties.data
        state.data.brands = brandProperties.data
        state.data.operatingSystems = operatingSystemProperties.data
    } catch (error) {
        console.log(error)
    }
}

const refreshFilter = () => {
    state.search.q = ''
    state.search.battery = ''
    state.search.brand = ''
    state.search.screen = ''
    state.search.operatingSystem = ''
    state.search.price = [10000, 50000000]
}

const columns: DataTableColumns<ADProductResponse> = [
    { type: 'selection' },
    {
        type: 'expand',
        expandable: rowData => rowData.name !== 'Jim Green',
        renderExpand: (rowData: ADProductResponse) => h('div', {style: {marginTop: '10px', marginLeft: '45px', display: 'flex', gap: '20px', flexDirection: 'column'}},
            [
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'carbon:tag' }), h('span', { style: { marginLeft: '8px' }, innerText: `Hãng: ${rowData.brand}` })]),
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'carbon:battery-half' }), h('span', { style: { marginLeft: '8px' }, innerText: `Pin: ${rowData.battery}` })]),
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'carbon:carbon-for-ibm-dotcom' }), h('span', { style: { marginLeft: '8px' }, innerText: `Hệ điều hành: ${rowData.operatingSystem}` })]),
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'icon-park-outline:monitor' }), h('span', { style: { marginLeft: '8px' }, innerText: `Màn hình: ${rowData.screen}` })]),
            ]
        )
    },
    {
        title: '#', key: 'orderNumber', width: 50, render: (_: ADProductResponse, index: number) => {
            return h('span', { innerText: index + 1 })
        }
    },
    { title: 'Mã', key: 'code', width: 100, },
    { title: 'Tên', key: 'name', width: 200, },
    {
        title: 'Ảnh sản phẩm', key: 'brand', width: 150, align: 'center',
        render: (data: ADProductResponse) => {
            return h(NImage, { width: 200, src: data.urlImage })
        }
    },
    {
        title: 'Giá thấp nhất', key: 'minPrice', width: 150, align: 'center',
        render: (data: ADProductResponse) => h('span', (data.minPrice + '').split('').reduce((prev, curr, index, arr) => {
            if ((arr.length - index) % 3 == 0) return prev + '.' + curr
            return prev + curr
        }, '') + ' vnđ')
    },
    {
        title: 'Giá lớn nhất', key: 'maxPrice', width: 150, align: 'center',
        render: (data: ADProductResponse) => h('span', (data.maxPrice + '').split('').reduce((prev, curr, index, arr) => {
            if ((arr.length - index) % 3 == 0) return prev + '.' + curr
            return prev + curr
        }, '') + ' vnđ')
    },
    {
        title: 'Số lượng', key: 'quantity', width: 150, align: 'center',
        render: (data: ADProductResponse) => h('span', data.quantity + ' sản phẩm')

    },
    {
        title: 'Trạng thái', key: 'status', width: 70, align: 'center',
        render: (data: ADProductResponse) => h(NSwitch, { value: data.status == 'ACTIVE', onUpdateValue: (value: boolean) => { handleChangeStatus(data.id as string) } })
    },
    {
        title: 'Thao tác', key: 'action', width: 150,
        render: (data: ADProductResponse) => {
            return h(NSpace,
                [
                    h(NButton, {
                        quaternary: true, size: 'small', circle: true, type: 'info',
                        onClick: () => redirectVariant(data.id as string)
                    },
                        h(Icon, { icon: 'material-symbols:info-outline' })
                    ),
                    h(NButton, {
                        quaternary: true, size: 'small', circle: true,
                        onClick: () => clickOpenModal(data.id, true)
                    },
                        h(Icon, { icon: 'carbon:edit' })
                    ),
                    h(NButton, {
                        quaternary: true, size: 'small', circle: true, type: 'success',
                        onClick: () => addVariant(data.id as string)
                    },
                        h(Icon, { icon: 'material-symbols:add' })
                    ),
                ]
            )
        }
    },
]

onMounted(() => {
    fetchProducts()
    fetchComboboxProperties()
})

const isOpenModal = ref<boolean>(false)

const isDetailModal: Ref<boolean> = ref(true)

const productIdSelected = ref<string>()

const clickOpenModal = (id?: string, isDetail?: boolean) => {
    productIdSelected.value = id
    isOpenModal.value = true
    isDetailModal.value = isDetail ?? false
}

const closeModal = () => {
    isOpenModal.value = false
}

const handleSuccessModifyModal = () => {
    fetchProducts()
    closeModal()
}

const debounceFetchProducts = debounce(fetchProducts, 300)

watch(
    () => [state.search.q, state.search.operatingSystem, state.search.battery, state.search.brand, state.search.screen, state.search.price[0], state.search.price[1]],
    () => {
        debounceFetchProducts()
    }
)

const handleClickAddProduct = (id?: string) => {
    router.push({
        name: 'products_add',
        params: {
            id,
        },
    })
}

const notification = useNotification()

const handleChangeStatus = async (id: string) => {
    const res = await changeProductStatus(id)

    if (res.success) notification.success({ content: 'Thay đổi trạng thái thành công', duration: 3000 })
    else notification.error({ content: 'Thay đổi trạng thái thất bại', duration: 3000 })

    fetchProducts();
}

const redirectVariant = (id: string) => {
    router.push({ name: 'products_variant', query: { idProduct: id } })
}

const addVariant = (id: string) => {
    router.push({ name: 'products_add', params: { id } })
}

</script>

<style scoped>
.container {
    padding: 0 20px 20px 20px;
}

.mt-20px {
    margin-top: 20px;
}

.mb-20px {
    margin-bottom: 20px;
}

.pt-20px {
    padding-top: 20px;
}

.p-20px {
    padding: 20px;
}

.line {
    margin: 32px 0;
    border-top: 1px solid #a4a5a8;
}

.d-inline {
    display: inline;
}
</style>