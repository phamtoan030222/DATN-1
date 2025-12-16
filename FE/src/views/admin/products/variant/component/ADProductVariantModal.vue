<template>
    <n-modal :show="isOpen">
        <n-card style="width: 50%" title="Cập nhật sản phẩm chi tiết" :bordered="false" size="huge" role="dialog"
            aria-modal="true">
            <template #header-extra>
                <n-button @click="handleClickCancel">
                    <Icon icon="ic:outline-close" />
                </n-button>
            </template>

            <!-- content -->
            <div :style="{  }">
                <n-form>
                    <n-grid :span="24" :x-gap="24">
                        <n-form-item-gi :span="12" label="Mã">
                            <n-input v-model:value="detailProduct.code" disabled placeholder="Nhập mã"></n-input>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Tên">
                            <n-input v-model:value="detailProduct.name" disabled placeholder="Nhập tên"></n-input>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="CPU">
                            <n-select v-model:value="detailProduct.idCPU" :options="cpus"
                                :placeholder="'Chọn tấm nền'"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="GPU">
                            <n-select v-model:value="detailProduct.idGPU" :options="gpus"
                                :placeholder="'Chọn kích thước màn hình'"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="RAM">
                            <n-select v-model:value="detailProduct.idRAM" :options="rams"
                                :placeholder="'Chọn kích thước màn hình'"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="Chất liệu">
                            <n-select v-model:value="detailProduct.idMaterial" :options="materials"
                                :placeholder="'Chọn kích thước màn hình'"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="Màu sắc">
                            <n-select v-model:value="detailProduct.idColor" :options="colors"
                                :placeholder="'Chọn kích thước màn hình'"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="Ổ cứng">
                            <n-select v-model:value="detailProduct.idHardDrive" :options="hardDrives"
                                :placeholder="'Chọn kích thước màn hình'"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="24" label="Giá sản phẩm">
                            <n-input-number style="width: 100%;" v-model:value="detailProduct.price" placeholder="Nhập giá"></n-input-number>
                        </n-form-item-gi>
                    </n-grid>
                </n-form>
            </div>

            <n-data-table v-show="imeisProductDetail && imeisProductDetail.length > 0" :columns="columnsImei" :data="imeisProductDetail"></n-data-table>

            <!-- footer -->
            <template #footer>
                <n-space justify="end">
                    <n-button @click="handleClickCancel">Hủy</n-button>
                    <n-popconfirm @positive-click="handleClickOK" @negative-click="handleClickCancel"
                        :positive-button-props="{ type: 'info' }">
                        <template #trigger>
                            <n-button type="success">Xác nhận</n-button>
                        </template>
                        Bạn chắc chắn muốn thao tác
                    </n-popconfirm>
                </n-space>
            </template>
        </n-card>
    </n-modal>
</template>

<script setup lang="ts">
import { Ref, ref, watch } from 'vue'
import { Icon } from '@iconify/vue';
import { ADPDImeiResponse, ADProductDetailDetailResponse, ADPRPropertiesComboboxResponse, changeStatusImei, getImeiProductDetail, getProductDetailById, modifyProductDetail } from '@/service/api/admin/product/productDetail.api';
import { DataTableColumns, NButton, NSpace, NSwitch } from 'naive-ui';

const props = defineProps<{
    isOpen: boolean
    id: string | undefined
    cpus: ADPRPropertiesComboboxResponse[]
    gpus: ADPRPropertiesComboboxResponse[]
    materials: ADPRPropertiesComboboxResponse[]
    hardDrives: ADPRPropertiesComboboxResponse[]
    rams: ADPRPropertiesComboboxResponse[]
    colors: ADPRPropertiesComboboxResponse[]
}>()

const emit = defineEmits(['success', 'close'])

const detailProduct: Ref<Partial<ADProductDetailDetailResponse>> = ref({
    id: undefined,
    code: undefined,
    name: undefined,
    idCPU: undefined,
    idGPU: undefined,
    idColor: undefined,
    idRAM: undefined,
    idHardDrive: undefined,
    idMaterial: undefined,
    price: 0,
})

const fetchDetailProduct = async () => {
    const res = await getProductDetailById(props.id as string)

    detailProduct.value = res.data
}

const imeisProductDetail: Ref<ADPDImeiResponse[]> = ref([])

const getImeisProductDetail = async () => {
    const res = await getImeiProductDetail(props.id as string)

    imeisProductDetail.value = res.data as ADPDImeiResponse[]
}

const resetField = () => {
    detailProduct.value = {
        id: undefined,
        code: undefined,
        name: undefined,
        idCPU: undefined,
        idGPU: undefined,
        idColor: undefined,
        idRAM: undefined,
        idHardDrive: undefined,
        idMaterial: undefined,
        price: 0,
    }
}

watch(
    () => props.id,
    (newId) => {
        if (newId) {
            fetchDetailProduct()
            getImeisProductDetail()
        }
        else resetField()
    }
)

const handleClickCancel = () => {
    emit('close')
}

const notification = useNotification()

const handleClickOK = async () => {
    const res = await modifyProductDetail({
        ...detailProduct.value as ADProductDetailDetailResponse,
        imei: []
    })

    if (res.success) notification.success({ content: props.id ? 'Cập nhật sản phẩm thành công' : 'Thêm sản phẩm thành công', duration: 3000 })
    else notification.error({ content: props.id ? 'Cập nhật sản phẩm thất bại' : 'Thêm sản phẩm thất bại', duration: 3000 })
    if (!props.id) resetField()
    emit('success')
}

const columnsImei: DataTableColumns<ADPDImeiResponse> = [
    { type: 'selection', fixed: 'left' },
    {
        title: '#', key: 'orderNumber', width: 50, fixed: 'left', render: (_, index: number) => {
            return h('span', { innerText: index + 1 })
        }
    },
    { title: 'Mã', key: 'code', width: 100, fixed: 'left', },
    { title: 'Tên', key: 'name', width: 100 },
    {
        title: 'Trạng thái', key: 'status', width: 70, align: 'center',
        render: (data: ADPDImeiResponse) => h(NSwitch, { value: data.status == 'ACTIVE', onUpdateValue: (_) => { handleChangeStatusImei(data.id as string) } })
    },
    // {
    //     title: 'Thao tác', key: 'action', width: 100, fixed: 'right',
    //     render: (data: ADPDImeiResponse) => {
    //         return h(NSpace,
    //             [
    //                 h(NButton, {
    //                     quaternary: true, size: 'small', circle: true,
    //                     onClick: () => clickOpenModal(data.id, true)
    //                 },
    //                     h(Icon, { icon: 'carbon:edit' })
    //                 ),
    //             ]
    //         )
    //     }
    // },
]


const handleChangeStatusImei = async (idImei: string) => {
    const res = await changeStatusImei(idImei)
    if (res.success) notification.success({ content: 'Thay đổi trạng thái IMEI thành công', duration: 3000 })
    else notification.error({ content: 'Thay đổi trạng thái IMEI thất bại', duration: 3000 })

    getImeisProductDetail()
}
</script>

<style scoped>
.container {
    max-height: 400px;
    overflow-y: auto;
}
</style>