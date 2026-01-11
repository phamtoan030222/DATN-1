<template>
    <n-modal :show="isOpen">
        <n-card style="width: 30%" :title="getTextByProductPropertiesType" :bordered="false" size="huge" role="dialog"
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
                        :positive-text="'Xác nhận'" :negative-text="'Hủy'">
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
    if (!data.value || !props.type) {
        notification.error({ content: `Vui lòng nhập giá trị ${translateProperty(props.type as ProductPropertiesType)}`, duration: 3000 })
        return;
    }

    try {
        const res = await quickAddProperties(data.value.trim(), props.type as ProductPropertiesType);

        if (res.success) {
            notification.success({ content: `Thêm nhanh ${translateProperty(props.type as ProductPropertiesType)} thành công`, duration: 3000 })
            emit('success');
        } else {
            // Hiển thị message lỗi từ backend nếu có
            const errorMessage = res.message || `Thêm nhanh ${translateProperty(props.type as ProductPropertiesType)} thất bại`;
            notification.error({ content: errorMessage, duration: 3000 });
        }
    } catch (error: any) {
        // Xử lý lỗi từ API call
        console.error('Lỗi khi thêm nhanh thuộc tính:', error);
        
        // Lấy message lỗi từ response backend
        const backendErrorMessage = error.response?.data?.message || 
                                   error.response?.data?.error || 
                                   error.message || 
                                   `Thêm nhanh ${translateProperty(props.type as ProductPropertiesType)} thất bại`;
        
        notification.error({ 
            content: backendErrorMessage, 
            duration: 3000 
        });
    } finally {
        emit('close');
        clearData();
    }
}

const clearData = () => {
    data.value = '';
}
const data = ref<string>('');

const getTextByProductPropertiesType = computed(() => {
    return translateProperty(props.type as ProductPropertiesType);
})
</script>