<template>
    <n-modal :show="isOpen">
        <n-card style="width: 40%" :title="getTextByProductPropertiesType" :bordered="false" size="huge" role="dialog"
            aria-modal="true">
            <template #header-extra>
                <n-button @click="handleClickCancel">
                    <Icon icon="ic:outline-close" />
                </n-button>
            </template>

            <!-- content -->
            <div :style="{}">
                <n-input v-model:value="data" placeholder="Nhập giá trị" />
            </div>

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

<script lang="ts" setup>
import { ProductPropertiesType, translateProperty } from '@/constants/ProductPropertiesType';
import { quickAddProperties } from '@/service/api/admin/product/productDetail.api';
import { Icon } from '@iconify/vue';

const props = defineProps<{
    isOpen: boolean,
    type: ProductPropertiesType | undefined
}>()

const emit = defineEmits(['close', 'success'])

const handleClickCancel = () => {
    clearData();
    emit('close');
}

const notification = useNotification();

const handleClickOK = async () => {
    const res = await quickAddProperties(data.value, props.type as ProductPropertiesType);

    if (res.success) {
        notification.success({ content: `Thêm nhanh ${translateProperty(props.type as ProductPropertiesType)} thành công`, duration: 3000 })
        emit('success');
    }
    else notification.error({ content: `Thêm nhanh ${translateProperty(props.type as ProductPropertiesType)} thất bại`, duration: 3000 })
    emit('close');
    clearData();
}
const clearData = () => {
    data.value = '';
}

const data = ref<string>('');

const getTextByProductPropertiesType = computed(() => {
    return translateProperty(props.type as ProductPropertiesType);
})
</script>