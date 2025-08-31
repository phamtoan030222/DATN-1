<template>
    <n-modal :show="isOpen">
        <n-card style="width: 600px" :title="id ? (isDetail ? 'Chi tiết sản phẩm' : 'Cập nhật sản phẩm') : 'Thêm sản phẩm'"
            :bordered="false" size="huge" role="dialog" aria-modal="true">
            <template #header-extra>
                <n-button @click="handleClickCancel">
                    <Icon icon="ic:outline-close" />
                </n-button>
            </template>

            <!-- content -->
            <div :style="{ maxHeight: '400px', overflowY: 'auto' }">
                <n-form>
                    <n-form-item label="Mã">
                        <n-input v-model:value="detailProduct.code" placeholder="Nhập mã"></n-input>
                    </n-form-item>
                    <n-form-item label="Tên">
                        <n-input v-model:value="detailProduct.name" placeholder="Nhập tên"></n-input>
                    </n-form-item>
                    <n-form-item label="Màn hình">
                        <n-select v-model:value="detailProduct.idScreen" :options="screens"
                            :placeholder="'Chọn tấm nền'"></n-select>
                    </n-form-item>
                    <n-form-item label="Pin">
                        <n-select v-model:value="detailProduct.idBattery" :options="batteries"
                            :placeholder="'Chọn kích thước màn hình'"></n-select>
                    </n-form-item>
                    <n-form-item label="Hệ điều hành">
                        <n-select v-model:value="detailProduct.idOperatingSystem" :options="operatingSystems"
                            :placeholder="'Chọn kích thước màn hình'"></n-select>
                    </n-form-item>
                    <n-form-item label="Hãng">
                        <n-select v-model:value="detailProduct.idBrand" :options="brands"
                        :placeholder="'Chọn kích thước màn hình'"></n-select>
                    </n-form-item>
                </n-form>
            </div>

            <!-- footer -->
            <template #footer>
                <n-space justify="end">
                    <n-button @click="handleClickCancel">Hủy</n-button>
                    <n-popconfirm @positive-click="handleClickOK" @negative-click="handleClickCancel"
                        :positive-button-props="{ type: 'info' }">
                        <template #trigger>
                            <n-button>Xác nhận</n-button>
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
import { ADProductDetailResponse, ADPRPropertiesComboboxResponse, getProductById, modifyProduct } from '@/service/api/admin/product/product.api'
import { Icon } from '@iconify/vue';

const props = defineProps<{
    isOpen: boolean
    id: string | undefined
    isDetail: boolean
    screens: ADPRPropertiesComboboxResponse[]
    batteries: ADPRPropertiesComboboxResponse[]
    brands: ADPRPropertiesComboboxResponse[]
    operatingSystems: ADPRPropertiesComboboxResponse[]
}>()

const emit = defineEmits(['success', 'close'])

const detailProduct: Ref<ADProductDetailResponse> = ref({
    id: '',
    code: '',
    name: '',
    idBattery: '',
    idOperatingSystem: '',
    idBrand: '',
    idScreen: '',
})

const fetchDetailProduct = async () => {
    const res = await getProductById(props.id as string)

    detailProduct.value = res.data
}

const resetField = () => {
    detailProduct.value = {
        id: '',
        code: '',
        name: '',
        idBattery: '',
        idOperatingSystem: '',
        idBrand: '',
        idScreen: '',
    }
}

watch(
    () => props.id,
    (newId) => {
        if (newId) fetchDetailProduct()
        else resetField()
    }
)

const handleClickCancel = () => {
    emit('close')
}

const notification = useNotification()

const handleClickOK = async () => {
    const res = await modifyProduct(detailProduct.value)
    console.log(res.success)
    if (res.success) notification.success({ content: props.id ? 'Cập nhật sản phẩm thành công' : 'Thêm sản phẩm thành công', duration: 3000 })
    else notification.error({ content: props.id ? 'Cập nhật sản phẩm thất bại' : 'Thêm sản phẩm thất bại', duration: 3000 })
    if (!props.id) resetField()
    emit('success')
}
</script>

<style scoped>
.container {
    max-height: 400px;
    overflow-y: auto;
}
</style>