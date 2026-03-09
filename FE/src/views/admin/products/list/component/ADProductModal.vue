<template>
    <n-modal :show="isOpen">
        <n-card style="width: 50vw"
            title="Cập nhật sản phẩm" :bordered="false"
            size="huge" role="dialog" aria-modal="true">
            <template #header-extra>
                <n-button @click="handleClickCancel">
                    <Icon icon="ic:outline-close" />
                </n-button>
            </template>
            <!-- content -->
            <div>
                <n-form ref="formRef" :model="detailProduct" :rules="rules">
                    <n-grid  :style="{ width: '100%' }" :span="12" :x-gap="30">
                        <n-form-item-gi :span="24" label="Tên" path="name">
                            <n-input v-model:value="detailProduct.name" placeholder="Nhập tên" clearable></n-input>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Màn hình" path="idScreen">
                            <n-select v-model:value="detailProduct.idScreen" :options="screens"
                                :placeholder="'Chọn tấm nền'" clearable></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Pin" path="idBattery">
                            <n-select v-model:value="detailProduct.idBattery" :options="batteries"
                                :placeholder="'Chọn kích thước màn hình'" clearable></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Hệ điều hành" path="idOperatingSystem">
                            <n-select v-model:value="detailProduct.idOperatingSystem" :options="operatingSystems"
                                :placeholder="'Chọn kích thước màn hình'" clearable></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Hãng" path="idBrand">
                            <n-select v-model:value="detailProduct.idBrand" :options="brands"
                                :placeholder="'Chọn kích thước màn hình'" clearable></n-select>
                        </n-form-item-gi>
                    </n-grid>
                </n-form>
            </div>

            <!-- footer -->
            <template #footer>
                <n-space justify="end">
                    <n-button @click="handleClickCancel">Hủy</n-button>
                    <n-popconfirm @positive-click="handleClickOK" @negative-click="handleClickCancel"
                    :positive-text="'Xác nhận'" :negative-text="'Hủy'"
                    >
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
import { ADProductDetailResponse, ADPRPropertiesComboboxResponse, getProductById, updateProduct } from '@/service/api/admin/product/product.api';
import { NullableOptional } from '@/typings/api/api.common';
import { Icon } from '@iconify/vue';
import { Ref, ref, watch } from 'vue';

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

const detailProduct: Ref<NullableOptional<ADProductDetailResponse>> = ref({
    id: null as string | null | undefined,
    code: null as string | null | undefined,
    name: null as string | null | undefined,
    idBattery: null as string | null | undefined,
    idOperatingSystem: null as string | null | undefined,
    idBrand: null as string | null | undefined,
    idScreen: null as string | null | undefined,
})

// validation
const formRef = ref();
const rules = {
    name: { required: true, message: 'Vui lòng nhập tên', trigger: ['input', 'blur'] },
    idScreen: { required: true, message: 'Vui lòng chọn tấm nền', trigger: ['change', 'blur'] },
    idBattery: { required: true, message: 'Vui lòng chọn pin', trigger: ['change', 'blur'] },
    idOperatingSystem: { required: true, message: 'Vui lòng chọn hệ điều hành', trigger: ['change', 'blur'] },
    idBrand: { required: true, message: 'Vui lòng chọn hãng', trigger: ['change', 'blur'] },
};

const fetchDetailProduct = async () => {
    const res = await getProductById(props.id as string)

    detailProduct.value = res.data
}

const resetField = () => {
    detailProduct.value = {
        id: null,
        code: null,
        name: null,
        idBattery: null,
        idOperatingSystem: null,
        idBrand: null,
        idScreen: null,
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
    // validate form before submission
    if (formRef.value) {
        try {
            await formRef.value.validate();
        } catch (err) {
            // validation failed, abort
            return;
        }
    }

    const res = await updateProduct(detailProduct.value as ADProductDetailResponse)

    if (res.success) notification.success({ content: props.id ? 'Cập nhật sản phẩm thành công' : 'Thêm sản phẩm thành công', duration: 3000 })
    else notification.error({ content: props.id ? 'Cập nhật sản phẩm thất bại' : 'Thêm sản phẩm thất bại', duration: 3000 })
    if (!props.id) resetField()
    emit('success')
    handleClickCancel()
}
</script>

<style scoped>

</style>