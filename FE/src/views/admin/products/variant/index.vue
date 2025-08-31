<template>
    <div>
        <n-card>
            <NSpace vertical :size="8">
                <NSpace align="center">
                    <NIcon size="24">
                        <Icon icon="icon-park-outline:list" />
                    </NIcon>
                    <span style="font-weight: 600; font-size: 24px">
                        Quản lý biến thể sản phẩm
                    </span>
                </NSpace>
                <span>Quản lý biến thể sản phẩm trong cửa hàng</span>
            </NSpace>
        </n-card>
        <n-card title="Bộ lọc">
            <n-space justify="center">
                <n-form>
                    
                </n-form>
            </n-space>
        </n-card>
    </div>
</template>

<script lang="ts" setup>
import { ADProductDetailResponse, ADPRPropertiesComboboxResponse, changeProductDetailStatus, getColors, getCPUs, getGPUs, getHardDrives, getMaterials, getProductDetails, getRAMs } from '@/service/api/admin/product/productDetail.api'
import { Icon } from '@iconify/vue'
import { debounce } from 'lodash'
import { DataTableColumns, NButton, NSpace, NSwitch } from 'naive-ui'
import { onMounted, reactive, ref, Ref } from 'vue'

const router = useRouter()

const state = reactive({
    search: {
        q: '',
        cpu: '',
        gpu: '',
        hardDrive: '',
        ram: '',
        color: '',
        material: '',
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

const fetchProductDetails = async () => {
    const res = await getProductDetails({
        page: state.pagination.page,
        size: state.pagination.size,
        q: state.search.q,
        idCPU: state.search.cpu,
        idGPU: state.search.gpu,
        idColor: state.search.color,
        idRAM: state.search.ram,
        idHardDrive: state.search.hardDrive,
        idMaterial: state.search.material,
    })

    state.data.productDetails = res.data.data
    state.pagination.totalPages = res.data.totalPages
}

const fetchComboboxProperties = async () => {
    try {
        const [colorProperties ,cpuProperties, gpuProperties, materialProperties, ramSystemProperties, hardDriveProperties] = await Promise.all([
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
    state.search.q = ''
    state.search.cpu = ''
    state.search.gpu = ''
    state.search.material = ''
    state.search.ram = ''
    state.search.color = ''
    state.search.hardDrive = ''
}

const columns: DataTableColumns<ADProductDetailResponse> = [
    { type: 'selection', fixed: 'left' },
    {
        title: '#', key: 'orderNumber', width: 50, fixed: 'left', render: (data: ADProductDetailResponse, index: number) => {
            return h('span', { innerText: index + 1 })
        }
    },
    { title: 'Mã', key: 'code', width: 100, fixed: 'left', },
    { title: 'Tên', key: 'name', width: 100, fixed: 'left', },
    {
        title: 'Trạng thái', key: 'status', width: 70, align: 'center',
        render: (data: ADProductDetailResponse) => h(NSwitch, {value: data.status == 'ACTIVE', onUpdateValue: (value: boolean) => {handleChangeStatus(data.id as string)}})
    },
    { title: 'Hãng', key: 'brand', width: 150, align: 'center', },
    { title: 'Pin', key: 'battery', width: 150, fixed: 'left', },
    { title: 'Màn hình', key: 'screen', width: 150, align: 'center', },
    { title: 'Hệ điều hành', key: 'operatingSystem', width: 150, align: 'center', },
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
    fetchProductDetails()
    closeModal()
}

const debounceFetchProducts = debounce(fetchProductDetails, 300)

watch(
    () => [state.search.q, state.search.cpu, state.search.gpu, state.search.hardDrive, state.search.material, state.search.ram, state.search.color, state.pagination.page, state.pagination.size],
    () => {
        debounceFetchProducts()
    }
)

const notification = useNotification()

const handleChangeStatus = async (id: string) => {
    const res = await changeProductDetailStatus(id)

    if(res.success) notification.success({content: 'Thay đổi trạng thái thành công', duration: 3000})
    else notification.error({content: 'Thay đổi trạng thái thất bại', duration: 3000})

    fetchProductDetails();
}
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